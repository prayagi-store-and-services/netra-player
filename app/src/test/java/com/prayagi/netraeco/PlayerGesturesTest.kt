package com.prayagi.netraplayer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerGesturesTest {

    @Test fun smallMovementStaysUndecided() {
        assertEquals(PlayerGestures.DragMode.UNDECIDED, PlayerGestures.decideDragMode(0.2f, 5f, 5f))
    }

    @Test fun verticalDragOnLeftIsBrightnessOnRightIsVolume() {
        assertEquals(PlayerGestures.DragMode.BRIGHTNESS, PlayerGestures.decideDragMode(0.2f, 0f, 100f))
        assertEquals(PlayerGestures.DragMode.VOLUME, PlayerGestures.decideDragMode(0.8f, 0f, 100f))
    }

    @Test fun horizontalDragIsSeekAnywhere() {
        assertEquals(PlayerGestures.DragMode.SEEK, PlayerGestures.decideDragMode(0.2f, 100f, 10f))
        assertEquals(PlayerGestures.DragMode.SEEK, PlayerGestures.decideDragMode(0.9f, -100f, 10f))
    }

    @Test fun dragUpRaisesLevelDragDownLowersIt() {
        assertEquals(0.75f, PlayerGestures.levelAfterDrag(0.5f, -250f, 1000f), 0.001f)
        assertEquals(0.25f, PlayerGestures.levelAfterDrag(0.5f, 250f, 1000f), 0.001f)
    }

    @Test fun levelClampsToZeroAndOne() {
        assertEquals(1f, PlayerGestures.levelAfterDrag(0.9f, -1000f, 1000f), 0.001f)
        assertEquals(0f, PlayerGestures.levelAfterDrag(0.1f, 1000f, 1000f), 0.001f)
        assertEquals(0.5f, PlayerGestures.levelAfterDrag(0.5f, 100f, 0f), 0.001f)
    }

    @Test fun fullWidthDragSeeksFourMinutes() {
        assertEquals(240_000L, PlayerGestures.seekDeltaMs(1000f, 1000f))
        assertEquals(-120_000L, PlayerGestures.seekDeltaMs(-500f, 1000f))
        assertEquals(0L, PlayerGestures.seekDeltaMs(100f, 0f))
    }

    @Test fun seekTargetClampsToBounds() {
        assertEquals(0L, PlayerGestures.seekTargetMs(5_000L, -60_000L, 600_000L))
        assertEquals(600_000L, PlayerGestures.seekTargetMs(590_000L, 60_000L, 600_000L))
        assertEquals(65_000L, PlayerGestures.seekTargetMs(5_000L, 60_000L, 600_000L))
    }

    @Test fun doubleTapJumpsTenSecondsWithClamps() {
        assertEquals(20_000L, PlayerGestures.doubleTapTargetMs(10_000L, forward = true, durationMs = 600_000L))
        assertEquals(0L, PlayerGestures.doubleTapTargetMs(5_000L, forward = false, durationMs = 600_000L))
        assertEquals(600_000L, PlayerGestures.doubleTapTargetMs(599_000L, forward = true, durationMs = 600_000L))
    }

    @Test fun labelsAreHonest() {
        assertEquals(75, PlayerGestures.levelPercent(0.75f))
        assertEquals("+30s", PlayerGestures.formatSeekDelta(30_000L))
        assertEquals("-10s", PlayerGestures.formatSeekDelta(-10_000L))
        assertTrue(PlayerGestures.FULL_WIDTH_SEEK_MS == 240_000L)
    }
}
