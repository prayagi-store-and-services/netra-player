package com.prayagi.netraplayer

import org.junit.Assert.*
import org.junit.Test

class VideoFitTest {
    @Test fun explicitModesCycleBackToUndistortedFit() {
        assertEquals(VideoFit.CROP, VideoFit.FIT.next())
        assertEquals(VideoFit.STRETCH, VideoFit.CROP.next())
        assertEquals(VideoFit.FIT, VideoFit.STRETCH.next())
        assertEquals(0, VideoFit.FIT.resizeMode)
        assertEquals(4, VideoFit.CROP.resizeMode)
        assertEquals(3, VideoFit.STRETCH.resizeMode)
    }
}
