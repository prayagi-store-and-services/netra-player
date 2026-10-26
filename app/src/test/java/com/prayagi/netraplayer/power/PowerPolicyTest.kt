package com.prayagi.netraplayer.power

import org.junit.Assert.*
import org.junit.Test

class PowerPolicyTest {
    private fun input(percent: Int? = 100, charging: Boolean = false, saver: Boolean = false,
        thermal: Boolean = false, visible: Boolean = true, screen: Boolean = true) =
        PowerInputs(percent, charging, saver, thermal, visible, screen)

    @Test fun exactBoundaryTiers() {
        for ((percent, expected) in listOf(100 to PowerTier.NORMAL, 31 to PowerTier.NORMAL,
            30 to PowerTier.SAVER, 15 to PowerTier.SAVER, 14 to PowerTier.CRITICAL,
            5 to PowerTier.CRITICAL, 4 to PowerTier.SURVIVAL)) {
            assertEquals("battery=$percent", expected, PowerPolicy.tier(input(percent)))
        }
    }
    @Test fun saverAndThermalOverrideCharging() {
        assertEquals(PowerTier.SAVER, PowerPolicy.tier(input(100, charging = true, saver = true)))
        assertEquals(PowerTier.SAVER, PowerPolicy.tier(input(100, charging = true, thermal = true)))
        assertEquals(PowerTier.SAVER, PowerPolicy.tier(input(4, charging = true, thermal = true)))
        assertEquals(PowerTier.NORMAL, PowerPolicy.tier(input(4, charging = true)))
    }
    @Test fun thermalDropsOneTierAndIsBounded() {
        assertEquals(PowerTier.CRITICAL, PowerPolicy.tier(input(20, thermal = true)))
        assertEquals(PowerTier.SURVIVAL, PowerPolicy.tier(input(10, thermal = true)))
        assertEquals(PowerTier.SURVIVAL, PowerPolicy.tier(input(2, thermal = true)))
    }
    @Test fun recoveryHasMarginButEntryIsImmediate() {
        assertEquals(PowerTier.SURVIVAL, PowerPolicy.tier(input(5), PowerTier.SURVIVAL))
        assertEquals(PowerTier.CRITICAL, PowerPolicy.tier(input(7), PowerTier.SURVIVAL))
        assertEquals(PowerTier.CRITICAL, PowerPolicy.tier(input(15), PowerTier.CRITICAL))
        assertEquals(PowerTier.SAVER, PowerPolicy.tier(input(17), PowerTier.CRITICAL))
        assertEquals(PowerTier.SAVER, PowerPolicy.tier(input(31), PowerTier.SAVER))
        assertEquals(PowerTier.NORMAL, PowerPolicy.tier(input(33), PowerTier.SAVER))
        assertEquals(PowerTier.SURVIVAL, PowerPolicy.tier(input(4), PowerTier.NORMAL))
    }
    @Test fun unpluggingAtLowBatteryRestrictsImmediately() {
        assertEquals(PowerTier.SURVIVAL, PowerPolicy.tier(input(4), PowerTier.NORMAL))
        assertEquals(PowerTier.CRITICAL, PowerPolicy.tier(input(14), PowerTier.NORMAL))
    }
    @Test fun previousTierWithThermalDoesNotSkipRequiredDrop() {
        assertEquals(PowerTier.CRITICAL, PowerPolicy.tier(input(31, thermal = true), PowerTier.SAVER))
        assertEquals(PowerTier.SAVER, PowerPolicy.tier(input(33, thermal = true), PowerTier.SAVER))
    }
    @Test fun invalidPercentIsClampedAndSaverKeepsLowBatteryRestrictions() {
        assertEquals(PowerTier.SURVIVAL, PowerPolicy.tier(input(-1)))
        assertEquals(PowerTier.NORMAL, PowerPolicy.tier(input(101)))
        assertEquals(PowerTier.SURVIVAL, PowerPolicy.tier(input(4, saver = true)))
        assertEquals(PowerTier.CRITICAL, PowerPolicy.tier(input(10, saver = true)))
    }
    @Test fun missingBatteryIsConservative() {
        assertEquals(PowerTier.SAVER, PowerPolicy.tier(input(null)))
        assertEquals(PowerTier.SURVIVAL, PowerPolicy.tier(input(null), PowerTier.SURVIVAL))
    }
    @Test fun decorationStopsWhenHiddenOrScreenOffInEveryTier() {
        for (tier in PowerTier.entries) {
            assertNull(PowerPolicy.headerIntervalMillis(input(visible = false), tier))
            assertNull(PowerPolicy.headerIntervalMillis(input(screen = false), tier))
        }
    }
    @Test fun decorationSlowsAndStopsWithoutTouchingQuizState() {
        val i = input()
        assertEquals(30_000L, PowerPolicy.headerIntervalMillis(i, PowerTier.NORMAL))
        assertEquals(60_000L, PowerPolicy.headerIntervalMillis(i, PowerTier.SAVER))
        assertEquals(300_000L, PowerPolicy.headerIntervalMillis(i, PowerTier.CRITICAL))
        assertNull(PowerPolicy.headerIntervalMillis(i, PowerTier.SURVIVAL))
    }
}
