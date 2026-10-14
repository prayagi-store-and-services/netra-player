package com.prayagi.netraplayer

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LibraryPermissionStateTest {
    @Test fun hostSelectedOrRevokedAccessIsHonest() {
        val ins = InstrumentationRegistry.getInstrumentation()
        val state = InstrumentationRegistry.getArguments().getString("libraryState") ?: "full"
        val access = LocalMediaLibrary.access(ins.targetContext)
        if (state == "partial") {
            assertFalse(access.video); assertFalse(access.audio); assertTrue(access.selectedVideo)
        } else if (state == "full") {
            assertFalse("No partial-access claim under full access", access.video && access.selectedVideo)
        } else {
            assertFalse(access.any)
            assertTrue(LocalMediaLibrary.scan(ins.targetContext, access).isEmpty())
        }
        fun has(n: android.view.accessibility.AccessibilityNodeInfo?, text: String): Boolean {
            if (n == null) return false
            if (n.text?.toString()?.contains(text) == true) return true
            return (0 until n.childCount).any { has(n.getChild(it), text) }
        }
        ins.runOnMainSync { playerFullscreen.value = false }
        androidx.test.core.app.ActivityScenario.launch(MainActivity::class.java).use {
            // A fresh host install may show the existing notification permission prompt.
            // Explicitly exercise the deny path rather than silently granting permission.
            fun deny(n: android.view.accessibility.AccessibilityNodeInfo?): android.view.accessibility.AccessibilityNodeInfo? {
                if (n == null) return null
                if (n.text?.toString() in listOf("Don't allow", "Don’t allow")) return n
                for (i in 0 until n.childCount) deny(n.getChild(i))?.let { return it }
                return null
            }
            val promptUntil = System.currentTimeMillis() + 2000
            while (System.currentTimeMillis() < promptUntil) {
                var button = deny(ins.uiAutomation.rootInActiveWindow)
                if (button != null) {
                    while (button != null && !button.isClickable) button = button.parent
                    assertTrue("Notification deny action", button?.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK) == true)
                    ins.waitForIdleSync(); Thread.sleep(400)
                    assertFalse("Notification prompt closes after denial", has(ins.uiAutomation.rootInActiveWindow, "send you notifications"))
                    break
                }
                Thread.sleep(100)
            }
            val expected = if (state == "partial") "Selected videos only" else if (state == "full" && access.any) "Manage media access" else "Media access is off"
            val end = System.currentTimeMillis() + 10000
            while (!has(ins.uiAutomation.rootInActiveWindow, expected) && System.currentTimeMillis() < end) {
                android.os.ParcelFileDescriptor.AutoCloseInputStream(ins.uiAutomation.executeShellCommand("input swipe 20 1200 20 500 300")).use { it.readBytes() }
                Thread.sleep(250)
            }

            android.os.ParcelFileDescriptor.AutoCloseInputStream(ins.uiAutomation.executeShellCommand("screencap -p /data/local/tmp/player-library-$state.png")).use { it.readBytes() }
            assertTrue("Permission state disclosed: $state", has(ins.uiAutomation.rootInActiveWindow, expected))
        }
    }
}
