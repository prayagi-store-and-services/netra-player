package com.prayagi.netraplayer

import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicReference
import android.view.accessibility.AccessibilityNodeInfo

@RunWith(AndroidJUnit4::class)
class PlayerGateVisualTest {
    private val ins get() = InstrumentationRegistry.getInstrumentation()
    private fun shell(cmd: String) = android.os.ParcelFileDescriptor.AutoCloseInputStream(ins.uiAutomation.executeShellCommand(cmd)).use { it.readBytes() }
    private fun find(n: AccessibilityNodeInfo?, label: String): AccessibilityNodeInfo? {
        if(n == null) return null
        if(n.text?.toString() == label) return n
        for(i in 0 until n.childCount) find(n.getChild(i),label)?.let { return it }
        return null
    }
    private fun click(label: String) {
        var n: AccessibilityNodeInfo? = find(ins.uiAutomation.rootInActiveWindow,label)
        while(n != null && !n.isClickable) n=n.parent
        assertTrue("Clickable $label",n?.performAction(AccessibilityNodeInfo.ACTION_CLICK)==true)
        Thread.sleep(1000)
    }
    private fun shot(name: String) { ins.waitForIdleSync(); Thread.sleep(700);shell("screencap -p /data/local/tmp/player-$name.png") }
    @Test fun midnightFlipAndClockRollback() {
        val time=AtomicReference(LocalDateTime.of(2026,10,10,23,59,59))
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { a -> a.setContent { CompositionLocalProvider(LocalPlayerDesignClock provides { time.get() }) { NetraTheme { PlayerScreen() } } } }
            Thread.sleep(1500)
            shot("gate-before")
            assertTrue("No early tagline",find(ins.uiAutomation.rootInActiveWindow,"Play local. Stay private. No login.")==null)
            time.set(LocalDateTime.of(2026,10,11,0,0))
            Thread.sleep(1600)
            assertTrue("Midnight tagline",find(ins.uiAutomation.rootInActiveWindow,"Play local. Stay private. No login.")!=null)
            shot("gate-after")
            click("Update");shot("gate-update")
            click("About");shot("gate-about")
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
            time.set(LocalDateTime.of(2026,10,10,22,0))
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
            Thread.sleep(1600)
            assertTrue("Rollback uses current local date",find(ins.uiAutomation.rootInActiveWindow,"Play local. Stay private. No login.")==null)
            shot("gate-rollback")
        }
    }
}
