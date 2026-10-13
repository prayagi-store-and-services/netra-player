package com.prayagi.netraplayer

import android.content.Intent
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class WidgetPlaybackTest {
    @Test fun commandsUseSessionAndPrivateRenderedWidgetHidesName() {
        val ins = InstrumentationRegistry.getInstrumentation()
        val c = ins.targetContext
        val info = c.packageManager.getReceiverInfo(android.content.ComponentName(c, WidgetPlaybackReceiver::class.java), 0)
        assertFalse("Widget command receiver is private", info.exported)
        val wav = File(c.filesDir, "widget-test.wav")
        val size = 8000 * 2 * 120
        val buffer = ByteBuffer.allocate(44 + size).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put("RIFF".toByteArray()).putInt(36 + size).put("WAVEfmt ".toByteArray())
        buffer.putInt(16).putShort(1).putShort(1).putInt(8000).putInt(16000)
            .putShort(2).putShort(16).put("data".toByteArray()).putInt(size)
        while (buffer.hasRemaining()) buffer.putShort(0)
        wav.writeBytes(buffer.array())
        var controller: MediaController? = null
        val ready = CountDownLatch(1)
        ins.runOnMainSync {
            val future = MediaController.Builder(c, SessionToken(c, android.content.ComponentName(c, PlaybackService::class.java))).buildAsync()
            future.addListener({ controller = future.get(); ready.countDown() }, androidx.core.content.ContextCompat.getMainExecutor(c))
        }
        assertTrue(ready.await(10, TimeUnit.SECONDS))
        fun await(check: () -> Boolean) {
            var ok = false
            val until = System.currentTimeMillis() + 5000
            while (!ok && System.currentTimeMillis() < until) {
                ins.runOnMainSync { ok = check() }
                if (!ok) Thread.sleep(50)
            }
            assertTrue("Widget command changed the actual session", ok)
        }
        fun command(action: String) = c.sendBroadcast(Intent(c, WidgetPlaybackReceiver::class.java).setAction(action))
        try {
            ins.runOnMainSync {
                controller!!.setMediaItems(listOf("Widget first", "Widget second").map {
                    MediaItem.Builder().setUri(android.net.Uri.fromFile(wav)).setMediaId(android.net.Uri.fromFile(wav).toString())
                        .setMediaMetadata(MediaMetadata.Builder().setTitle(it).build()).build()
                })
                controller!!.prepare(); controller!!.play()
            }
            await { controller!!.isPlaying && WidgetPlaybackState.playing }
            command(WidgetPlaybackReceiver.PLAY_PAUSE)
            await { !controller!!.playWhenReady && !WidgetPlaybackState.playing }
            command(WidgetPlaybackReceiver.PLAY_PAUSE)
            await { controller!!.isPlaying && WidgetPlaybackState.playing }
            command(WidgetPlaybackReceiver.NEXT)
            await { controller!!.currentMediaItemIndex == 1 && !WidgetPlaybackState.next }
            RecentFiles.setPrivate(c, false)
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                fun render(name: String) {
                    scenario.onActivity { activity ->
                        activity.setContent {
                            Box(Modifier.fillMaxSize().padding(16.dp)) {
                                androidx.compose.runtime.key(name) { AndroidView(modifier = Modifier.width(300.dp).height(180.dp), factory = { PlayerWidgetProvider.buildViews(it).apply(it, null) }) }
                            }
                        }
                    }
                    ins.waitForIdleSync(); Thread.sleep(300)
                    android.os.ParcelFileDescriptor.AutoCloseInputStream(ins.uiAutomation.executeShellCommand("screencap -p /data/local/tmp/player-$name.png")).use { it.readBytes() }
                }
                render("widget-playing")
                RecentFiles.setPrivate(c, true)
                scenario.onActivity { activity ->
                    val view = PlayerWidgetProvider.buildViews(activity).apply(activity, null)
                    assertEquals("Private playback", view.findViewById<android.widget.TextView>(R.id.pw_title).text.toString())
                    assertEquals("File history is hidden", view.findViewById<android.widget.TextView>(R.id.pw_detail).text.toString())
                    assertFalse(view.findViewById<android.widget.Button>(R.id.pw_next).isEnabled)
                }
                // Force a distinct composition so the RemoteViews private state is applied.
                scenario.onActivity { it.setContent { androidx.compose.material3.Text("Rendering private widget") } }
                render("widget-private")
            }
        } finally {
            RecentFiles.setPrivate(c, false)
            ins.runOnMainSync { controller?.stop(); controller?.clearMediaItems(); controller?.release() }
            wav.delete()
        }
    }
}
