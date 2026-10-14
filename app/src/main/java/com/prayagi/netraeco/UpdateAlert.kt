package com.prayagi.netraplayer

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Update alert: every 6 hours (network required) asks the repo's latest release whether a newer version exists and
 * pre-downloads a verified APK when allowed and posts a notification. Tapping opens Android install confirmation.
 */
object UpdateAlert {
    const val EXTRA_AUTO_UPDATE = "netra_auto_update"
    private const val NOTIFICATION_ID = 7431
    private const val WORK_NAME = "netra_update_alert"
    private const val PREFS = "netra_update_alert"
    // Netra Eco beeps (default notification sound); the other apps use a silent channel.
    private const val BEEP = false
    private val CHANNEL_ID = if (BEEP) "netra_app_update_beep" else "netra_app_update"

    private fun allowedNow(c: Context, size: Long): Boolean {
        val cm = c.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork ?: return false) ?: return false
        val battery = c.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return false
        val level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val percent = if (level >= 0 && scale > 0) (level.toLong() * 100 / scale).toInt() else null
        val status = battery.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        return PreDownloadPolicy.allowed(
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED),
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI), caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR),
            !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING),
            cm.restrictBackgroundStatus == ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED,
            percent, status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL,
            false, size)
    }

    fun start(activity: Activity) {
        try { schedule(activity.applicationContext) } catch (_: Exception) { /* background check unavailable; the manual check still works */ }
        if (Build.VERSION.SDK_INT >= 33 &&
            activity.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            activity.requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 7432)
        }
        handle(activity, activity.intent)
    }

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<UpdateAlertWorker>(6, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    /** Runs when the notification was tapped: check, download, verify, hand to the installer. */
    fun handle(activity: Activity, intent: Intent?) {
        if (activity !is UpdateEntryActivity || intent?.getBooleanExtra(EXTRA_AUTO_UPDATE, false) != true) return
        intent.removeExtra(EXTRA_AUTO_UPDATE)
        val app = activity.applicationContext
        Toast.makeText(app, "Preparing the update...", Toast.LENGTH_SHORT).show()
        Thread {
            try {
                val self = Net.selfApp(app)
                val installed = Net.installed(app, self.packageName)
                val latest = Net.fetchLatest(self)
                if (latest != null && statusForRelease(installed?.first, installed?.second, latest) == Status.UpdateAvailable) {
                    val file = if (DownloadCenter.hasVerifiedFile(app, self, latest))
                        DownloadCenter.file(app, DownloadCenter.keyFor(self, latest))
                    else Net.download(app, self, latest).also { DownloadCenter.register(app, self, latest) }
                    activity.runOnUiThread {
                        try { Net.install(app, file) } catch (e: Exception) {
                            Toast.makeText(app, e.message ?: "Update failed.", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    activity.runOnUiThread { Toast.makeText(app, "No newer version found.", Toast.LENGTH_LONG).show() }
                }
            } catch (e: Exception) {
                activity.runOnUiThread { Toast.makeText(app, e.message ?: "Update failed.", Toast.LENGTH_LONG).show() }
            }
        }.start()
    }

    internal fun checkAndNotify(c: Context) {
        val self = Net.selfApp(c)
        val latest = Net.fetchLatest(self) ?: return
        val installed = Net.installed(c, self.packageName)
        if (statusForRelease(installed?.first, installed?.second, latest) != Status.UpdateAvailable) return
        val prefs = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        DownloadCenter.cleanFinished(c)
        if (!DownloadCenter.hasVerifiedFile(c, self, latest) && allowedNow(c, latest.size)) {
            try {
                Net.download(c, self, latest, backgroundAllowed = { allowedNow(c, latest.size) })
                DownloadCenter.register(c, self, latest)
            } catch (_: Exception) { /* No ready claim after a failed or policy-cancelled download. */ }
        }
        val ready = DownloadCenter.hasVerifiedFile(c, self, latest)
        val state = latest.tag + if (ready) ":downloaded" else ":available"
        if (prefs.getString("notified_tag", "") == state) return
        if (Build.VERSION.SDK_INT >= 33 &&
            c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val nm = c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "App updates",
                    if (BEEP) NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_LOW)
            )
        }
        val launch = Intent(c, UpdateEntryActivity::class.java)
        launch.putExtra(EXTRA_AUTO_UPDATE, true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val open = PendingIntent.getActivity(c, 0, launch, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val n = NotificationCompat.Builder(c, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Update available")
            .setContentText("Version " + latest.versionName + (if (ready) " downloaded. Tap to install." else " is available. Tap to download."))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        nm.notify(NOTIFICATION_ID, n)
        prefs.edit().putString("notified_tag", state).apply()
    }
}

class UpdateAlertWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try { UpdateAlert.checkAndNotify(applicationContext) } catch (_: Exception) { }
        Result.success()
    }
}
