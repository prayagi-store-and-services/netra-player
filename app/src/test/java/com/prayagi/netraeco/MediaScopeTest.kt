package com.prayagi.netraplayer

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaScopeTest {
    @Test fun acceptsVideoAndMp3() {
        assertTrue(isVideoOrMp3("video/mp4", "a.mp4"))
        assertTrue(isVideoOrMp3("audio/mpeg", "a.mp3"))
        assertTrue(isVideoOrMp3("audio/mp3", null))
        assertTrue(isVideoOrMp3(null, "song.MP3"))
        assertTrue(isVideoOrMp3("application/octet-stream", "song.mp3"))
    }
    @Test fun refusesOtherFiles() {
        assertFalse(isVideoOrMp3("audio/flac", "a.flac"))
        assertFalse(isVideoOrMp3("audio/ogg", "a.ogg"))
        assertFalse(isVideoOrMp3("image/png", "a.png"))
        assertFalse(isVideoOrMp3(null, "notes.txt"))
        assertFalse(isVideoOrMp3(null, null))
    }
}
