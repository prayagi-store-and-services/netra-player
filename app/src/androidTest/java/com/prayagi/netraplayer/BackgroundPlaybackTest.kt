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
    private fun awaitNode(label: String, scroll: Boolean = false, predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo {
        val until = System.currentTimeMillis() + 10000
        while (System.currentTimeMillis() < until) {
            find(instrumentation.uiAutomation.rootInActiveWindow, predicate)?.let { return it }
            if (scroll) scrollPage(true)
            Thread.sleep(200)
        }
        capture("failure")
        fun dump(node: AccessibilityNodeInfo?, depth: Int = 0): String {
            if (node == null) return ""
            return " ".repeat(depth) + "${node.className} text=${node.text} desc=${node.contentDescription}\n" +
                (0 until node.childCount).joinToString("") { dump(node.getChild(it), depth + 1) }
        }
        File(instrumentation.targetContext.getExternalFilesDir(null), "failure-ui.txt").writeText(dump(instrumentation.uiAutomation.rootInActiveWindow))
        throw AssertionError("Missing $label")
    }
    private fun capture(name: String) {
        try { instrumentation.uiAutomation.waitForIdle(250, 2500) } catch (_: java.util.concurrent.TimeoutException) {}
        shell("screencap -p /data/local/tmp/player-$name.png")
    }
    private fun scrollPage(forward: Boolean) {
        fun scrollable(n: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
            if (n == null) return null
            if (n.isScrollable) return n
            for (i in 0 until n.childCount) scrollable(n.getChild(i))?.let { return it }
            return null
        }
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val action = if (forward) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        scrollable(automation.rootInActiveWindow)?.performAction(action)
        Thread.sleep(400)
    }
    @Test fun localAudioContinuesWithActivityStopped() {
        val context = instrumentation.targetContext
        val wav = File(context.filesDir, "device-test.wav")
        val sampleRate = 8000
        val dataSize = sampleRate * 2 * 120
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
                controller!!.seekTo(119_500)
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
            // Resume the scenario-owned activity. A shell am start creates another standard-mode instance.
            scenario.onActivity { activity ->
                org.junit.Assert.assertEquals(Lifecycle.State.RESUMED, activity.lifecycle.currentState)
            }
            awaitNode("loaded player", scroll = true) { it.text?.contains("Open another file") == true }
            capture("loaded")
            shell("cmd uimode night yes")
            awaitNode("dark player", scroll = true) { it.text?.contains("Open another file") == true }
            Thread.sleep(800)
            capture("dark")
            // Establish visible controls before testing the user's fullscreen/hide/show actions.
            scenario.onActivity { activity ->
                fun findPlayer(view: android.view.View): androidx.media3.ui.PlayerView? {
                    if (view is androidx.media3.ui.PlayerView) return view
                    if (view is android.view.ViewGroup) for (i in 0 until view.childCount) findPlayer(view.getChildAt(i))?.let { return it }
                    return null
                }
                val view = findPlayer(activity.window.decorView) ?: throw AssertionError("PlayerView missing from view hierarchy")
                org.junit.Assert.assertTrue("Scenario activity owns visible PlayerView", view.isShown)
                view.showController()
            }
            // showController animates; isControllerFullyVisible is false until the animation ends.
            var controllerReady = false
            val controllerUntil = System.currentTimeMillis() + 3000
            while (!controllerReady && System.currentTimeMillis() < controllerUntil) {
                instrumentation.waitForIdleSync()
                scenario.onActivity { activity ->
                    fun findPlayer(view: android.view.View): androidx.media3.ui.PlayerView? {
                        if (view is androidx.media3.ui.PlayerView) return view
                        if (view is android.view.ViewGroup) for (i in 0 until view.childCount) findPlayer(view.getChildAt(i))?.let { return it }
                        return null
                    }
                    controllerReady = findPlayer(activity.window.decorView)?.isControllerFullyVisible == true
                }
                if (!controllerReady) Thread.sleep(50)
            }
            org.junit.Assert.assertTrue("Controller became fully visible after animation", controllerReady)
            val fullscreen = awaitNode("Fullscreen control") { it.contentDescription?.toString()?.contains("fullscreen", true) == true }
            assertTrue("Fullscreen is clickable", fullscreen.performAction(AccessibilityNodeInfo.ACTION_CLICK))
            // First-use immersive tutorial is an OS window, not the player controller.
            val tutorialUntil = System.currentTimeMillis() + 5000
            while (System.currentTimeMillis() < tutorialUntil) {
                var tutorial = find(instrumentation.uiAutomation.rootInActiveWindow) { it.text?.toString() == "Got it" }
                if (tutorial != null) {
                    while (!tutorial!!.isClickable && tutorial.parent != null) tutorial = tutorial.parent
                    tutorial!!.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    break
                }
                Thread.sleep(200)
            }
            // A tutorial is a separate system window. An ACTION_CLICK return alone
            // does not prove dismissal, so verify absence before touching the player.
            val tutorialGoneUntil = System.currentTimeMillis() + 3000
            while (find(instrumentation.uiAutomation.rootInActiveWindow) { it.text?.toString() == "Got it" } != null && System.currentTimeMillis() < tutorialGoneUntil) Thread.sleep(50)
            assertTrue("Immersive tutorial dismissed before player touch", find(instrumentation.uiAutomation.rootInActiveWindow) { it.text?.toString() == "Got it" } == null)
            // Accessibility removal can precede WindowManager's input-window transaction.
            // A stale ImmersiveModeConfirmation input handle must not receive the real tap.
            instrumentation.uiAutomation.syncInputTransactions()
            val inputReadyUntil = System.currentTimeMillis() + 5000
            var inputWindowReady = false
            while (System.currentTimeMillis() < inputReadyUntil) {
                instrumentation.uiAutomation.syncInputTransactions()
                val windows = shell("dumpsys window windows")
                if (!windows.contains("ImmersiveModeConfirmation")) {
                    inputWindowReady = true
                    break
                }
                Thread.sleep(50)
            }
            assertTrue("Immersive input window removed before real player touch", inputWindowReady)
            val touches = java.util.concurrent.atomic.AtomicInteger(0)
            var inputStates = ""
            var physicalTapIssued = false
            var diagnosticPlayer: androidx.media3.ui.PlayerView? = null
            scenario.onActivity { activity ->
                fun findPlayer(view: android.view.View): androidx.media3.ui.PlayerView? {
                    if (view is androidx.media3.ui.PlayerView) return view
                    if (view is android.view.ViewGroup) for (i in 0 until view.childCount) findPlayer(view.getChildAt(i))?.let { return it }
                    return null
                }
                diagnosticPlayer = findPlayer(activity.window.decorView)
                diagnosticPlayer?.setOnTouchListener { _, event ->
                    if (event.actionMasked == android.view.MotionEvent.ACTION_UP) touches.incrementAndGet()
                    inputStates += " action=${event.actionMasked} controller=${diagnosticPlayer?.isControllerFullyVisible} time=${event.eventTime}"
                    false
                }
            }
            fun controllerVisible(): Boolean {
                var visible = false
                scenario.onActivity { activity ->
                    fun findPlayer(view: android.view.View): androidx.media3.ui.PlayerView? {
                        if (view is androidx.media3.ui.PlayerView) return view
                        if (view is android.view.ViewGroup) for (i in 0 until view.childCount) findPlayer(view.getChildAt(i))?.let { return it }
                        return null
                    }
                    visible = findPlayer(activity.window.decorView)?.isControllerFullyVisible == true
                }
                return visible
            }
            fun awaitController(visible: Boolean) {
                val until = System.currentTimeMillis() + 1500
                while (controllerVisible() != visible && System.currentTimeMillis() < until) Thread.sleep(50)
                var diagnostic = ""
                if (controllerVisible() != visible) {
                    shell("screencap -p /data/local/tmp/player-failure.png")
                    scenario.onActivity { activity ->
                        fun dump(view: android.view.View, depth: Int = 0): String {
                            val bounds = android.graphics.Rect()
                            view.getGlobalVisibleRect(bounds)
                            val state = if (view is androidx.media3.ui.PlayerView) " viewIdentity=${System.identityHashCode(view)} retainedIdentity=${diagnosticPlayer?.let { System.identityHashCode(it) }} nativeControlVisibility=${view.findViewById<android.view.View>(androidx.media3.ui.R.id.exo_controller)?.visibility} controllerFullyVisible=${view.isControllerFullyVisible} useController=${view.useController} playerBound=${view.player != null} hasWindowFocus=${view.hasWindowFocus()}" else ""
                            return " ".repeat(depth) + "${view.javaClass.name} shown=${view.isShown} visibility=${view.visibility} clickable=${view.isClickable} bounds=$bounds$state\n" +
                                if (view is android.view.ViewGroup) (0 until view.childCount).joinToString("") { dump(view.getChildAt(it), depth + 1) } else ""
                        }
                        diagnostic = "overlayTracksPresent=${find(instrumentation.uiAutomation.rootInActiveWindow) { it.text?.toString() == "Tracks" } != null} activeWindowId=${instrumentation.uiAutomation.rootInActiveWindow?.windowId} physicalTapIssued=$physicalTapIssued\n" + "lifecycle=${activity.lifecycle.currentState} activityFocus=${activity.hasWindowFocus()} fullscreen=${playerFullscreen.value}\n" + dump(activity.window.decorView)
                    }
                }
                val originalState = controllerVisible()
                if (originalState != visible) {
                    scenario.onActivity { diagnosticPlayer?.performClick() }
                    Thread.sleep(500)
                    diagnostic += "\nphysicalTapIssued=$physicalTapIssued realTouchUps=${touches.get()} inputStates=$inputStates retainedViewShown=${diagnosticPlayer?.isShown} directClickVisible=${controllerVisible()}"
                }
                assertTrue("Controller fully-visible state equals $visible\n$diagnostic", originalState == visible)
            }
            // Establish a hidden starting state without racing the 4-second auto-hide
            // boundary. The behavior under test still MUST pass via real touch.
            scenario.onActivity { diagnosticPlayer?.hideController() }
            awaitController(false)
            physicalTapIssued = true
            instrumentation.uiAutomation.syncInputTransactions()
            shell("input tap 650 550")
            awaitController(true)
            awaitNode("Tracks visible in fullscreen") { it.text?.toString() == "Tracks" }
            // Capture immediately after the settled state. Idle-wait capture can consume the auto-hide window.
            shell("screencap -p /data/local/tmp/player-fullscreen.png")
            awaitController(true)
            instrumentation.uiAutomation.syncInputTransactions()
            shell("input tap 650 550")
            awaitController(false)
            val hiddenUntil = System.currentTimeMillis() + 1500
            while (find(instrumentation.uiAutomation.rootInActiveWindow) { it.text?.toString() == "Tracks" } != null && System.currentTimeMillis() < hiddenUntil) Thread.sleep(50)
            shell("screencap -p /data/local/tmp/player-fullscreen-hidden.png")
            assertTrue("Tracks overlay hides with controller", find(instrumentation.uiAutomation.rootInActiveWindow) { it.text?.toString() == "Tracks" } == null)
            instrumentation.uiAutomation.syncInputTransactions()
            shell("input tap 650 550")
            awaitController(true)
            awaitNode("Tracks visible after tap") { it.text?.toString() == "Tracks" }
            shell("screencap -p /data/local/tmp/player-fullscreen-shown.png")
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
