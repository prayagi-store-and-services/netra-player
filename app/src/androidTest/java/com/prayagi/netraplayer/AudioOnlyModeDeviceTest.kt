package com.prayagi.netraplayer

import android.content.ComponentName
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import androidx.lifecycle.Lifecycle
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.ui.PlayerView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AudioOnlyModeDeviceTest {
    @Test fun togglePersistsAcrossActivityRecreationAndBackgroundQueueTransition() {
        val ins = InstrumentationRegistry.getInstrumentation()
        val context = ins.targetContext
        val fixture = File(context.filesDir, "audio-mode-fixture.mp4")
        ins.context.assets.open("audio-mode-fixture.mp4").use { input ->
            fixture.outputStream().use { input.copyTo(it) }
        }
        val uri = fixture.toURI().toString()
        val otherId = "$uri#second"
        context.getSharedPreferences("audio_only_mode", android.content.Context.MODE_PRIVATE).edit().clear().commit()
        var controller: MediaController? = null
        val connected = CountDownLatch(1)
        fun awaitState(label: String, check: () -> Boolean) {
            val until = System.currentTimeMillis() + 15000
            var ready = false
            while (!ready && System.currentTimeMillis() < until) {
                ins.runOnMainSync { ready = check() }
                if (!ready) Thread.sleep(50)
            }
            assertTrue(label, ready)
        }
        fun findPlayer(view: View): PlayerView? {
            if (view is PlayerView) return view
            if (view is ViewGroup) for (i in 0 until view.childCount) findPlayer(view.getChildAt(i))?.let { return it }
            return null
        }
        fun findLabel(node: AccessibilityNodeInfo?, label: String): AccessibilityNodeInfo? {
            if (node == null) return null
            if (node.isVisibleToUser && node.text?.toString() == label) return node
            for (i in 0 until node.childCount) findLabel(node.getChild(i), label)?.let { return it }
            return null
        }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            try {
                ins.runOnMainSync {
                    val future = MediaController.Builder(context,
                        SessionToken(context, ComponentName(context, PlaybackService::class.java))).buildAsync()
                    future.addListener({ controller = future.get(); connected.countDown() },
                        androidx.core.content.ContextCompat.getMainExecutor(context))
                }
                assertTrue("Session connected", connected.await(15, TimeUnit.SECONDS))
                ins.runOnMainSync {
                    controller!!.setMediaItems(listOf(uri, otherId).map { id ->
                        MediaItem.Builder().setUri(uri).setMediaId(id)
                            .setMediaMetadata(MediaMetadata.Builder().setTitle("Audio-mode fixture").build()).build()
                    })
                    controller!!.prepare()
                    controller!!.play()
                }
                awaitState("Fixture has video and audio playback started") {
                    controller!!.isPlaying && AudioOnlyMode.hasVideoTrack(controller!!.currentTracks)
                }
                scenario.onActivity { findPlayer(it.window.decorView)?.showController() }
                val until = System.currentTimeMillis() + 10000
                var toggle: AccessibilityNodeInfo? = null
                while (toggle == null && System.currentTimeMillis() < until) {
                    toggle = findLabel(ins.uiAutomation.rootInActiveWindow, "Listen as Audio")
                    if (toggle == null) Thread.sleep(50)
                }
                assertTrue("Listen as Audio button visible", toggle != null)
                while (toggle != null && !toggle!!.isClickable) toggle = toggle!!.parent
                assertTrue("Audio-only button clicked", toggle?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true)
                awaitState("Toggle disables video without stopping audio") {
                    controller!!.isPlaying && C.TRACK_TYPE_VIDEO in controller!!.trackSelectionParameters.disabledTrackTypes &&
                        AudioOnlyMode.isAudioOnly(context, uri)
                }
                assertTrue("Play Video remains available", run {
                    val deadline = System.currentTimeMillis() + 5000
                    while (findLabel(ins.uiAutomation.rootInActiveWindow, "Play Video") == null && System.currentTimeMillis() < deadline) Thread.sleep(50)
                    findLabel(ins.uiAutomation.rootInActiveWindow, "Play Video") != null
                })
                ins.uiAutomation.waitForIdle(250, 3000)
                android.os.ParcelFileDescriptor.AutoCloseInputStream(ins.uiAutomation.executeShellCommand(
                    "screencap -p /data/local/tmp/player-audio-only.png")).use { it.readBytes() }
                scenario.recreate()
                awaitState("Audio-only survives activity recreation") {
                    controller!!.isPlaying && C.TRACK_TYPE_VIDEO in controller!!.trackSelectionParameters.disabledTrackTypes
                }
                scenario.moveToState(Lifecycle.State.CREATED)
                ins.runOnMainSync { controller!!.seekToNextMediaItem() }
                awaitState("Background queue transition keeps video disabled") {
                    controller!!.currentMediaItem?.mediaId == otherId &&
                        C.TRACK_TYPE_VIDEO in controller!!.trackSelectionParameters.disabledTrackTypes
                }
                scenario.moveToState(Lifecycle.State.RESUMED)
                awaitState("Foreground restores video only for non-audio-only item") {
                    controller!!.currentMediaItem?.mediaId == otherId &&
                        C.TRACK_TYPE_VIDEO !in controller!!.trackSelectionParameters.disabledTrackTypes
                }
                ins.runOnMainSync { controller!!.seekToPreviousMediaItem() }
                awaitState("Returning to first item restores its saved audio-only choice") {
                    controller!!.currentMediaItem?.mediaId == uri &&
                        C.TRACK_TYPE_VIDEO in controller!!.trackSelectionParameters.disabledTrackTypes
                }
            } finally {
                ins.runOnMainSync { controller?.stop(); controller?.release() }
                context.getSharedPreferences("audio_only_mode", android.content.Context.MODE_PRIVATE).edit().clear().commit()
            }
        }
    }
}
