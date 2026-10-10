package com.prayagi.netraplayer

import androidx.activity.compose.setContent
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.TrackGroup
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import com.google.common.collect.ImmutableList
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class PlayerVisualTest {
    @Test fun homeAndTrackDialog() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        android.os.ParcelFileDescriptor.AutoCloseInputStream(
            instrumentation.uiAutomation.executeShellCommand("pm grant com.prayagi.netraplayer android.permission.POST_NOTIFICATIONS")
        ).use { it.readBytes() }
        val context = instrumentation.targetContext
        val screenshots = File(context.getExternalFilesDir(null), "screenshots").apply { mkdirs() }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            instrumentation.waitForIdleSync()
            Thread.sleep(1000)
            android.os.ParcelFileDescriptor.AutoCloseInputStream(
                instrumentation.uiAutomation.executeShellCommand("screencap -p /data/local/tmp/player-home.png")
            ).use { it.readBytes() }
            var player: ExoPlayer? = null
            scenario.onActivity { activity ->
                val p = ExoPlayer.Builder(activity).build()
                player = p
                val audio = TrackGroup("audio", Format.Builder().setSampleMimeType("audio/mp4a-latm").setLanguage("en").setLabel("Original").build(),
                    Format.Builder().setSampleMimeType("audio/mp4a-latm").setLanguage("hi").setLabel("Hindi").build())
                val text = TrackGroup("text", Format.Builder().setSampleMimeType("application/x-subrip").setLanguage("en").build())
                val tracks = Tracks(ImmutableList.of(
                    Tracks.Group(audio, false, intArrayOf(C.FORMAT_HANDLED, C.FORMAT_HANDLED), booleanArrayOf(true, false)),
                    Tracks.Group(text, false, intArrayOf(C.FORMAT_HANDLED), booleanArrayOf(false))))
                activity.setContent { NetraTheme { TrackChoiceDialog(p, tracks) {} } }
            }
            instrumentation.waitForIdleSync()
            Thread.sleep(700)
            android.os.ParcelFileDescriptor.AutoCloseInputStream(
                instrumentation.uiAutomation.executeShellCommand("screencap -p /data/local/tmp/player-tracks.png")
            ).use { it.readBytes() }
            scenario.onActivity { player?.release() }
        }
    }
}

@RunWith(AndroidJUnit4::class)
class UpdateEntrySecurityTest {
    @Test fun launcherIgnoresUpdateExtraAndEntryIsPrivate() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val info = context.packageManager.getActivityInfo(android.content.ComponentName(context, UpdateEntryActivity::class.java), 0)
        org.junit.Assert.assertFalse(info.exported)
        val intent = android.content.Intent(context, MainActivity::class.java)
            .putExtra(UpdateAlert.EXTRA_AUTO_UPDATE, true)
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                org.junit.Assert.assertTrue(activity.intent.getBooleanExtra(UpdateAlert.EXTRA_AUTO_UPDATE, false))
            }
        }
        ActivityScenario.launch(UpdateEntryActivity::class.java).use { scenario ->
            scenario.onActivity { activity -> org.junit.Assert.assertTrue(activity is UpdateEntryActivity) }
        }
    }
}
