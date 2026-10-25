package com.prayagi.netraplayer
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
@RunWith(AndroidJUnit4::class)
class UpdateArtifactDeviceTest {
    @Test fun unregisteredCacheIsRefusedBeforeInstallerLaunch() {
        val c = InstrumentationRegistry.getInstrumentation().targetContext
        val f = File(File(c.cacheDir,"updates").apply { mkdirs() }, "old-unregistered.apk")
        c.getSharedPreferences("verified_update_artifacts",Context.MODE_PRIVATE).edit().remove(f.name).commit()
        f.writeText("not an APK")
        try { Net.install(c,f); fail("Unregistered file accepted") }
        catch (e: IllegalStateException) { assertTrue(e.message!!.contains("fresh verification")) }
        assertFalse(f.exists())
    }
    @Test fun sameInstalledRealArchiveCannotBeReinstalledAsUpdate() {
        val c = InstrumentationRegistry.getInstrumentation().targetContext
        val f = File(File(c.cacheDir,"updates").apply { mkdirs() }, "same-installed.apk")
        File(c.applicationInfo.sourceDir).copyTo(f,overwrite=true)
        val digest=java.security.MessageDigest.getInstance("SHA-256").digest(f.readBytes()).joinToString("") { "%02x".format(it) }
        try {
            Net.verifyArchive(c,f,c.packageName,LatestRelease("v"+BuildConfig.VERSION_NAME,BuildConfig.VERSION_NAME,BuildConfig.VERSION_CODE.toLong(),"","",digest,f.length()))
            fail("Same installed APK accepted as newer update")
        } catch (e: IllegalStateException) { assertTrue(e.message!!.contains("identity did not match")) }
        finally { f.delete() }
    }
}
