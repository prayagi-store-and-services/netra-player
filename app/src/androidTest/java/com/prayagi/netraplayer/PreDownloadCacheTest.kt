package com.prayagi.netraplayer

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import java.security.MessageDigest

class PreDownloadCacheTest {
    @Test fun verifiedCacheSurvivesRefreshThenCleansOnlyWhenInstalled() {
        val c = InstrumentationRegistry.getInstrumentation().targetContext
        val app = Net.selfApp(c)
        val bytes = "Local cache integrity test".toByteArray()
        val sha = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        val installed = Net.installed(c, c.packageName)!!
        val release = LatestRelease("v99.0.0", "99.0.0", installed.first + 1, "", "", sha, bytes.size.toLong())
        val key = DownloadCenter.keyFor(app, release)
        val f = DownloadCenter.file(c, key)
        try {
            f.writeBytes(bytes)
            assertTrue(DownloadCenter.hasVerifiedFile(c, app, release))
            DownloadCenter.register(c, app, release)
            DownloadCenter.refresh(c)
            assertEquals(DownloadCenter.DONE, DownloadCenter.items.value[key]!!.state)
            DownloadCenter.cleanFinished(c)
            assertTrue("An uninstalled newer version remains saved", f.exists())
            f.appendText("changed")
            assertFalse(DownloadCenter.hasVerifiedFile(c, app, release))
            DownloadCenter.refresh(c)
            assertFalse("Tampered cache is removed", f.exists())
            f.writeBytes(bytes)
            val installedRelease = release.copy(versionName = installed.second, versionCode = installed.first)
            DownloadCenter.register(c, app, installedRelease)
            DownloadCenter.cleanFinished(c)
            assertFalse("Installed version is cleaned", f.exists())
        } finally { DownloadCenter.delete(c, key) }
    }
}
