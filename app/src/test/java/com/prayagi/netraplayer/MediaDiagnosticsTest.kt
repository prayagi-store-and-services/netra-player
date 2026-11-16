package com.prayagi.netraplayer

import androidx.media3.common.C
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaDiagnosticsTest {
    @Test
    fun containerHint_readsExtensionOnly() {
        assertEquals("mkv", MediaDiagnostics.containerHint("movie.MKV"))
        assertEquals("srt", MediaDiagnostics.containerHint("a.b.srt"))
        assertNull(MediaDiagnostics.containerHint("noextension"))
        assertNull(MediaDiagnostics.containerHint(null))
        assertNull(MediaDiagnostics.containerHint("bad.toooolongext"))
    }

    @Test
    fun trackLine_labelsTypeAndSelection() {
        assertEquals("Video: video/avc (selected)",
            MediaDiagnostics.trackLine(MediaDiagnostics.TrackInfo(C.TRACK_TYPE_VIDEO, "video/avc", true)))
        assertEquals("Audio: unknown codec (not selected)",
            MediaDiagnostics.trackLine(MediaDiagnostics.TrackInfo(C.TRACK_TYPE_AUDIO, null, false)))
        assertEquals("Subtitle: application/x-subrip (selected)",
            MediaDiagnostics.trackLine(MediaDiagnostics.TrackInfo(C.TRACK_TYPE_TEXT, "application/x-subrip", true)))
    }

    @Test
    fun panelText_showsFailureAndNeverPaths() {
        val lines = MediaDiagnostics.panelText(
            "clip.mp4", "video/mp4",
            listOf(MediaDiagnostics.TrackInfo(C.TRACK_TYPE_VIDEO, "video/hevc", true)),
            "This phone cannot play this kind of file.")
        assertTrue(lines.any { it.startsWith("Last failure:") })
        assertTrue(lines.none { MediaDiagnostics.leaksPath(it) })
    }

    @Test
    fun leaksPath_detectsContentUris() {
        assertTrue(MediaDiagnostics.leaksPath("content://media/external/file/1"))
        assertTrue(MediaDiagnostics.leaksPath("/storage/emulated/0/x"))
        assertFalse(MediaDiagnostics.leaksPath("clip.mp4"))
    }
}
