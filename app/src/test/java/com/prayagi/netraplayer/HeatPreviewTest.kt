package com.prayagi.netraplayer

import org.junit.Assert.*
import org.junit.Test

class HeatPreviewTest {
    private fun reading(t: Long, cpu: Long, temp: Int? = 390, thermal: Int? = 2) =
        HeatReading(t, cpu, temp, false, thermal)
    @Test fun offByDefaultAndNoSampling() {
        val gate = HeatPreviewGate()
        assertFalse(gate.enabled); assertNull(gate.begin()); assertNull(gate.preview)
    }
    @Test fun computesOwnCpuNotWholeDevice() {
        val p = HeatPreview.between(reading(100, 50), reading(5_100, 300))!!
        assertEquals(5.0, p.cpuCorePercent, 0.0001)
        assertEquals(250L, p.processCpuMs); assertEquals(390, p.batteryDeciC)
    }
    @Test fun multicorePercentageNotSilentlyClamped() {
        assertEquals(200.0, HeatPreview.between(reading(0, 0), reading(5_000, 10_000))!!.cpuCorePercent, 0.0001)
    }
    @Test fun refusesShortWindow() { assertNull(HeatPreview.between(reading(0, 0), reading(4_999, 20))) }
    @Test fun refusesBackgroundOrLongWindow() { assertNull(HeatPreview.between(reading(0, 0), reading(15_001, 20))) }
    @Test fun refusesClockReversal() { assertNull(HeatPreview.between(reading(100, 0), reading(90, 20))) }
    @Test fun refusesCpuReset() { assertNull(HeatPreview.between(reading(0, 30), reading(5_000, 20))) }
    @Test fun refusesNegativeCounters() { assertNull(HeatPreview.between(reading(-1, 0), reading(5_000, 20))) }
    @Test fun preservesUnavailableSensors() {
        val p = HeatPreview.between(reading(0, 0), HeatReading(5_000, 1, null, null, null))!!
        assertNull(p.batteryDeciC); assertNull(p.thermalStatus); assertNull(p.charging)
    }
    @Test fun invalidSensorsAreUnavailableNotHealthy() {
        val p = HeatPreview.between(reading(0, 0), reading(5_000, 20, 20_000, 9))!!
        assertNull(p.batteryDeciC); assertNull(p.thermalStatus)
    }
    @Test fun oneFlightOnly() {
        val g = HeatPreviewGate(); g.setEnabled(true); assertNotNull(g.begin()); assertNull(g.begin())
    }
    @Test fun offInvalidatesInFlightAndClearsPreview() {
        val g = HeatPreviewGate(); g.setEnabled(true); val token = g.begin()!!; g.setEnabled(false)
        g.finish(token, HeatPreview.between(reading(0, 0), reading(5_000, 20)))
        assertNull(g.preview); assertNull(g.begin())
    }
    @Test fun oldSessionCannotReplaceNewSession() {
        val g = HeatPreviewGate(); g.setEnabled(true); val old = g.begin()!!; g.cancel(); val fresh = g.begin()!!
        g.finish(old, HeatPreview.between(reading(0, 0), reading(5_000, 20)))
        assertNull(g.preview); assertNull(g.begin())
        g.finish(fresh, HeatPreview.between(reading(0, 0), reading(5_000, 20))); assertNotNull(g.preview)
    }
    @Test fun clearDropsMemoryOnlySnapshot() {
        val g = HeatPreviewGate(); g.setEnabled(true); val t = g.begin()!!
        g.finish(t, HeatPreview.between(reading(0, 0), reading(5_000, 20))); g.clear(); assertNull(g.preview)
    }
    @Test fun staleFinallyCannotClearNewSession() {
        val g = HeatPreviewGate(); g.setEnabled(true); val old = g.begin()!!; g.cancel(); val fresh = g.begin()!!
        assertFalse(g.isCurrent(old)); assertTrue(g.isCurrent(fresh))
    }
}
