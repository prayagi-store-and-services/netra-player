package com.prayagi.netraplayer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.io.File
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

/** Stand-in for the retired UpdateAlertWorker; only its unique work name matters. */
class LegacyAlertWorkerStub(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = Result.success()
}

/**
 * Device tests for the 1.1.17 migration: leftovers from the old self-updater (scheduled work,
 * notification + channel, cached APKs, updater records) must really be gone after purge(), the
 * retired update entry must not be loadable (its old notification PendingIntent targets it), and
 * the Netra Eco presence check must answer honestly whether Eco is there or not.
 */
@RunWith(AndroidJUnit4::class)
class LegacyMigrationTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun legacyPrefsAndCachedApksAreDeleted() {
        val alert = context.getSharedPreferences(LegacyUpdateCleanup.ALERT_PREFS, Context.MODE_PRIVATE)
        val downloads = context.getSharedPreferences(LegacyUpdateCleanup.DOWNLOAD_PREFS, Context.MODE_PRIVATE)
        alert.edit().putString("notified_tag", "v1.1.16.8").putString("downloaded_tag", "v1.1.16.8").commit()
        downloads.edit().putString("netra-player-v1.1.16.8.apk", "old-record").commit()
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        val apk = File(dir, "netra-player-v1.1.16.8.apk").apply { writeText("old apk bytes") }
        val part = File(dir, "pending-old.part").apply { writeText("partial") }
        try {
            LegacyUpdateCleanup.purge(context)
            assertFalse(alert.contains("notified_tag"))
            assertFalse(alert.contains("downloaded_tag"))
            assertFalse(downloads.contains("netra-player-v1.1.16.8.apk"))
            assertFalse(apk.exists())
            assertFalse(part.exists())
        } finally {
            alert.edit().clear().commit(); downloads.edit().clear().commit()
            apk.delete(); part.delete()
        }
    }

    @Test fun oldUpdateChannelIsRemoved() {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel(LegacyUpdateCleanup.UPDATE_CHANNEL_ID, "App updates", NotificationManager.IMPORTANCE_LOW))
        assertTrue(nm.getNotificationChannel(LegacyUpdateCleanup.UPDATE_CHANNEL_ID) != null)
        try {
            LegacyUpdateCleanup.purge(context)
            assertNull(nm.getNotificationChannel(LegacyUpdateCleanup.UPDATE_CHANNEL_ID))
        } finally {
            nm.deleteNotificationChannel(LegacyUpdateCleanup.UPDATE_CHANNEL_ID)
        }
    }

    @Test fun queuedLegacyUpdateWorkIsCancelled() {
        val wm = WorkManager.getInstance(context)
        wm.cancelUniqueWork(LegacyUpdateCleanup.ALERT_WORK)
        val request = PeriodicWorkRequestBuilder<LegacyAlertWorkerStub>(6, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        wm.enqueueUniquePeriodicWork(LegacyUpdateCleanup.ALERT_WORK, ExistingPeriodicWorkPolicy.UPDATE, request)
        val before = wm.getWorkInfosForUniqueWork(LegacyUpdateCleanup.ALERT_WORK).get(10, TimeUnit.SECONDS)
        assertTrue(before.isNotEmpty() && before.none { it.state == WorkInfo.State.CANCELLED })
        LegacyUpdateCleanup.purge(context)
        val end = System.currentTimeMillis() + 15000
        var cancelled = false
        while (System.currentTimeMillis() < end) {
            val infos = wm.getWorkInfosForUniqueWork(LegacyUpdateCleanup.ALERT_WORK).get(10, TimeUnit.SECONDS)
            if (infos.isNotEmpty() && infos.all { it.state == WorkInfo.State.CANCELLED }) { cancelled = true; break }
            Thread.sleep(300)
        }
        assertTrue("legacy update work must be CANCELLED after purge", cancelled)
    }

    @Test fun oldUpdateEntryActivityIsGone() {
        try {
            Class.forName("com.prayagi.netraplayer.UpdateEntryActivity")
            fail("UpdateEntryActivity must stay removed - old update notifications point at it")
        } catch (_: ClassNotFoundException) {
            // expected
        }
    }

    @Test fun ecoPresenceCheckAnswersBothWays() {
        // Self is always visible: proves the helper says true for an installed package.
        assertTrue(isPackageInstalled(context, context.packageName))
        // A bogus id is never installed: proves the absent path.
        assertFalse(isPackageInstalled(context, "com.prayagi.no.such.app"))
        // Eco itself: the answer must match the package manager exactly (queries declaration works).
        val pmSays = try {
            context.packageManager.getPackageInfo(NETRA_ECO_PACKAGE, 0); true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
        assertEquals(pmSays, isPackageInstalled(context, NETRA_ECO_PACKAGE))
        // The update card text must follow the real state.
        assertTrue(updateSourceMessage(pmSays).contains("Netra Eco"))
    }
}
