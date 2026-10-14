package com.prayagi.netraplayer

import android.Manifest
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocalLibraryTest {
    private val ins get() = InstrumentationRegistry.getInstrumentation()
    private fun shell(command: String) = android.os.ParcelFileDescriptor.AutoCloseInputStream(ins.uiAutomation.executeShellCommand(command)).use { it.readBytes() }
    @Test fun permissionBoundsAndPermittedMetadata() {
        val c = ins.targetContext
        val permissions = LocalMediaLibrary.permissions()
        assertFalse("Never all-files access", permissions.contains(Manifest.permission.MANAGE_EXTERNAL_STORAGE))
        assertEquals("No access means no query rows", emptyList<LibraryMedia>(), LocalMediaLibrary.scan(c, LibraryAccess(false, false, false)))
        run {
            permissions.forEach { shell("pm grant ${c.packageName} $it") }
            val access = LocalMediaLibrary.access(c)
            assertTrue("Full video access granted", access.video)
            assertTrue("Audio access granted", access.audio)
            var rows = LocalMediaLibrary.scan(c, access)
            val ready = System.currentTimeMillis() + 10000
            while (rows.none { it.name == "a-video.mp4" } && System.currentTimeMillis() < ready) {
                Thread.sleep(250); rows = LocalMediaLibrary.scan(c, access)
            }
            val video = rows.firstOrNull { it.name == "a-video.mp4" }
            assertNotNull("Device fixture discovered via MediaStore", video)
            assertTrue("Duration from local metadata", video!!.durationMs > 0)
            assertTrue("Resolution from local metadata", video.width > 0 && video.height > 0)
            assertTrue("No filesystem playback URI", video.uri.startsWith("content://media/"))
            assertFalse("No absolute folder path", video.folder.startsWith("/"))
            if (Build.VERSION.SDK_INT >= 29) {
                val thumb = c.contentResolver.loadThumbnail(android.net.Uri.parse(video.uri), android.util.Size(144, 96), null)
                assertTrue("Real local thumbnail", thumb.width > 0 && thumb.height > 0)
            }
            val videosOnly = LocalMediaLibrary.scan(c, LibraryAccess(true, false, false))
            assertTrue("Audio category not queried when denied", videosOnly.all { it.video })
        }
        // Real revocation/resume requires a separate host-side scenario, not a fake in-process check.
    }
}
