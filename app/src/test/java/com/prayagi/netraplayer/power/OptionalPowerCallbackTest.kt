package com.prayagi.netraplayer.power

import org.junit.Assert.*
import org.junit.Test

class OptionalPowerCallbackTest {
    @Test fun unavailableServiceUsesConservativeFallbackWithoutCrashing() {
        var fallback = false
        assertFalse(OptionalPowerCallback.register({ throw RuntimeException("Thermal status listener failed to set") }, { fallback = true }))
        assertTrue(fallback)
    }
    @Test fun supportedServiceRegistersOnceWithoutFallback() {
        var calls = 0; var fallback = false
        assertTrue(OptionalPowerCallback.register({ calls++ }, { fallback = true }))
        assertEquals(1, calls); assertFalse(fallback)
    }
}
