package com.prayagi.netraplayer

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CleanupTest {
    @Test fun removesEveryLeftoverInstallerFile() {
        val dir = java.nio.file.Files.createTempDirectory("updates").toFile()
        File(dir, "netra-player-v1.apk").writeText("a")
        File(dir, "pending-x.part").writeText("b")
        assertEquals(2, LegacyUpdateCleanup.cleanDir(dir))
        assertTrue(dir.listFiles()!!.isEmpty())
        dir.delete()
    }

    @Test fun missingFolderIsFine() {
        assertEquals(0, LegacyUpdateCleanup.cleanDir(File("/nonexistent-folder-for-test")))
        assertEquals(0, LegacyUpdateCleanup.cleanDir(null))
    }
}
