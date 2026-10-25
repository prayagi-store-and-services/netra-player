package com.prayagi.netraplayer

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateSourceTest {
    @Test fun bothStatesNameNetraEcoAsTheUpdatePath() {
        assertTrue(updateSourceMessage(true).contains("Netra Eco"))
        assertTrue(updateSourceMessage(false).contains("Netra Eco"))
    }

    @Test fun presentAndAbsentMessagesDifferHonestly() {
        assertFalse(updateSourceMessage(true) == updateSourceMessage(false))
        assertTrue(updateSourceMessage(true).contains("on this phone"))
    }

    @Test fun absentStatePointsToTheWebsiteWithoutPromisingAutomation() {
        val absent = updateSourceMessage(false)
        assertTrue(absent.contains("Netra website"))
        assertFalse(absent.contains("on this phone"))
    }

    @Test fun ecoPackageNameIsStable() {
        assertTrue(NETRA_ECO_PACKAGE.matches(Regex("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+$")))
    }
}
