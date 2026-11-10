package com.prayagi.netraplayer

import android.content.Context
import org.robolectric.RuntimeEnvironment
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AudioOnlyPersistenceTest {
    @Test fun savedChoiceIsPerItemAndSurvivesReload() {
        val context: Context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("audio_only_mode", Context.MODE_PRIVATE).edit().clear().commit()
        AudioOnlyMode.setAudioOnly(context, "content://fixture/video-a", true)
        assertTrue(AudioOnlyMode.isAudioOnly(context, "content://fixture/video-a"))
        assertFalse(AudioOnlyMode.isAudioOnly(context, "content://fixture/video-b"))
        assertTrue(AudioOnlyMode.disableVideoTrack(
            AudioOnlyMode.isAudioOnly(context, "content://fixture/video-a"), true))
        // Transition to a different non-audio-only item while backgrounded
        // must still leave video disabled.
        assertTrue(AudioOnlyMode.disableVideoTrack(
            AudioOnlyMode.isAudioOnly(context, "content://fixture/video-b"), false))
        AudioOnlyMode.setAudioOnly(context, "content://fixture/video-a", false)
        assertFalse(AudioOnlyMode.isAudioOnly(context, "content://fixture/video-a"))
    }
}
