package com.prayagi.netraplayer

import org.junit.Assert.assertTrue
import org.junit.Test

/** v1.1.16.1 (background update download) sorts after 1.1.16 but before the planned 1.1.17, so the roadmap stays honest. */
class BackgroundUpdateVersionTest {
    @Test fun patchReleaseSortsCorrectly() {
        assertTrue(compareVersions("1.1.16.1", "1.1.16") > 0)
        assertTrue(compareVersions("1.1.16.1", "1.1.17") < 0)
        assertTrue(compareVersions("v1.1.16.1", "1.1.16.1") == 0)
    }
}
