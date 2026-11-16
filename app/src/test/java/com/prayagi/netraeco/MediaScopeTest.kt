package com.prayagi.netraplayer

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaScopeTest {
    @Test fun acceptsVideoAndCommonAudioByMime() {
        assertTrue(isPlayableMedia("video/mp4", "a.mp4"))
        assertTrue(isPlayableMedia("video/x-matroska", "a.mkv"))
        assertTrue(isPlayableMedia("audio/mpeg", "a.mp3"))
        assertTrue(isPlayableMedia("audio/mp3", null))
        assertTrue(isPlayableMedia("audio/mp4", "a.m4a")) // AAC in M4A container
        assertTrue(isPlayableMedia("audio/aac", "a.aac"))
        assertTrue(isPlayableMedia("audio/wav", "a.wav"))
        assertTrue(isPlayableMedia("audio/x-wav", "b.wav"))
        assertTrue(isPlayableMedia("audio/flac", "a.flac"))
        assertTrue(isPlayableMedia("audio/ogg", "a.ogg"))
        assertTrue(isPlayableMedia("audio/opus", "a.opus"))
    }

    @Test fun extensionFallbackOnlyWhenMimeMissingOrGeneric() {
        assertTrue(isPlayableMedia(null, "song.MP3"))
        assertTrue(isPlayableMedia("application/octet-stream", "song.mp3"))
        assertTrue(isPlayableMedia(null, "track.flac"))
        assertTrue(isPlayableMedia("application/octet-stream", "track.ogg"))
        assertTrue(isPlayableMedia(null, "clip.MKV"))
        assertTrue(isPlayableMedia("application/octet-stream", "voice.m4a"))
        assertTrue(isPlayableMedia(null, "note.opus"))
        assertTrue(isPlayableMedia(null, "rec.wav"))
    }

    @Test fun refusesNonMediaAndUnknownFiles() {
        assertFalse(isPlayableMedia("image/png", "a.png"))
        assertFalse(isPlayableMedia("text/plain", "notes.txt"))
        assertFalse(isPlayableMedia("application/pdf", "doc.pdf"))
        assertFalse(isPlayableMedia(null, "notes.txt"))
        assertFalse(isPlayableMedia(null, "archive.zip"))
        assertFalse(isPlayableMedia("application/octet-stream", "payload.bin"))
        assertFalse(isPlayableMedia(null, "song.mp3.exe")) // last extension wins
        assertFalse(isPlayableMedia(null, "noextension"))
        assertFalse(isPlayableMedia(null, null))
    }

    @Test fun invalidMediaFilesPassFilterButFailAtPlayback() {
        // A corrupt file with a valid name/MIME is admitted to the picker/queue;
        // ExoPlayer + PlaybackErrors then surface the failure honestly to the user.
        assertTrue(isPlayableMedia("audio/mpeg", "corrupt.mp3"))
        assertTrue(isPlayableMedia(null, "broken.flac"))
    }
}
