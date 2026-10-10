package com.prayagi.netraplayer
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDateTime
class PlayerDesignGateTest {
    @Test fun localMidnightBoundary() {
        assertFalse(PlayerDesignGate.enabled(LocalDateTime.of(2026,10,10,23,59,59)))
        assertTrue(PlayerDesignGate.enabled(LocalDateTime.of(2026,10,11,0,0)))
        assertTrue(PlayerDesignGate.enabled(LocalDateTime.of(2026,10,12,1,0)))
        assertFalse(PlayerDesignGate.enabled(LocalDateTime.of(2026,10,9,20,0)))
    }
}
