package com.prayagi.netraplayer

import androidx.media3.common.PlaybackException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackErrorsTest {
    @Test fun missingFileSaysSo() {
        assertTrue(playbackErrorMessage(PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND).contains("not found"))
    }
    @Test fun lostPermissionSaysSo() {
        assertTrue(playbackErrorMessage(PlaybackException.ERROR_CODE_IO_NO_PERMISSION).contains("permission"))
    }
    @Test fun unsupportedFormatSaysSo() {
        assertTrue(playbackErrorMessage(PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED).contains("cannot play"))
    }
    @Test fun unknownCodeKeepsGenericMessage() {
        assertEquals("This file could not be played on this phone.", playbackErrorMessage(PlaybackException.ERROR_CODE_UNSPECIFIED))
    }
}
