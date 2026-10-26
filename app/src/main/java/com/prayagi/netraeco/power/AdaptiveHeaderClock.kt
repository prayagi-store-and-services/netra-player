package com.prayagi.netraplayer.power

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import kotlinx.coroutines.delay
import java.util.Date

/** Lifecycle-owned callbacks only. No alarm, worker, service, network, sensor or wake lock. */
data class HeaderClock(val now: Date, val paused: Boolean)

@Composable
fun adaptiveHeaderClock(): HeaderClock {
    val context = LocalContext.current.applicationContext
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val pm = remember(context) { context.getSystemService(Context.POWER_SERVICE) as PowerManager }
    var batteryPercent by remember { mutableStateOf<Int?>(null) }
    var charging by remember { mutableStateOf(false) }
    var saver by remember { mutableStateOf(pm.isPowerSaveMode) }
    var interactive by remember { mutableStateOf(pm.isInteractive) }
    var thermalThrottling by remember { mutableStateOf(false) }
    var visible by remember { mutableStateOf(false) }
    var baseOrdinal by rememberSaveable { mutableStateOf(-1) }
    var now by remember { mutableStateOf(Date()) }

    DisposableEffect(context, lifecycle) {
        fun readBattery(i: Intent?) {
            if (i != null) {
                val level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                batteryPercent = if (level >= 0 && scale > 0) level * 100 / scale else null
                val status = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            }
        }
        var thermalUnavailable = false
        fun readPower() {
            saver = pm.isPowerSaveMode
            interactive = pm.isInteractive
            if (thermalUnavailable) {
                thermalThrottling = true
            } else if (Build.VERSION.SDK_INT >= 29) {
                try {
                    thermalThrottling = thermalUnavailable || pm.currentThermalStatus >= PowerManager.THERMAL_STATUS_MODERATE
                } catch (error: RuntimeException) {
                    thermalUnavailable = true
                    thermalThrottling = true
                    android.util.Log.w("NetraPower", "Thermal service unavailable; conservative header tier", error)
                }
            }
        }
        var receiverRegistered = false
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) {
                if (i?.action == Intent.ACTION_BATTERY_CHANGED) readBattery(i)
                readPower()
            }
        }
        val thermal = if (Build.VERSION.SDK_INT >= 29) PowerManager.OnThermalStatusChangedListener { status -> thermalThrottling = thermalUnavailable || status >= PowerManager.THERMAL_STATUS_MODERATE } else null
        var thermalRegistered = false
        fun stop() {
            if (receiverRegistered) { context.unregisterReceiver(receiver); receiverRegistered = false }
            if (Build.VERSION.SDK_INT >= 29 && thermalRegistered && thermal != null) {
                try { pm.removeThermalStatusListener(thermal) } catch (_: RuntimeException) { }
                thermalRegistered = false
            }
        }
        fun start() {
            if (receiverRegistered) return
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED).apply {
                addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            }
            readBattery(androidx.core.content.ContextCompat.registerReceiver(context, receiver, filter,
                androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED))
            receiverRegistered = true
            readPower()
            if (Build.VERSION.SDK_INT >= 29 && thermal != null) {
                thermalRegistered = OptionalPowerCallback.register(
                    { pm.addThermalStatusListener(context.mainExecutor, thermal) },
                    {
                        thermalUnavailable = true
                        thermalThrottling = true
                        android.util.Log.w("NetraPower", "Thermal callback unavailable; conservative header tier")
                    })
            }
        }
        val lease = LifecycleCallbackLease(lifecycle, ::start, ::stop) { visible = it }
        onDispose { lease.close() }
    }
    val input = PowerInputs(batteryPercent, charging, saver, thermalThrottling, visible, interactive)
    // Track unadjusted tier so a sustained thermal event cannot compound hysteresis repeatedly.
    val unadjusted = PowerPolicy.tier(input.copy(thermalThrottling = false), PowerTier.entries.getOrNull(baseOrdinal))
    SideEffect { if (baseOrdinal != unadjusted.ordinal) baseOrdinal = unadjusted.ordinal }
    val tier = if (input.thermalThrottling && unadjusted != PowerTier.SURVIVAL)
        PowerTier.entries[unadjusted.ordinal + 1] else unadjusted
    val interval = PowerPolicy.headerIntervalMillis(input, tier)
    LaunchedEffect(interval, visible, interactive) {
        if (!visible || !input.screenInteractive) return@LaunchedEffect
        fun minuteNow() = Date()
        now = minuteNow()
        if (interval != null) while (true) { delay(interval); now = minuteNow() }
    }
    return HeaderClock(now, tier == PowerTier.SURVIVAL)
}
