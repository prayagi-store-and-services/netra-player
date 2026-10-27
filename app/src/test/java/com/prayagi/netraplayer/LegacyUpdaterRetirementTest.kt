package com.prayagi.netraplayer

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],application=Application::class)
class LegacyUpdaterRetirementTest {
 @Test fun persistedWorkersTerminateWithoutArtifactOrUpdateNotification()= runBlocking {
  val c=RuntimeEnvironment.getApplication()
  assertEquals(ListenableWorker.Result.success(),TestListenableWorkerBuilder<UpdateAlertWorker>(c).build().doWork())
  assertEquals(ListenableWorker.Result.success(),TestListenableWorkerBuilder<UpdateDownloadWorker>(c).build().doWork())
  assertFalse(File(c.cacheDir,"updates").exists())
  val nm=c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
  assertTrue(nm.activeNotifications.isEmpty())
 }
 @Test fun upgradeClearsLegacyPreferencesChannelAndCachedApk() {
  val c=RuntimeEnvironment.getApplication()
  val names=listOf("netra_update_alert","netra_app_update","verified_update_artifacts","netra_eco_downloads")
  names.forEach{c.getSharedPreferences(it,Context.MODE_PRIVATE).edit().putString("url","https://test.invalid/old.apk").commit()}
  val nm=c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
  nm.createNotificationChannel(NotificationChannel("netra_app_update","Old updater",NotificationManager.IMPORTANCE_LOW))
  val file=File(c.cacheDir,"updates/old.apk").apply{parentFile!!.mkdirs();writeText("stale fixture")}
  AppUpdater.retireLegacyUpdater(c)
  val deadline=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(5)
  while(file.exists()&&System.nanoTime()<deadline)Thread.sleep(10)
  assertFalse(file.exists())
  names.forEach{assertTrue(c.getSharedPreferences(it,Context.MODE_PRIVATE).all.isEmpty())}
  assertNull(nm.getNotificationChannel("netra_app_update"))
  assertEquals(listOf("netra_update_alert","netra_update_download"),AppUpdater.LEGACY_WORK_NAMES)
 }
 @Test fun manifestAndSourceHaveNoApkInstallerSurface() {
  val manifest=File("src/main/AndroidManifest.xml").readText()
  assertFalse(manifest.contains("REQUEST_INSTALL_PACKAGES"));assertFalse(manifest.contains("FileProvider"))
  assertTrue(manifest.contains("POST_NOTIFICATIONS"));assertTrue(manifest.contains("FOREGROUND_SERVICE_MEDIA_PLAYBACK"))
  val net=File("src/main/java/com/prayagi/netraeco/Net.kt").readText()
  assertFalse(net.contains("ACTION_INSTALL_PACKAGE"));assertFalse(net.contains("application/vnd.android.package-archive"))
  assertFalse(net.contains("fun downloadApk"));assertFalse(net.contains("fun installApk"))
 }
}
