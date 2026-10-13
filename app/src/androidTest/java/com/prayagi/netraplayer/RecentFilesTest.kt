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

class RecentMigrationTest {
    @Test fun migrateLegacyOnceAndDatabaseReopens() {
        val c = InstrumentationRegistry.getInstrumentation().targetContext
        // Use isolated ContextWrapper prefs so production test state is untouched.
        val isolated = object : android.content.ContextWrapper(c) {
            override fun getSharedPreferences(name: String, mode: Int): android.content.SharedPreferences =
                c.getSharedPreferences("migration_test_$name", mode)
        }
        isolated.getSharedPreferences("netra_history_settings", 0).edit().clear().commit()
        isolated.getSharedPreferences("netra_last_played", 0).edit().clear()
            .putString("uri", "content://migration/file").putString("name", "Legacy file")
            .putLong("pos", 75000).putLong("at", 12345).commit()
        val name = "recent-migration-test.db"
        c.deleteDatabase(name)
        var db = androidx.room.Room.databaseBuilder(c, RecentDatabase::class.java, name).build()
        try {
            RecentFiles.migrateLegacy(isolated, db)
            val item = db.recent().list().single()
            assertEquals(75000L, item.positionMs)
            assertEquals(12345L, item.playedAtMs)
            assertFalse(isolated.getSharedPreferences("netra_last_played", 0).contains("uri"))
            db.recent().clear()
            isolated.getSharedPreferences("netra_last_played", 0).edit().putString("uri", "content://stale/file").putString("name", "Stale").commit()
            RecentFiles.migrateLegacy(isolated, db)
            assertTrue("Migration runs once", db.recent().list().isEmpty())
            db.recent().put(item)
            db.close()
            db = androidx.room.Room.databaseBuilder(c, RecentDatabase::class.java, name).build()
            assertEquals(item, db.recent().list().single())
        } finally {
            db.close(); c.deleteDatabase(name)
            isolated.getSharedPreferences("netra_history_settings", 0).edit().clear().commit()
            isolated.getSharedPreferences("netra_last_played", 0).edit().clear().commit()
        }
    }
}
