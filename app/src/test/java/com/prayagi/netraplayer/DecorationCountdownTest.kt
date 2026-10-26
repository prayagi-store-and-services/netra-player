package com.prayagi.netraplayer

import org.junit.Assert.*
import org.junit.Test

class DecorationCountdownTest {
    @Test fun sampleDoesNotImplyLiveSeconds() {
        assertEquals("0d 00h 01m (sampled)", decorationCountdown(90_000L, false))
        assertEquals("Estimate passed, still being finished", decorationCountdown(0, false))
    }
    @Test fun survivalDoesNotClaimFreshCountdown() {
        assertEquals("Paused (power saving)", decorationCountdown(90_000L, true))
    }
}
