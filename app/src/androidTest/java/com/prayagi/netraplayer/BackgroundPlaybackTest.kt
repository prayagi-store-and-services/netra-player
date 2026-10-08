package com.prayagi.netraplayer

import android.content.ComponentName
import android.view.accessibility.AccessibilityNodeInfo
import androidx.lifecycle.Lifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class BackgroundPlaybackTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private fun shell(command: String) = android.os.ParcelFileDescriptor.AutoCloseInputStream(
        instrumentation.uiAutomation.executeShellCommand(command)).use { it.readBytes() }
    private fun find(node: AccessibilityNodeInfo?, predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        if (node == null) return null
        if (predicate(node)) return node
        for (i in 0 until node.childCount) find(node.getChild(i), predicate)?.let { return it }
        return null
    }
    private fun awaitNode(label: String, predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo {
        val until = System.currentTimeMillis() + 10000
        while (System.currentTimeMillis() < until) {
            find(instrumentation.uiAutomation.rootInActiveWindow, predicate)?.let { return it }
            Thread.sleep(200)
        }
        throw AssertionError("Missing $label")
    }
    private fun capture(name: String) {
        instrumentation.uiAutomation.waitForIdle(500, 5000)
        shell("screencap -p /data/local/tmp/player-$name.png")
    }
    @Test fun localAudioContinuesWithActivityStopped() {
        val context = instrumentation.targetContext
        val wav = File(context.filesDir, "device-test.wav")
        val sampleRate = 8000
        val dataSize = sampleRate * 2 * 20
        val buffer = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put("RIFF".toByteArray()).putInt(36 + dataSize).put("WAVEfmt ".toByteArray())
        buffer.putInt(16).putShort(1).putShort(1).putInt(sampleRate).putInt(sampleRate * 2)
            .putShort(2).putShort(16).put("data".toByteArray()).putInt(dataSize)
        while (buffer.hasRemaining()) buffer.putShort(0)
        wav.writeBytes(buffer.array())
        shell("pm grant com.prayagi.netraplayer android.permission.POST_NOTIFICATIONS")
        var controller: MediaController? = null
        val connected = CountDownLatch(1)
        val playing = CountDownLatch(1)
        val advanced = CountDownLatch(1)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            instrumentation.runOnMainSync {
                val future = MediaController.Builder(context,
                    SessionToken(context, ComponentName(context, PlaybackService::class.java))).buildAsync()
                future.addListener({
                    controller = future.get()
                    controller!!.addListener(object : Player.Listener {
                        override fun onIsPlayingChanged(isPlaying: Boolean) { if (isPlaying) playing.countDown() }
                    })
                    val uri = wav.toURI().toString()
                    controller!!.setMediaItems(listOf("First test audio", "Second test audio").map { title ->
                        MediaItem.Builder().setUri(uri).setMediaId(uri)
                            .setMediaMetadata(MediaMetadata.Builder().setTitle(title).build()).build() })
                    controller!!.prepare()
                    controller!!.play()
                    connected.countDown()
                }, androidx.core.content.ContextCompat.getMainExecutor(context))
            }
            assertTrue("Service connected", connected.await(15, TimeUnit.SECONDS))
            assertTrue("Local audio started", playing.await(15, TimeUnit.SECONDS))
            var before = 0L
            instrumentation.runOnMainSync { before = controller!!.currentPosition }
            scenario.moveToState(Lifecycle.State.CREATED)
            Thread.sleep(1800)
            instrumentation.runOnMainSync {
                assertTrue("Audio remained playing after activity stop", controller!!.isPlaying)
                assertTrue("Playback position advanced in background", controller!!.currentPosition > before + 500)
                controller!!.addListener(object : Player.Listener {
                    override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
                        if (controller!!.currentMediaItemIndex == 1) advanced.countDown()
                    }
                })
                controller!!.seekTo(19_500)
            }
            assertTrue("Queue advanced to next local item", advanced.await(10, TimeUnit.SECONDS))
            shell("cmd statusbar expand-notifications")
            awaitNode("media title") { it.text?.contains("Second test audio") == true }
            val pause = awaitNode("notification Pause") { it.contentDescription?.toString()?.equals("Pause", true) == true }
            capture("background")
            assertTrue("Notification Pause is clickable", pause.performAction(AccessibilityNodeInfo.ACTION_CLICK))
            awaitNode("notification Play") { it.contentDescription?.toString()?.equals("Play", true) == true }
            instrumentation.runOnMainSync { assertTrue("Notification paused playback", !controller!!.playWhenReady) }
            val play = awaitNode("notification Play") { it.contentDescription?.toString()?.equals("Play", true) == true }
            assertTrue("Notification Play is clickable", play.performAction(AccessibilityNodeInfo.ACTION_CLICK))
            awaitNode("notification Pause after Play") { it.contentDescription?.toString()?.equals("Pause", true) == true }
            instrumentation.runOnMainSync { assertTrue("Notification resumed playback", controller!!.playWhenReady) }
            shell("cmd statusbar collapse")
            scenario.moveToState(Lifecycle.State.RESUMED)
            awaitNode("loaded player") { it.text?.toString() == "Open another file" }
            capture("loaded")
            shell("cmd uimode night yes")
            awaitNode("dark player") { it.text?.toString() == "Open another file" }
            Thread.sleep(800)
            capture("dark")
            val fullscreen = awaitNode("Fullscreen control") { it.contentDescription?.toString()?.contains("fullscreen", true) == true }
            assertTrue("Fullscreen is clickable", fullscreen.performAction(AccessibilityNodeInfo.ACTION_CLICK))
            Thread.sleep(1500)
            capture("fullscreen")
            shell("input keyevent KEYCODE_BACK")
            shell("cmd uimode night no")
            instrumentation.runOnMainSync {
                controller!!.stop(); controller!!.clearMediaItems(); controller!!.release()
                playerFullscreen.value = false
            }
        }
        wav.delete()
    }
}
