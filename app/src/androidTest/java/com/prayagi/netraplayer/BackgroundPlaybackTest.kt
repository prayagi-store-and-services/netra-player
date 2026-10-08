package com.prayagi.netraplayer

import android.content.ComponentName
import androidx.lifecycle.Lifecycle
import androidx.media3.common.MediaItem
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
    @Test fun localAudioContinuesWithActivityStopped() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
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
        android.os.ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(
            "pm grant com.prayagi.netraplayer android.permission.POST_NOTIFICATIONS")).use { it.readBytes() }
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
                    controller!!.setMediaItems(listOf(
                        MediaItem.Builder().setUri(wav.toURI().toString()).setMediaId("test-a").build(),
                        MediaItem.Builder().setUri(wav.toURI().toString()).setMediaId("test-b").build()))
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
                        if (item?.mediaId == "test-b") advanced.countDown()
                    }
                })
                controller!!.seekTo(19_500)
            }
            assertTrue("Queue advanced to next local item", advanced.await(10, TimeUnit.SECONDS))
            android.os.ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(
                "cmd statusbar expand-notifications")).use { it.readBytes() }
            Thread.sleep(600)
            android.os.ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(
                "screencap -p /data/local/tmp/player-background.png")).use { it.readBytes() }
            scenario.moveToState(Lifecycle.State.RESUMED)
            instrumentation.waitForIdleSync()
            Thread.sleep(700)
            android.os.ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(
                "cmd statusbar collapse")).use { it.readBytes() }
            android.os.ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(
                "screencap -p /data/local/tmp/player-loaded.png")).use { it.readBytes() }
            instrumentation.runOnMainSync { controller!!.pause(); controller!!.release() }
        }
        wav.delete()
    }
}
