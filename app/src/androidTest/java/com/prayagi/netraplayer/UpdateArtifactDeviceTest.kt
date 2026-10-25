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
    @Test fun notificationRequiresChoiceAndNotNowStartsNoDownload() {
        val ins = InstrumentationRegistry.getInstrumentation()
        val c = ins.targetContext
        val before = DownloadCenter.items.value
        val intent = android.content.Intent(c, UpdateEntryActivity::class.java)
            .putExtra(UpdateAlert.EXTRA_AUTO_UPDATE,true).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        androidx.test.core.app.ActivityScenario.launch<UpdateEntryActivity>(intent).use {
            val deadline = System.currentTimeMillis()+10000
            var cancel: android.view.accessibility.AccessibilityNodeInfo? = null
            fun nodes(n: android.view.accessibility.AccessibilityNodeInfo?): List<android.view.accessibility.AccessibilityNodeInfo> =
                if(n==null) emptyList() else listOf(n)+(0 until n.childCount).flatMap { nodes(n.getChild(it)) }
            while(System.currentTimeMillis()<deadline && cancel==null) {
                cancel=nodes(ins.uiAutomation.rootInActiveWindow).firstOrNull { it.text?.toString()?.equals("Not now",ignoreCase=true)==true }
                if(cancel==null) Thread.sleep(100)
            }
            assertNotNull("Explicit Not now choice",cancel)
            val texts=nodes(ins.uiAutomation.rootInActiveWindow).mapNotNull { it.text?.toString() }
            assertTrue(texts.any { it.contains("Download Netra Player update?") })
            assertTrue(texts.any { it.contains("Keep Play Protect enabled") })
            android.os.ParcelFileDescriptor.AutoCloseInputStream(ins.uiAutomation.executeShellCommand("screencap -p /data/local/tmp/player-update-choice.png")).use { it.readBytes() }
            assertTrue(cancel!!.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK))
            ins.waitForIdleSync()
            assertEquals(before,DownloadCenter.items.value)
            assertFalse(Net.downloadsRunning())
        }
    }
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
