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
    @Test fun folderGridTapStartsLocalQueue() {
        val c = ins.targetContext
        LocalMediaLibrary.permissions().forEach { shell("pm grant ${c.packageName} $it") }
        val files = LocalMediaLibrary.scan(c, LocalMediaLibrary.access(c))
        val fixture = files.first { it.name == "a-video.mp4" }
        fun find(n: android.view.accessibility.AccessibilityNodeInfo?, text: String): android.view.accessibility.AccessibilityNodeInfo? {
            if (n == null) return null
            if (n.text?.toString() == text) return n
            for (i in 0 until n.childCount) find(n.getChild(i), text)?.let { return it }
            return null
        }
        fun node(text: String): android.view.accessibility.AccessibilityNodeInfo {
            val end = System.currentTimeMillis() + 10000
            while (System.currentTimeMillis() < end) {
                find(ins.uiAutomation.rootInActiveWindow, text)?.let { return it }
                shell("input swipe 20 1200 20 500 300"); Thread.sleep(250)
            }
            shell("screencap -p /data/local/tmp/player-library-failure.png")
            throw AssertionError("Missing library UI $text")
        }
        fun click(text: String) {
            var n: android.view.accessibility.AccessibilityNodeInfo? = node(text)
            while (n != null && !n.isClickable) n = n.parent
            assertTrue("Activate library $text", n?.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK) == true)
            ins.waitForIdleSync(); Thread.sleep(500)
        }
        androidx.test.core.app.ActivityScenario.launch(MainActivity::class.java).use {
            node("Search local files")
            node(fixture.folder)
            shell("screencap -p /data/local/tmp/player-library-folders.png")
            click(fixture.folder)
            node("All folders")
            node(fixture.name)
            shell("screencap -p /data/local/tmp/player-library-media.png")
            click(fixture.name)
            var controller: androidx.media3.session.MediaController? = null
            val ready = java.util.concurrent.CountDownLatch(1)
            ins.runOnMainSync {
                val f = androidx.media3.session.MediaController.Builder(c, androidx.media3.session.SessionToken(c,
                    android.content.ComponentName(c, PlaybackService::class.java))).buildAsync()
                f.addListener({ controller = f.get(); ready.countDown() }, androidx.core.content.ContextCompat.getMainExecutor(c))
            }
            assertTrue("Library player connected", ready.await(10, java.util.concurrent.TimeUnit.SECONDS))
            var matched = false
            repeat(30) {
                ins.runOnMainSync { matched = controller!!.currentMediaItem?.mediaId == fixture.uri && controller!!.mediaItemCount >= 2 && controller!!.playerError == null }
                if (!matched) Thread.sleep(200)
            }
            ins.runOnMainSync { controller!!.release() }
            assertTrue("Tap starts permitted file in folder queue", matched)
        }
    }

    @Test fun mp3MetadataAndDecoderStayLocal() {
        val c = ins.targetContext
        LocalMediaLibrary.permissions().forEach { shell("pm grant ${c.packageName} $it") }
        val end = System.currentTimeMillis() + 10000
        var mp3: LibraryMedia? = null
        while (mp3 == null && System.currentTimeMillis() < end) {
            mp3 = LocalMediaLibrary.scan(c, LocalMediaLibrary.access(c)).firstOrNull { it.name == "library-tone.mp3" }
            if (mp3 == null) Thread.sleep(200)
        }
        assertNotNull("MP3 is in permitted local library", mp3)
        assertFalse(mp3!!.video)
        assertTrue("MP3 duration", mp3!!.durationMs in 7000..9000)
        var player: androidx.media3.exoplayer.ExoPlayer? = null
        ins.runOnMainSync {
            player = androidx.media3.exoplayer.ExoPlayer.Builder(c).build()
            player!!.setMediaItem(androidx.media3.common.MediaItem.fromUri(mp3!!.uri))
            player!!.prepare(); player!!.play()
        }
        var ready = false
        repeat(40) {
            ins.runOnMainSync { ready = player!!.playbackState == androidx.media3.common.Player.STATE_READY && player!!.playerError == null && player!!.currentPosition > 0 }
            if (!ready) Thread.sleep(100)
        }
        ins.runOnMainSync { player!!.release() }
        assertTrue("Actual local MP3 decoder advances playback", ready)
    }

}
