package com.prayagi.netraplayer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PipSupportTest {

    @Test fun offeredOnlyForVideoOnCapableDevice() {
        assertTrue(PipSupport.shouldOffer(hasVideo = true, deviceSupports = true))
        assertFalse(PipSupport.shouldOffer(hasVideo = false, deviceSupports = true))
        assertFalse(PipSupport.shouldOffer(hasVideo = true, deviceSupports = false))
        assertFalse(PipSupport.shouldOffer(hasVideo = false, deviceSupports = false))
    }

    @Test fun realVideoSizeIsKept() {
        assertEquals(1920 to 1080, PipSupport.aspectOrDefault(1920, 1080))
        assertEquals(1080 to 1920, PipSupport.aspectOrDefault(1080, 1920))
    }

    @Test fun unknownOrExtremeSizeFallsBack() {
        assertEquals(16 to 9, PipSupport.aspectOrDefault(0, 0))
        assertEquals(16 to 9, PipSupport.aspectOrDefault(-1, 1080))
        assertEquals(16 to 9, PipSupport.aspectOrDefault(100, 1000))
        assertEquals(239 to 100, PipSupport.aspectOrDefault(3000, 1000))
    }
}
