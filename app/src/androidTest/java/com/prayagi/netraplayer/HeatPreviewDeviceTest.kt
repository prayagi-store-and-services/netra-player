package com.prayagi.netraplayer

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.lifecycle.Lifecycle
import android.view.accessibility.AccessibilityNodeInfo
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HeatPreviewDeviceTest {
    private val ins get() = InstrumentationRegistry.getInstrumentation()
    private fun nodes(n: AccessibilityNodeInfo?): List<AccessibilityNodeInfo> {
        if (n == null) return emptyList()
        return listOf(n) + (0 until n.childCount).flatMap { nodes(n.getChild(it)) }
    }
    private fun all() = nodes(ins.uiAutomation.rootInActiveWindow)
    private fun label(s: String) = all().firstOrNull { it.text?.toString() == s }
    private fun tapLabel(text: String) {
        val deadline = System.currentTimeMillis() + 10000
        while (System.currentTimeMillis() < deadline) {
            var target = label(text)
            target?.performAction(AccessibilityNodeInfo.ACTION_SHOW_ON_SCREEN)
            while (target != null && !target.isClickable) target = target.parent
            if (target != null && target.isEnabled && target.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                ins.waitForIdleSync(); return
            }
            Thread.sleep(100)
        }
        shot("failure")
        fail("Enabled clickable control: $text")
    }
    private fun enablePreview() {
        val deadline = System.currentTimeMillis() + 10000
        while (System.currentTimeMillis() < deadline) {
            val toggle = all().firstOrNull { it.isCheckable }
            if (toggle?.isChecked == true) return
            toggle?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            Thread.sleep(200)
        }
        fail("Preview toggle did not become checked")
    }
    private fun shot(name: String) {
        android.os.ParcelFileDescriptor.AutoCloseInputStream(ins.uiAutomation.executeShellCommand("screencap -p /data/local/tmp/player-heat-$name.png")).use { it.readBytes() }
    }
    private fun await(label: String) {
        val deadline = System.currentTimeMillis() + 12000
        while (System.currentTimeMillis() < deadline) {
            if (all().any { it.text?.toString()?.startsWith(label) == true }) return
            Thread.sleep(200)
        }
        fail("Missing $label")
    }
    @Test fun realSampleOffClearAndBackgroundCancellation() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            // Production card and Android sensor adapter, isolated only for deterministic scrolling.
            scenario.onActivity { a -> a.setContent { NetraTheme { Column(Modifier.verticalScroll(rememberScrollState())) { HeatPreviewCard() } } } }
            await("Heating diagnostics: local preview")
            val off = label("Take one local sample")!!
            var button: AccessibilityNodeInfo? = off
            while (button != null && !button.isClickable) button = button.parent
            assertFalse("Default OFF", button!!.isEnabled)
            shot("off")
            enablePreview()
            tapLabel("Take one local sample")
            await("This app:")
            assertNotNull(label("Clear local sample")); shot("sample")
            tapLabel("Clear local sample")
            assertFalse(all().any { it.text?.toString()?.startsWith("This app:") == true })
            tapLabel("Take one local sample")
            scenario.moveToState(Lifecycle.State.CREATED)
            Thread.sleep(5500)
            scenario.moveToState(Lifecycle.State.RESUMED)
            ins.waitForIdleSync()
            assertFalse("No result after background", all().any { it.text?.toString()?.startsWith("This app:") == true })
            assertFalse("OFF after pause", all().first { it.isCheckable }.isChecked)
            shot("cancelled")
        }
    }
}
