package com.prayagi.netraplayer

import androidx.media3.common.MimeTypes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SubtitleSupportTest {
    @Test fun srtVttAssSsaAccepted() {
        assertEquals(MimeTypes.APPLICATION_SUBRIP, SubtitleSupport.mimeFor("movie.srt", null))
        assertEquals(MimeTypes.APPLICATION_SUBRIP, SubtitleSupport.mimeFor("MOVIE.SRT", "application/octet-stream"))
        assertEquals(MimeTypes.TEXT_VTT, SubtitleSupport.mimeFor("movie.vtt", null))
        assertEquals(MimeTypes.TEXT_SSA, SubtitleSupport.mimeFor("movie.ass", null))
        assertEquals(MimeTypes.TEXT_SSA, SubtitleSupport.mimeFor("movie.ssa", null))
    }

    @Test fun providerMimeIsFallbackOnly() {
        assertEquals(MimeTypes.APPLICATION_SUBRIP, SubtitleSupport.mimeFor(null, "application/x-subrip"))
        assertEquals(MimeTypes.TEXT_VTT, SubtitleSupport.mimeFor("file", "text/vtt"))
        assertEquals(MimeTypes.TEXT_SSA, SubtitleSupport.mimeFor("file", "text/x-ssa"))
        // Name wins over a generic provider type.
        assertEquals(MimeTypes.APPLICATION_SUBRIP, SubtitleSupport.mimeFor("x.srt", "application/octet-stream"))
    }

    @Test fun unsupportedFilesRejected() {
        assertNull(SubtitleSupport.mimeFor("notes.txt", "text/plain"))
        assertNull(SubtitleSupport.mimeFor("sub.ttml", "application/ttml+xml")) // not wired; do not claim TTML
        assertNull(SubtitleSupport.mimeFor("sub.srt.exe", "application/octet-stream"))
        assertNull(SubtitleSupport.mimeFor(null, null))
        assertNull(SubtitleSupport.mimeFor("noext", null))
    }
}
