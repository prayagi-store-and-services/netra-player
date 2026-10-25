package com.prayagi.netraplayer

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.os.SystemClock
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

private fun readHeat(context: Context): HeatReading {
    val battery = try { context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) }
        catch (_: SecurityException) { null }
    val temperature = battery?.takeIf { it.hasExtra(BatteryManager.EXTRA_TEMPERATURE) }
        ?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
    val status = battery?.takeIf { it.hasExtra(BatteryManager.EXTRA_STATUS) }
        ?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
    val charging = when (status) {
        BatteryManager.BATTERY_STATUS_CHARGING, BatteryManager.BATTERY_STATUS_FULL -> true
        BatteryManager.BATTERY_STATUS_DISCHARGING, BatteryManager.BATTERY_STATUS_NOT_CHARGING -> false
        else -> null
    }
    val thermal = if (Build.VERSION.SDK_INT >= 29) {
        try { context.getSystemService(PowerManager::class.java)?.currentThermalStatus }
        catch (_: RuntimeException) { null }
    } else null
    return HeatReading(SystemClock.elapsedRealtime(), Process.getElapsedCpuTime(), temperature, charging, thermal)
}

@Composable
internal fun HeatPreviewCard() {
    val context = LocalContext.current.applicationContext
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val gate = remember { HeatPreviewGate() }
    val scope = rememberCoroutineScope()
    var enabled by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var preview by remember { mutableStateOf<HeatPreview?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var job by remember { mutableStateOf<Job?>(null) }
    fun stop() {
        job?.cancel(); job = null; gate.cancel(); busy = false
    }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                stop(); gate.setEnabled(false); enabled = false; preview = null; message = null
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); job?.cancel(); gate.setEnabled(false) }
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Heating diagnostics: local preview", style = MaterialTheme.typography.titleMedium)
            Text("OFF by default. A manual 5-second sample reads this app's CPU time, battery temperature and Android thermal status when available. No upload, no login, no background monitoring.")
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Allow local preview on this screen", Modifier.weight(1f))
                Switch(checked = enabled, onCheckedChange = {
                    stop(); gate.setEnabled(it); enabled = it; preview = null; message = null
                })
            }
            Text("These readings cannot identify which other app is causing heat. Battery temperature is not CPU or skin temperature. CPU use includes this preview's small measurement cost.", style = MaterialTheme.typography.bodySmall)
            Button(enabled = enabled && !busy, onClick = {
                if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return@Button
                val token = gate.begin() ?: return@Button
                busy = true; preview = null; message = null
                job = scope.launch {
                    try {
                        val start = readHeat(context)
                        delay(5_000)
                        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) { gate.cancel(); return@launch }
                        gate.finish(token, HeatPreview.between(start, readHeat(context)))
                        preview = gate.preview
                        message = if (preview == null) "Sample unavailable. Keep this screen open and try again." else null
                    } catch (_: RuntimeException) {
                        if (gate.isCurrent(token)) message = "Sample unavailable on this device."
                    } finally {
                        if (gate.isCurrent(token)) { gate.cancel(); busy = false; job = null }
                    }
                }
            }) { Text(if (busy) "Sampling for 5 seconds..." else "Take one local sample") }
            preview?.let { p ->
                Text("This app: ${p.processCpuMs} ms CPU / ${p.windowMs} ms elapsed (${String.format(Locale.ROOT, "%.1f", p.cpuCorePercent)}% of one core; may exceed 100% with multiple cores)")
                Text("Battery temperature: ${p.batteryDeciC?.let { String.format(Locale.ROOT, "%.1f C", it / 10.0) } ?: "Unavailable"}")
                Text("Charging/full: ${p.charging?.let { if (it) "Yes" else "No" } ?: "Unavailable"}")
                val thermal = listOf("None", "Light", "Moderate", "Severe", "Critical", "Emergency", "Shutdown")
                Text("Android thermal status: ${p.thermalStatus?.let { thermal[it] } ?: "Unavailable"}")
            }
            message?.let { Text(it) }
            OutlinedButton(enabled = busy || preview != null || message != null, onClick = {
                stop(); gate.clear(); preview = null; message = null
            }) { Text("Clear local sample") }
            Text("Sample stays in screen memory only and clears when you turn this off or leave the app. Private opt-in fleet reports and the owner website are not enabled yet.", style = MaterialTheme.typography.bodySmall)
        }
    }
}
