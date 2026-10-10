package com.prayagi.netraplayer

import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FolderQueueTest {
    private val ins get() = InstrumentationRegistry.getInstrumentation()
    private fun shell(cmd: String) = android.os.ParcelFileDescriptor.AutoCloseInputStream(ins.uiAutomation.executeShellCommand(cmd)).use { it.readBytes() }
    private fun find(n: AccessibilityNodeInfo?, text: String): AccessibilityNodeInfo? {
        if (n == null) return null
        if (n.text?.toString()?.equals(text, true) == true || n.contentDescription?.toString()?.equals(text, true) == true) return n
        for(i in 0 until n.childCount) find(n.getChild(i), text)?.let { return it }
        return null
    }
    private fun node(text: String): AccessibilityNodeInfo {
        val end = System.currentTimeMillis() + 10000
        while (System.currentTimeMillis() < end) {
            find(ins.uiAutomation.rootInActiveWindow, text)?.let { return it }
            if (ins.uiAutomation.rootInActiveWindow?.packageName?.toString() == ins.targetContext.packageName &&
                text !in listOf("Open folder", "Download", "PlayerFixture", "Use this folder", "Allow")) {
                fun scroll(n: AccessibilityNodeInfo?): Boolean {
                    if (n == null) return false
                    if (n.isScrollable && n.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)) return true
                    for (i in 0 until n.childCount) if (scroll(n.getChild(i))) return true
                    return false
                }
                scroll(ins.uiAutomation.rootInActiveWindow)
            }
            Thread.sleep(300)
        }
        shell("screencap -p /data/local/tmp/player-folder-failure.png")
        shell("uiautomator dump /data/local/tmp/player-folder-failure.xml")
        throw AssertionError("Missing $text")
    }
    private fun click(text: String) {
        var n: AccessibilityNodeInfo? = node(text)
        while(n != null && !n.isClickable) n = n.parent
        var clicked = n?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true
        if (!clicked) { Thread.sleep(400); n = node(text); while(n != null && !n.isClickable) n = n.parent
            clicked = n?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true }
        assertTrue("Clickable $text", clicked)
    }
    private fun capture(name: String) { Thread.sleep(700); shell("screencap -p /data/local/tmp/player-$name.png") }
    @Test fun chosenFolderQueue() {
        shell("cmd statusbar collapse")
        // Each test starts with an empty service queue, even after a preceding failure.
        var reset: androidx.media3.session.MediaController? = null
        val ready = java.util.concurrent.CountDownLatch(1)
        ins.runOnMainSync {
            val f = androidx.media3.session.MediaController.Builder(ins.targetContext,
                androidx.media3.session.SessionToken(ins.targetContext,
                    android.content.ComponentName(ins.targetContext, PlaybackService::class.java))).buildAsync()
            f.addListener({ reset = f.get(); ready.countDown() },
                androidx.core.content.ContextCompat.getMainExecutor(ins.targetContext))
        }
        assertTrue("Reset controller connected", ready.await(10, java.util.concurrent.TimeUnit.SECONDS))
        ins.runOnMainSync { reset!!.stop(); reset!!.clearMediaItems(); reset!!.release(); playerFullscreen.value = false }
        ActivityScenario.launch(MainActivity::class.java).use {
            click("Open folder")
            val pickerReady = System.currentTimeMillis() + 10000
            while (System.currentTimeMillis() < pickerReady &&
                ins.uiAutomation.rootInActiveWindow?.packageName?.toString() == ins.targetContext.packageName) Thread.sleep(200)
            click("Download")
            click("PlayerFixture")
            click("Use this folder")
            click("Allow")
            shell("input swipe 20 1600 20 550 450")
            node("Folder files: 2")
            click("a-video.mp4")
            repeat(5) {
                fun top(n: AccessibilityNodeInfo?): Boolean {
                    if (n == null) return false
                    if (n.isScrollable && n.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)) return true
                    for (i in 0 until n.childCount) if (top(n.getChild(i))) return true
                    return false
                }
                top(ins.uiAutomation.rootInActiveWindow); Thread.sleep(200)
            }
            node("Queue: 1 of 2")
            capture("folder")
            click("Next")
            node("Queue: 2 of 2")
            click("Previous")
            node("Queue: 1 of 2")

        }
    }
}
