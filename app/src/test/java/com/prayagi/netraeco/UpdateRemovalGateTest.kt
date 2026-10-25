package com.prayagi.netraplayer

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Hard gate for v1.1.17: Netra Player no longer checks for, downloads or installs updates itself.
 * Updates come only from the Netra Eco app. This test fails if any piece of the old self-updater
 * (install permission, FileProvider, update entry activity, APK download/install code, update
 * workers or update prompts) comes back in the app.
 */
class UpdateRemovalGateTest {
    private fun projectFile(path: String): File {
        val fromModule = File(path)
        if (fromModule.isFile) return fromModule
        val fromRepoRoot = File("app/$path")
        assertTrue("Project file missing: $path", fromRepoRoot.isFile)
        return fromRepoRoot
    }

    @Test fun manifestHasNoInstallPermissionProviderOrUpdateEntry() {
        val m = projectFile("src/main/AndroidManifest.xml").readText()
        assertFalse("REQUEST_INSTALL_PACKAGES must stay removed", m.contains("REQUEST_INSTALL_PACKAGES"))
        assertFalse("update FileProvider must stay removed", m.contains("FileProvider"))
        assertFalse("UpdateEntryActivity must stay removed", m.contains("UpdateEntryActivity"))
        assertFalse("updates provider authority must stay removed", m.contains(".updates"))
        assertFalse("update_paths reference must stay removed", m.contains("update_paths"))
    }

    @Test fun updatePathsResourceStaysDeleted() {
        assertFalse(File("src/main/res/xml/update_paths.xml").isFile)
        assertFalse(File("app/src/main/res/xml/update_paths.xml").isFile)
    }

    @Test fun sourcesHaveNoApkDownloadInstallOrUpdatePrompt() {
        val forbidden = listOf(
            "REQUEST_INSTALL_PACKAGES",
            "ACTION_MANAGE_UNKNOWN_APP_SOURCES",
            "application/vnd.android.package-archive",
            "canRequestPackageInstalls",
            "FileProvider",
            "UpdateAlert",
            "UpdateEntryActivity",
            "DownloadCenter",
            "ApkIntegrity",
            "SelfDownloadButton",
            "DownloadManagerCard",
            "Check for update",
            "apkUrl",
            "app-release.apk",
            "stat_sys_download"
        )
        val srcDir = listOf(File("src/main/java"), File("app/src/main/java")).firstOrNull { it.isDirectory }
        assertTrue("app source dir missing", srcDir != null)
        val hits = mutableListOf<String>()
        srcDir!!.walkTopDown().filter { it.isFile && it.extension == "kt" }.forEach { f ->
            val text = f.readText()
            forbidden.forEach { token -> if (text.contains(token)) hits.add(f.name + ": " + token) }
        }
        assertTrue("Self-updater leftovers found: $hits", hits.isEmpty())
    }
}
