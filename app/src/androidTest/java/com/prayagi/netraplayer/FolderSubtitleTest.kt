package com.prayagi.netraplayer

import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FolderSubtitleTest {
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
            Thread.sleep(200)
        }
        shell("screencap -p /data/local/tmp/player-folder-failure.png")
        shell("uiautomator dump /data/local/tmp/player-folder-failure.xml")
        throw AssertionError("Missing $text")
    }
    private fun click(text: String) {
        var n: AccessibilityNodeInfo? = node(text)
        while(n != null && !n.isClickable) n = n.parent
        assertTrue("Clickable $text", n?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true)
    }
    private fun capture(name: String) { Thread.sleep(700); shell("screencap -p /data/local/tmp/player-$name.png") }
    @Test fun chosenFolderQueueAndLocalSubtitle() {
        shell("cmd statusbar collapse")
        ActivityScenario.launch(MainActivity::class.java).use {
            click("Open folder")
            click("Show roots")
            click("Downloads")
            click("PlayerFixture")
            click("Use this folder")
            click("Allow")
            node("Folder files: 2")
            click("a-video.mp4")
            node("Queue: 1 of 2")
            capture("folder")
            click("Next")
            node("Queue: 2 of 2")
            click("Previous")
            node("Queue: 1 of 2")
            click("Open .srt subtitles")
            click("Show roots")
            click("Downloads")
            click("PlayerFixture")
            click("caption.srt")
            node("LOCAL SUBTITLE CHECK")
            capture("subtitle")
        }
    }
}
