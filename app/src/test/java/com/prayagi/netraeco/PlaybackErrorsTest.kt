package com.prayagi.netraplayer

import androidx.media3.common.PlaybackException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackErrorsTest {
    @Test fun labelsUseOnlyFileMetadata() {
        assertEquals("Commentary (en)", trackChoiceLabel("Commentary", "en", 1))
        assertEquals("fr", trackChoiceLabel(null, "fr", 2))
        assertEquals("Track 3 (language unavailable)", trackChoiceLabel("", "und", 3))
        assertEquals("Track 1 (language unavailable)", trackChoiceLabel(null, null, 1))
        assertEquals("Original", trackChoiceLabel(" Original ", "und", 1))
    }

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
