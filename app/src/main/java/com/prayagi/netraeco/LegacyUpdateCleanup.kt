package com.prayagi.netraplayer

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.work.WorkManager
import java.io.File

/**
 * One-time removal of the old self-updater's leftovers. Versions up to 1.1.16.8 checked for a new
 * release every 6 hours, downloaded the APK in the background and kept it in app cache until install.
 * Updates now come from the Netra Eco app, so on the first start after the upgrade the scheduled
 * update work is cancelled, any updater notification still showing is removed (its tap target no
 * longer exists), the updater's notification channel is deleted, and downloaded files and updater
 * records are wiped. The media playback channel is not touched.
 */
object LegacyUpdateCleanup {
    internal const val ALERT_WORK = "netra_update_alert"
    internal const val DOWNLOAD_WORK = "netra_update_download"
    internal const val ALERT_PREFS = "netra_update_alert"
    internal const val DOWNLOAD_PREFS = "netra_eco_downloads"
    internal const val UPDATE_NOTIFICATION_ID = 7431
    internal const val UPDATE_CHANNEL_ID = "netra_app_update"

    fun purge(context: Context) {
        try { WorkManager.getInstance(context).cancelUniqueWork(ALERT_WORK) } catch (_: Exception) {}
        try { WorkManager.getInstance(context).cancelUniqueWork(DOWNLOAD_WORK) } catch (_: Exception) {}
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(UPDATE_NOTIFICATION_ID)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) nm.deleteNotificationChannel(UPDATE_CHANNEL_ID)
        } catch (_: Exception) {}
        try { context.getSharedPreferences(ALERT_PREFS, Context.MODE_PRIVATE).edit().clear().apply() } catch (_: Exception) {}
        try { context.getSharedPreferences(DOWNLOAD_PREFS, Context.MODE_PRIVATE).edit().clear().apply() } catch (_: Exception) {}
        try { cleanDir(File(context.cacheDir, "updates")) } catch (_: Exception) {}
    }

    /** Deletes every file in the folder and returns how many were removed. */
    fun cleanDir(dir: File?): Int {
        var n = 0
        dir?.listFiles()?.forEach { if (it.delete()) n++ }
        return n
    }
}
