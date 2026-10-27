package com.prayagi.netraplayer

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class UpdateIntegrityTest {
 @Test fun upgradeDropsAllLegacyInstallerArtifactsInsteadOfRestoringThem() {
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  val dir=File(context.cacheDir,"updates").apply{mkdirs()}
  val file=File(dir,"legacy.apk").apply{writeText("retired updater fixture")}
  val prefs=context.getSharedPreferences("netra_eco_downloads",android.content.Context.MODE_PRIVATE)
  prefs.edit().putString("legacy.apk","old installer record").commit()
  AppUpdater.retireLegacyUpdater(context)
  val deadline=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(5)
  while(file.exists()&&System.nanoTime()<deadline)Thread.sleep(10)
  assertFalse(file.exists());assertTrue(prefs.all.isEmpty())
  val requested=context.packageManager.getPackageInfo(context.packageName,android.content.pm.PackageManager.GET_PERMISSIONS).requestedPermissions.orEmpty()
  assertFalse(requested.contains("android.permission.REQUEST_INSTALL_PACKAGES"))
 }
}
