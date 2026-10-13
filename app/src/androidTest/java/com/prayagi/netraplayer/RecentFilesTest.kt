package com.prayagi.netraplayer

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test

class RecentFilesTest {
    private fun await(check: () -> Boolean) {
        val until = System.currentTimeMillis() + 6000
        while (!check() && System.currentTimeMillis() < until) Thread.sleep(50)
        assertTrue("Recent database condition completed", check())
    }
    @Test fun boundedLocalHistoryPrivateAndClearSuppression() {
        val c = InstrumentationRegistry.getInstrumentation().targetContext
        RecentFiles.start(c)
        RecentFiles.setPrivate(c, false)
        RecentFiles.clear(c, null)
        await { RecentFiles.items.value.isEmpty() }
        for (i in 0..24) { RecentFiles.selected(c); RecentFiles.save(c, "content://fixture/$i", "File $i", i.toLong()) }
        await { RecentFiles.items.value.size == 20 && RecentFiles.items.value.first().uri == "content://fixture/24" }
        RecentFiles.save(c, "content://fixture/24", "File 24", 5000)
        await { RecentFiles.items.value.first().positionMs == 5000L }
        RecentFiles.setPrivate(c, true)
        RecentFiles.save(c, "content://fixture/private", "Private file", 1)
        Thread.sleep(250)
        assertFalse(RecentFiles.items.value.any { it.uri.endsWith("/private") })
        assertNull(LastPlayed.load(c))
        RecentFiles.setPrivate(c, false)
        RecentFiles.clear(c, "content://fixture/24")
        RecentFiles.save(c, "content://fixture/24", "File 24", 6000)
        await { RecentFiles.items.value.isEmpty() }
        Thread.sleep(250)
        assertTrue("Clear does not recreate currently playing history", RecentFiles.items.value.isEmpty())
        RecentFiles.selected(c)
        RecentFiles.save(c, "content://fixture/24", "File 24", -1)
        await { RecentFiles.items.value.size == 1 }
        assertEquals(0L, RecentFiles.items.value.first().positionMs)
        RecentFiles.remove(c, "content://fixture/24", "content://fixture/24")
        await { RecentFiles.items.value.isEmpty() }
    }
}
