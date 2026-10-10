package com.prayagi.netraplayer

import org.junit.Assert.*
import org.junit.Test

class RegionPolicyTest {
    @Test fun listedCountrySignalsBlock() {
        for (country in RegionPolicy.BLOCKED_COUNTRIES) {
            assertTrue(RegionPolicy.blocked(country, "", "IN"))
            assertTrue(RegionPolicy.blocked("US", country, "IN"))
        }
    }
    @Test fun indianSimAlwaysAllows() {
        for (country in RegionPolicy.BLOCKED_COUNTRIES) assertFalse(RegionPolicy.blocked("in", country, country))
    }
    @Test fun localeOnlyWhenBothPhoneSignalsEmpty() {
        assertTrue(RegionPolicy.blocked("", null, " bd "))
        assertFalse(RegionPolicy.blocked("US", "", "BD"))
        assertFalse(RegionPolicy.blocked("", "GB", "PK"))
    }
    @Test fun emptyAndUnlistedSignalsFailOpen() {
        assertFalse(RegionPolicy.blocked(null, "", ""))
        assertFalse(RegionPolicy.blocked("IN", "IN", "IN"))
        assertFalse(RegionPolicy.blocked("GB", "US", "AF"))
    }
}
