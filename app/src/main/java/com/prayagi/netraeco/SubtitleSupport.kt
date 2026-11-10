package com.prayagi.netraplayer

import androidx.media3.common.MimeTypes

/**
 * External subtitle formats Netra Player accepts. Every entry maps to a parser that
 * EXISTS in Media3 1.11.1 (DefaultSubtitleParserFactory: SubRip, WebVTT, SSA/ASS) -
 * verified against the Media3 source, not assumed. Encodings: UTF-8 (incl. Hindi)
 * and UTF-16 via BOM detection inside Media3; legacy non-UTF files are not decoded
 * correctly and that is a documented limit, not a hidden one.
 */
internal object SubtitleSupport {

    /** Media3 mime for a picked subtitle, or null when we cannot support it. Name leads, provider MIME only backs it up. */
    fun mimeFor(name: String?, providerMime: String?): String? {
        val n = name?.lowercase().orEmpty()
        val p = providerMime?.lowercase().orEmpty()
        return when {
            n.endsWith(".srt") || p == "application/x-subrip" -> MimeTypes.APPLICATION_SUBRIP
            n.endsWith(".vtt") || p == "text/vtt" -> MimeTypes.TEXT_VTT
            n.endsWith(".ass") || n.endsWith(".ssa") || p == "text/x-ssa" -> MimeTypes.TEXT_SSA
            else -> null
        }
    }

    /** Short user-facing label for the picker filter and errors. */
    const val ACCEPTED_LABEL = ".srt, .vtt or .ass/.ssa"
}
