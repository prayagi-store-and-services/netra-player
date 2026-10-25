package com.prayagi.netraplayer

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.Before
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlayerClockRemovalTest {
    @Before fun prepareExpectedNotificationPermission() {
        val ins=InstrumentationRegistry.getInstrumentation()
        android.os.ParcelFileDescriptor.AutoCloseInputStream(ins.uiAutomation.executeShellCommand(
            "pm grant com.prayagi.netraplayer android.permission.POST_NOTIFICATIONS"
        )).use { it.readBytes() }
        org.junit.Assert.assertEquals(android.content.pm.PackageManager.PERMISSION_GRANTED,
            ins.targetContext.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS))
    }
    @Test fun headerHasNoClockBeforeOrAfterBackgroundReturn() {
        val ins = InstrumentationRegistry.getInstrumentation()
        fun hasClock(node: android.view.accessibility.AccessibilityNodeInfo?): Boolean {
            if (node == null) return false
            if (Regex(".*[0-9]{1,2}:[0-9]{2}:[0-9]{2}(?:\\.[0-9]{3})?.*").matches(node.text?.toString().orEmpty())) return true
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
                assertFalse("Removed live seconds/millisecond clocks must not survive in the UI", hasClock(ins.uiAutomation.rootInActiveWindow))
            }
            val end=System.currentTimeMillis()+10000
            while(!hasHeader(ins.uiAutomation.rootInActiveWindow) && System.currentTimeMillis()<end) Thread.sleep(100)
            assertNoClock()
            android.os.ParcelFileDescriptor.AutoCloseInputStream(ins.uiAutomation.executeShellCommand("screencap -p /data/local/tmp/player-clock-before.png")).use { it.readBytes() }
            scenario.moveToState(Lifecycle.State.CREATED)
            scenario.moveToState(Lifecycle.State.RESUMED)
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            assertNoClock()
            // Leave the actual festival/date content in the capture, not a replacement screen.
            Thread.sleep(1200)
            assertNoClock()
            android.os.ParcelFileDescriptor.AutoCloseInputStream(ins.uiAutomation.executeShellCommand("screencap -p /data/local/tmp/player-clock-removed.png")).use { it.readBytes() }

        }
    }
}
