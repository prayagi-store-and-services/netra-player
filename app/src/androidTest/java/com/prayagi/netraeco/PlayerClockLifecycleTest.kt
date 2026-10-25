package com.prayagi.netraplayer

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlayerClockRemovalTest {
    @Test fun headerHasNoClockBeforeOrAfterBackgroundReturn() {
        val ins = InstrumentationRegistry.getInstrumentation()
        fun hasClock(node: android.view.accessibility.AccessibilityNodeInfo?): Boolean {
            if (node == null) return false
            if (Regex(".*[0-9]{2}:[0-9]{2}:[0-9]{2}\\.[0-9]{3}.*").matches(node.text?.toString().orEmpty())) return true
            return (0 until node.childCount).any { hasClock(node.getChild(it)) }
        }
        fun hasHeader(node: android.view.accessibility.AccessibilityNodeInfo?): Boolean {
            if (node == null) return false
            if (node.text?.toString()?.startsWith("Netra Player  v") == true) return true
            return (0 until node.childCount).any { hasHeader(node.getChild(it)) }
        }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            fun assertNoClock() {
                org.junit.Assert.assertTrue("Actual Player header must be visible, not hidden by an overlay", hasHeader(ins.uiAutomation.rootInActiveWindow))
                assertFalse("Removed millisecond clock must not survive in the UI", hasClock(ins.uiAutomation.rootInActiveWindow))
            }
            assertNoClock()
            scenario.moveToState(Lifecycle.State.CREATED)
            scenario.moveToState(Lifecycle.State.RESUMED)
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            assertNoClock()
            android.os.ParcelFileDescriptor.AutoCloseInputStream(ins.uiAutomation.executeShellCommand("screencap -p /data/local/tmp/player-clock-removed.png")).use { it.readBytes() }

        }
    }
}
