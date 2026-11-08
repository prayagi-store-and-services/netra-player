package com.prayagi.netraplayer

/**
 * Touch gesture math for the player screen.
 * Pure functions so the behavior is unit-testable on the JVM; no Android classes here.
 */
object PlayerGestures {

    /** A full-width horizontal drag seeks at most this far. */
    const val FULL_WIDTH_SEEK_MS = 240_000L

    /** Double tap on the left/right half jumps this far back/forward. */
    const val DOUBLE_TAP_SEEK_MS = 10_000L

    /** Below this many pixels a drag is treated as a tap, not a gesture. */
    const val TAP_SLOP_PX = 24f

    /** Which adjustment a drag performs, decided from where it started and its direction. */
    enum class DragMode { UNDECIDED, BRIGHTNESS, VOLUME, SEEK }

    /** Left third of the player adjusts brightness, right third volume, anywhere horizontal seeks. */
    fun decideDragMode(startXFraction: Float, totalDx: Float, totalDy: Float): DragMode {
        if (kotlin.math.abs(totalDx) < TAP_SLOP_PX && kotlin.math.abs(totalDy) < TAP_SLOP_PX) return DragMode.UNDECIDED
        return if (kotlin.math.abs(totalDx) > kotlin.math.abs(totalDy)) {
            DragMode.SEEK
        } else {
            if (startXFraction < 0.5f) DragMode.BRIGHTNESS else DragMode.VOLUME
        }
    }

    /** Dragging up increases, dragging down decreases. Full height covers the whole 0..1 range. */
    fun levelAfterDrag(startLevel: Float, totalDyPx: Float, spanPx: Float): Float {
        if (spanPx <= 0f) return startLevel
        return (startLevel - totalDyPx / spanPx).coerceIn(0f, 1f)
    }

    /** Horizontal drag distance mapped to a seek delta; full width = FULL_WIDTH_SEEK_MS. */
    fun seekDeltaMs(totalDxPx: Float, spanPx: Float): Long {
        if (spanPx <= 0f) return 0L
        return (totalDxPx / spanPx * FULL_WIDTH_SEEK_MS).toLong()
    }

    fun seekTargetMs(currentMs: Long, deltaMs: Long, durationMs: Long): Long {
        val upper = if (durationMs > 0) durationMs else Long.MAX_VALUE
        return (currentMs + deltaMs).coerceIn(0L, upper)
    }

    fun doubleTapTargetMs(currentMs: Long, forward: Boolean, durationMs: Long): Long =
        seekTargetMs(currentMs, if (forward) DOUBLE_TAP_SEEK_MS else -DOUBLE_TAP_SEEK_MS, durationMs)

    fun levelPercent(level: Float): Int = (level.coerceIn(0f, 1f) * 100f).toInt()

    fun formatSeekDelta(deltaMs: Long): String {
        val s = deltaMs / 1000
        return if (s >= 0) "+${s}s" else "${s}s"
    }
}
