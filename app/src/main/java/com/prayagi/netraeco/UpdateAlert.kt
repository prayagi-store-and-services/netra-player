package com.prayagi.netraplayer

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
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
 * Update alert: every 6 hours (network required) asks the repo's latest release whether a newer version exists,
 * posts one notification per new version and downloads the verified build in the background (UpdateDownloadWorker).
 * When the download finishes the notification says it is ready to install. Android always asks the user before an
 * install, so tapping still opens the installer - but the file is already there and verified, so no wait and no
 * second download. If the background download has not finished, tapping downloads in the foreground as before.
 */
object UpdateAlert {
    const val EXTRA_AUTO_UPDATE = "netra_auto_update"
    private const val NOTIFICATION_ID = 7431
    private const val WORK_NAME = "netra_update_alert"
    internal const val PREFS = "netra_update_alert"
    // Netra Eco beeps (default notification sound); the other apps use a silent channel.
    private const val BEEP = false
    private val CHANNEL_ID = if (BEEP) "netra_app_update_beep" else "netra_app_update"

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
        Toast.makeText(app, "Downloading the update...", Toast.LENGTH_SHORT).show()
        Thread {
            try {
                val self = Net.selfApp(app)
                val installed = Net.installed(app, self.packageName)
                val latest = Net.fetchLatest(self)
                if (latest != null && statusForRelease(installed?.first, installed?.second, latest) == Status.UpdateAvailable) {
                    val ready = verifiedFileFor(app, self, latest)
                    val file = ready ?: Net.download(app, self, latest)
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
        if (prefs.getString("notified_tag", "") == latest.tag) return
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
            .setContentText("Version " + latest.versionName + " is ready. Tap to update.")
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        nm.notify(NOTIFICATION_ID, n)
        prefs.edit().putString("notified_tag", latest.tag).apply()
        enqueueBackgroundDownload(c)
    }

    /** One background download for the newest version. Unique: a second check never starts a duplicate. */
    fun enqueueBackgroundDownload(c: Context) {
        val request = androidx.work.OneTimeWorkRequestBuilder<UpdateDownloadWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(androidx.work.BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(c).enqueueUniqueWork("netra_update_download", androidx.work.ExistingWorkPolicy.KEEP, request)
    }

    /** The verified file for a tag, or null when it is missing or fails the integrity re-check. Never trusts a partial. */
    internal fun verifiedFileFor(c: Context, app: CatalogApp, latest: LatestRelease): java.io.File? {
        val file = java.io.File(java.io.File(c.cacheDir, "updates"), "${app.id}-${latest.tag}.apk")
        return if (file.isFile && ApkIntegrity.matches(file, latest.size, latest.sha256)) file else null
    }

    /** "Download finished" notification: states only what is true (verified file on disk). */
    internal fun notifyReadyToInstall(c: Context, latest: LatestRelease) {
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
            .setContentTitle("Update downloaded")
            .setContentText("Version " + latest.versionName + " is downloaded and verified. Tap to install; Android will ask once before installing.")
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        nm.notify(NOTIFICATION_ID, n)
    }
}

class UpdateAlertWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try { UpdateAlert.checkAndNotify(applicationContext) } catch (_: Exception) { }
        Result.success()
    }
}

/**
 * Background update download: downloads and verifies the newest APK while the app is not open,
 * then posts an honest "downloaded, tap to install" notification. On failure it retries with
 * backoff; nothing is notified for a file that did not verify.
 */
class UpdateDownloadWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val c = applicationContext
        try {
            val self = Net.selfApp(c)
            val latest = Net.fetchLatest(self) ?: return@withContext Result.success()
            val installed = Net.installed(c, self.packageName)
            if (statusForRelease(installed?.first, installed?.second, latest) != Status.UpdateAvailable) {
                return@withContext Result.success()
            }
            val prefs = c.getSharedPreferences(UpdateAlert.PREFS, Context.MODE_PRIVATE)
            val file = UpdateAlert.verifiedFileFor(c, self, latest) ?: Net.download(c, self, latest)
            // Re-check what is on disk before telling the user anything.
            if (!ApkIntegrity.matches(file, latest.size, latest.sha256)) return@withContext Result.retry()
            prefs.edit().putString("downloaded_tag", latest.tag).apply()
            UpdateAlert.notifyReadyToInstall(c, latest)
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.success()
        }
    }
}
