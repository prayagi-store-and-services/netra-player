package com.prayagi.netraplayer

import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.media3.common.*
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.collect.ImmutableList
import org.junit.Assert.*
import org.junit.Test
import org.junit.Before
import org.junit.runner.RunWith
import java.io.File

/** Targeted discriminator only. Does not replace the original regression test. */
@RunWith(AndroidJUnit4::class)
class PlayerSubtitleDiagnosticTest {
    private val ins get() = InstrumentationRegistry.getInstrumentation()
    @Before fun grantExpectedNotificationPermissionOnEmulator() {
        // MainActivity explicitly requests this runtime permission on launch.
        // Scope is the disposable test app, never a user's installed phone.
        android.os.ParcelFileDescriptor.AutoCloseInputStream(ins.uiAutomation.executeShellCommand(
            "pm grant com.prayagi.netraplayer android.permission.POST_NOTIFICATIONS"
        )).use { it.readBytes() }
        assertEquals(android.content.pm.PackageManager.PERMISSION_GRANTED,
            ins.targetContext.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS))
    }
    private fun textGroup() = TrackGroup("text",Format.Builder().setSampleMimeType("application/x-subrip").setLanguage("en").build())
    private fun tracks(group:TrackGroup) = Tracks(ImmutableList.of(Tracks.Group(group,false,intArrayOf(C.FORMAT_HANDLED),booleanArrayOf(false))))
    private fun record(label:String,value:Any?) {
        val line="SUBTITLE_DIAG $label: $value\n"
        android.util.Log.i("SubtitleDiagnostic",line)
        val dir=File(ins.targetContext.getExternalFilesDir(null),"subtitle-diagnostics").apply { mkdirs() }
        File(dir,"trace.txt").appendText(line)
    }
    private fun find(node:AccessibilityNodeInfo?,label:String):AccessibilityNodeInfo? {
        if(node==null)return null
        if(node.isVisibleToUser && node.text?.toString()==label)return node
        for(i in 0 until node.childCount)find(node.getChild(i),label)?.let{return it}
        return null
    }
    private fun snapshot(label:String) {
        fun walk(node:AccessibilityNodeInfo?,depth:Int=0) {
            if(node==null)return
            record(label,"${" ".repeat(depth)}text=${node.text} checked=${node.isChecked} selected=${node.isSelected} clickable=${node.isClickable} visible=${node.isVisibleToUser}")
            for(i in 0 until node.childCount)walk(node.getChild(i),depth+1)
        }
        walk(ins.uiAutomation.rootInActiveWindow)
    }
    private fun click(label:String) {
        ins.waitForIdleSync()
        ins.uiAutomation.waitForIdle(300,5000)
        val deadline=System.currentTimeMillis()+10000
        var node=find(ins.uiAutomation.rootInActiveWindow,label)
        while(node==null && System.currentTimeMillis()<deadline) {
            Thread.sleep(100)
            node=find(ins.uiAutomation.rootInActiveWindow,label)
        }
        if(node==null) {
            snapshot("MISSING $label")
            val file=File(ins.targetContext.getExternalFilesDir(null),"subtitle-diagnostics/missing-control.png")
            file.parentFile?.mkdirs()
            ins.uiAutomation.takeScreenshot()?.let { image ->
                file.outputStream().use { image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }
            }
        }
        assertNotNull("Diagnostic label missing: $label",node)
        while(node!=null && !node.isClickable)node=node.parent
        assertTrue("Diagnostic click not accepted: $label",node?.performAction(AccessibilityNodeInfo.ACTION_CLICK)==true)
        ins.waitForIdleSync()
        ins.uiAutomation.waitForIdle(300,5000)
        snapshot("after $label")
    }
    @Test fun directSyntheticOverrideRoundTrip() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val p=ExoPlayer.Builder(activity).build()
                try {
                    val group=textGroup()
                    val intended=p.trackSelectionParameters.buildUpon().setTrackTypeDisabled(C.TRACK_TYPE_TEXT,false)
                        .setOverrideForType(TrackSelectionOverride(group,0)).build()
                    record("direct intended",intended.overrides)
                    p.trackSelectionParameters=intended
                    record("direct actual",p.trackSelectionParameters.overrides)
                    assertEquals("Direct setter must preserve this exact synthetic override",listOf(0),p.trackSelectionParameters.overrides[group]?.trackIndices)
                } finally { p.release() }
            }
        }
    }
    @Test fun freshDialogApplyWithoutPriorCancel() { dialogPath(false) }
    @Test fun actualDismissAndReopenThenApply() { dialogPath(true) }
    private fun dialogPath(cancelFirst:Boolean) {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            lateinit var p:ExoPlayer
            lateinit var reopen:()->Unit
            var closed=false
            val group=textGroup()
            val available=tracks(group)
            scenario.onActivity { activity ->
                p=ExoPlayer.Builder(activity).build()
                activity.setContent {
                    var visible by remember { mutableStateOf(true) }
                    reopen={ visible=true }
                    NetraTheme { if(visible)TrackChoiceDialog(p,available) { visible=false;closed=true } }
                }
            }
            try {
                if(cancelFirst) {
                    click("en")
                    scenario.onActivity { assertTrue("Pending choice must not mutate player",p.trackSelectionParameters.overrides.isEmpty()) }
                    click("Subtitles off")
                    click("Cancel")
                    scenario.onActivity {
                        assertTrue(closed)
                        assertTrue("Cancel preserves original overrides",p.trackSelectionParameters.overrides.isEmpty())
                        assertFalse("Cancel preserves subtitle type",C.TRACK_TYPE_TEXT in p.trackSelectionParameters.disabledTrackTypes)
                    }
                    ins.waitForIdleSync()
                    assertNull("Actual close must remove dialog before reopen",find(ins.uiAutomation.rootInActiveWindow,"Audio and subtitles"))
                    scenario.onActivity { closed=false;reopen() }
                }
                click("en")
                scenario.onActivity { record("before apply actual",p.trackSelectionParameters.overrides);assertTrue(p.trackSelectionParameters.overrides.isEmpty()) }
                click("Use selected tracks")
                scenario.onActivity {
                    record("after apply actual",p.trackSelectionParameters.overrides)
                    assertTrue("Apply closes dialog",closed)
                    assertEquals("Explicit subtitle override applied",listOf(0),p.trackSelectionParameters.overrides[group]?.trackIndices)
                    assertFalse(C.TRACK_TYPE_TEXT in p.trackSelectionParameters.disabledTrackTypes)
                }
            } finally { scenario.onActivity { p.release() } }
        }
    }
}
