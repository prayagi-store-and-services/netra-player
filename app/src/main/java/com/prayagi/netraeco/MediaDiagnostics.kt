package com.prayagi.netraplayer

import androidx.media3.common.C
import androidx.media3.common.Tracks

/**
 * Local, privacy-safe media info for the "Media info" panel.
 * Never includes the file's path or content - only its display name, container hint and track details.
 */
object MediaDiagnostics {

    data class TrackInfo(val type: Int, val mime: String?, val selected: Boolean)

    /** Container hint from the display name's extension; null when unknown. Never guesses from content. */
    fun containerHint(fileName: String?): String? {
        val ext = fileName?.substringAfterLast('.', "")?.lowercase()
        return ext?.takeIf { it.isNotEmpty() && it.length <= 5 && it.all { c -> c.isLetterOrDigit() } }
    }

    /** Flatten Media3 Tracks into simple rows (type, codec mime, selected). */
    fun flatten(tracks: Tracks): List<TrackInfo> {
        val out = mutableListOf<TrackInfo>()
        for (g in tracks.groups) {
            val tg = g.mediaTrackGroup
            for (i in 0 until tg.length) {
                out.add(TrackInfo(g.type, tg.getFormat(i).sampleMimeType, g.isTrackSelected(i)))
            }
        }
        return out
    }

    fun trackTypeLabel(type: Int): String = when (type) {
        C.TRACK_TYPE_VIDEO -> "Video"
        C.TRACK_TYPE_AUDIO -> "Audio"
        C.TRACK_TYPE_TEXT -> "Subtitle"
        else -> "Other"
    }

    /** One summary line per track, e.g. "Video: video/avc (selected)". */
    fun trackLine(t: TrackInfo): String =
        trackTypeLabel(t.type) + ": " + (t.mime ?: "unknown codec") + if (t.selected) " (selected)" else " (not selected)"

    /** Privacy check: a diagnostics line must never leak a content/file path. */
    fun leaksPath(line: String): Boolean = line.contains("content://") || line.contains("file://") || line.contains("/storage/")

    /** The full panel text. [failureReason] is the plain-words error from playbackErrorMessage, or null. */
    fun panelText(fileName: String?, resolverMime: String?, tracks: List<TrackInfo>, failureReason: String?): List<String> {
        val lines = mutableListOf<String>()
        lines.add("File: " + (fileName ?: "Unknown"))
        val container = containerHint(fileName)
        lines.add("Container: " + (container ?: "unknown") + (resolverMime?.let { " ($it)" } ?: ""))
        if (tracks.isEmpty()) lines.add("Tracks: none loaded yet") else tracks.forEach { lines.add(trackLine(it)) }
        failureReason?.let { lines.add("Last failure: $it") }
        return lines
    }
}
