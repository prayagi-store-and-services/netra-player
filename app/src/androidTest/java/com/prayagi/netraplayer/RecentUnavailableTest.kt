package com.prayagi.netraplayer

import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class RecentUnavailableTest {
    @Test fun missingLocalFileShowsRecoveryAndCanBeRemoved() {
        val ins = InstrumentationRegistry.getInstrumentation()
        val c = ins.targetContext
        c.stopService(android.content.Intent(c, PlaybackService::class.java))
        Thread.sleep(400)
        RecentFiles.start(c)
        RecentFiles.setPrivate(c, false)
        RecentFiles.clear(c, null)
        fun await(check: () -> Boolean) {
            val end = System.currentTimeMillis() + 8000
            while (!check() && System.currentTimeMillis() < end) Thread.sleep(100)
            assertTrue(check())
        }
        await { RecentFiles.items.value.isEmpty() }
        val gone = File(c.filesDir, "removed-recent-fixture.mp3")
        gone.delete()
        RecentFiles.selected(c)
        RecentFiles.save(c, android.net.Uri.fromFile(gone).toString(), "Removed recent fixture", 7000)
        await { RecentFiles.items.value.size == 1 }
        fun find(n: AccessibilityNodeInfo?, text: String): AccessibilityNodeInfo? {
            if (n == null) return null
            if (n.text?.contains(text) == true) return n
            for (i in 0 until n.childCount) find(n.getChild(i), text)?.let { return it }
            return null
        }
        fun scroll(n: AccessibilityNodeInfo?): Boolean {
            if (n == null) return false
            if (n.isScrollable) return n.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
            for (i in 0 until n.childCount) if (scroll(n.getChild(i))) return true
            return false
        }
        fun node(text: String): AccessibilityNodeInfo {
            val end = System.currentTimeMillis() + 10000
            while (System.currentTimeMillis() < end) {
                find(ins.uiAutomation.rootInActiveWindow, text)?.let { return it }
                scroll(ins.uiAutomation.rootInActiveWindow)
                Thread.sleep(200)
            }
            throw AssertionError("Missing UI text: $text")
        }
        fun click(n: AccessibilityNodeInfo) {
            var target: AccessibilityNodeInfo? = n
            while (target != null && !target.isClickable) target = target.parent
            assertTrue("Recent file action accepted", target?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true)
        }
        try {
            ActivityScenario.launch(MainActivity::class.java).use {
                click(node("Removed recent fixture at"))
                node("This file is unavailable. Pick it again or remove it from recent files.")
                assertEquals(1, RecentFiles.items.value.size)
                android.os.ParcelFileDescriptor.AutoCloseInputStream(ins.uiAutomation.executeShellCommand("screencap -p /data/local/tmp/player-recent-unavailable.png")).use { it.readBytes() }
                click(node("Remove"))
                await { RecentFiles.items.value.isEmpty() }
            }
        } finally {
            RecentFiles.clear(c, null)
        }
    }
}
