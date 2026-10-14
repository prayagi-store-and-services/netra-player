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
        val state = InstrumentationRegistry.getArguments().getString("libraryState")
        if (state == null) return // Host runs explicit state checks after the unchanged full suite.
        val access = LocalMediaLibrary.access(ins.targetContext)
        if (state == "partial") {
            assertFalse(access.video); assertFalse(access.audio); assertTrue(access.selectedVideo)
        } else {
            assertFalse(access.any)
            assertTrue(LocalMediaLibrary.scan(ins.targetContext, access).isEmpty())
        }
        fun has(n: android.view.accessibility.AccessibilityNodeInfo?, text: String): Boolean {
            if (n == null) return false
            if (n.text?.toString()?.contains(text) == true) return true
            return (0 until n.childCount).any { has(n.getChild(it), text) }
        }
        androidx.test.core.app.ActivityScenario.launch(MainActivity::class.java).use {
            val expected = if (state == "partial") "Selected videos only" else "Media access is off"
            val end = System.currentTimeMillis() + 10000
            while (!has(ins.uiAutomation.rootInActiveWindow, expected) && System.currentTimeMillis() < end) {
                android.os.ParcelFileDescriptor.AutoCloseInputStream(ins.uiAutomation.executeShellCommand("input swipe 20 1200 20 500 300")).use { it.readBytes() }
                Thread.sleep(250)
            }
            assertTrue("Permission state disclosed: $state", has(ins.uiAutomation.rootInActiveWindow, expected))
            android.os.ParcelFileDescriptor.AutoCloseInputStream(ins.uiAutomation.executeShellCommand("screencap -p /data/local/tmp/player-library-$state.png")).use { it.readBytes() }
        }
    }
}
