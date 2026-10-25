package com.prayagi.netraplayer

import android.content.Context
import android.content.Intent
import android.os.ParcelFileDescriptor
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.security.MessageDigest

/** Uses owning production-signed BSPN releases, never an injectable trust override. */
@RunWith(AndroidJUnit4::class)
class UpdatePositiveDeviceTest {
    private val ins get() = InstrumentationRegistry.getInstrumentation()
    private val c get() = ins.targetContext
    private val pkg = "com.aistudio.batterysentinel.ntra"
    private fun shell(command: String): String = ParcelFileDescriptor.AutoCloseInputStream(ins.uiAutomation.executeShellCommand(command)).use { String(it.readBytes()) }
    private fun fixture(code: Int): File {
        val f = File(File(c.cacheDir,"updates").apply { mkdirs() },"bspn-proof-$code.apk")
        ParcelFileDescriptor.AutoCloseInputStream(ins.uiAutomation.executeShellCommand("cat /data/local/tmp/bspn-proof-$code.apk")).use { input -> f.outputStream().use { input.copyTo(it) } }
        assertEquals(if(code==90)17687136L else 17687140L,f.length())
        assertEquals(hash(code),MessageDigest.getInstance("SHA-256").digest(f.readBytes()).joinToString("") { "%02x".format(it) })
        return f
    }
    private fun hash(code:Int) = if(code==90) "af034734bfa2b69f901a613b58bff0c9eeb8acf11e6caf4989cd94699b729d52" else "493ce43f8b9ba50e1540bfdf4bdb0cedef80a3ea66f7e6eef1f339099c6c3a6f"
    private fun release(code:Int,f:File) = LatestRelease("v2.2.${code-80}","2.2.${code-80}",code.toLong(),"","",hash(code),f.length())
    private fun nodes(n:AccessibilityNodeInfo?):List<AccessibilityNodeInfo> = if(n==null) emptyList() else listOf(n)+(0 until n.childCount).flatMap { nodes(n.getChild(it)) }
    private fun absent() { shell("pm uninstall $pkg"); assertNull("Fixture absent",Net.installed(c,pkg)) }
    @Test fun realSignedFirstInstallOlderUpgradeAndExternalInstallerReadsUri() {
        val old=fixture(90);val newer=fixture(91)
        // Production resume refresh deletes unregistered cache. Use the real restored-download schema.
        val downloads=c.getSharedPreferences("netra_eco_downloads",Context.MODE_PRIVATE)
        for ((f,code) in listOf(old to 90,newer to 91)) {
            check(downloads.edit().putString(f.name,listOf(pkg,"2.2.${code-80}",code,f.length(),"BSPN proof","battery-sentinel",hash(code)).joinToString("|")).commit())
        }
        val pins=c.getSharedPreferences("verified_update_artifacts",Context.MODE_PRIVATE)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            try {
                for ((f,code) in listOf(old to 90,newer to 91)) {
                    assertTrue("Registered fixture survives launch: $code",f.isFile)
                    assertEquals("Post-launch fixture digest: $code",hash(code),MessageDigest.getInstance("SHA-256").digest(f.readBytes()).joinToString("") { "%02x".format(it) })
                }
                absent()
                Net.verifyArchive(c,newer,pkg,release(91,newer))
                assertTrue(shell("pm install -r /data/local/tmp/bspn-proof-90.apk").contains("Success"))
                assertEquals(90L,Net.installed(c,pkg)?.first)
                Net.verifyArchive(c,newer,pkg,release(91,newer))
                try { Net.verifyArchive(c,old,pkg,release(90,old));fail("Installed same-version archive accepted") }
                catch(e:IllegalStateException) { assertTrue(e.message!!.contains("identity did not match")) }
                shell("appops set ${c.packageName} REQUEST_INSTALL_PACKAGES allow")
                assertTrue(shell("appops get ${c.packageName} REQUEST_INSTALL_PACKAGES").contains("allow"))
                assertTrue(c.packageManager.canRequestPackageInstalls())
                assertTrue(pins.edit().putString(newer.name,JSONObject().put("package",pkg).put("size",newer.length()).put("sha256",hash(91)).put("code",91).put("version","2.2.11").toString()).commit())
                scenario.onActivity { Net.install(it,newer) }
                val deadline=System.currentTimeMillis()+15000
                var tree=emptyList<AccessibilityNodeInfo>()
                while(System.currentTimeMillis()<deadline) {
                    tree=nodes(ins.uiAutomation.rootInActiveWindow)
                    if(tree.any { it.packageName?.toString()?.contains("packageinstaller")==true } && tree.any { it.text?.toString()=="Netra Battery Sentinel Pro" }) break
                    Thread.sleep(100)
                }
                val text=tree.mapNotNull { it.text?.toString() }
                File(c.filesDir,"positive-installer-ui.txt").writeText(tree.joinToString("\n") { "${it.packageName}|${it.text}|${it.contentDescription}" })
                shell("screencap -p /data/local/tmp/player-update-installer.png")
                assertTrue("External Android installer process",tree.any { it.packageName?.toString()?.contains("packageinstaller")==true })
                assertTrue("Installer decoded APK application label",text.contains("Netra Battery Sentinel Pro"))
                assertTrue("Installer confirmation, not an error",text.any { it.equals("Update",true)||it.equals("Install",true) })
                shell("screencap -p /data/local/tmp/player-update-installer.png")
                // Never confirm installation from UI. Production parsing above verifies exact version91.
                shell("input keyevent 4")
                assertEquals("No UI install happened",90L,Net.installed(c,pkg)?.first)
            } finally {
                shell("input keyevent 4")
                absent();old.delete();newer.delete();pins.edit().remove(newer.name).commit()
                downloads.edit().remove(old.name).remove(newer.name).commit()
                DownloadCenter.refresh(c)
                check(!pins.contains(newer.name))
                check(!downloads.contains(old.name) && !downloads.contains(newer.name))
                check(!old.exists() && !newer.exists())
                android.util.Log.i("UpdatePositiveProof", "Fixture absent; files and preference records removed before host permission cleanup")
            }
        }
    }
}
