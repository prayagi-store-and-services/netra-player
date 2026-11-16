package com.prayagi.netraplayer

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioOnlyModeTest {
    @Test fun videoDisabledOnlyForAudioOnlyOrBackground() {
        assertTrue(AudioOnlyMode.disableVideoTrack(audioOnly = true, appForeground = true))
        assertTrue(AudioOnlyMode.disableVideoTrack(audioOnly = true, appForeground = false))
        assertTrue(AudioOnlyMode.disableVideoTrack(audioOnly = false, appForeground = false))
        assertFalse(AudioOnlyMode.disableVideoTrack(audioOnly = false, appForeground = true))
    }

    @Test fun foregroundResumeNeverOverridesUserAudioOnlyChoice() {
        // Regression: returning to the app must NOT re-enable video when the
        // user picked audio-only for this item.
        assertTrue(AudioOnlyMode.disableVideoTrack(audioOnly = true, appForeground = true))
    }

    @Test fun preferenceKeyIsStablePerItem() {
        assertTrue(AudioOnlyMode.key("content://x/1").startsWith("m:"))
        assertTrue(AudioOnlyMode.key("a") != AudioOnlyMode.key("b"))
        assertTrue(AudioOnlyMode.key("same") == AudioOnlyMode.key("same"))
    }
}
