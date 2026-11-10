package com.prayagi.netraplayer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ResumeStoreTest {
    @Test
    fun encodeDecode_roundTrips_specialChars() {
        val e = ResumeStore.decodeEntry(ResumeStore.encodeEntry(90_000L, 1_700_000_000_000L, "a|b \\ c", "content://x/y|z"))!!
        assertEquals(90_000L, e.positionMs)
        assertEquals(1_700_000_000_000L, e.savedAtMs)
        assertEquals("a|b \\ c", e.name)
        assertEquals("content://x/y|z", e.uri)
    }

    @Test
    fun decode_rejectsGarbage() {
        assertNull(ResumeStore.decodeEntry(null))
        assertNull(ResumeStore.decodeEntry(""))
        assertNull(ResumeStore.decodeEntry("1|2|3"))
        assertNull(ResumeStore.decodeEntry("x|2|n|u"))
        assertNull(ResumeStore.decodeEntry("1|2|n|u|extra"))
    }

    @Test
    fun decode_clampsNegativePositions() {
        assertEquals(0L, ResumeStore.decodeEntry("-5|1|n|u")!!.positionMs)
    }

    @Test
    fun indexWithFirst_movesToFront_dedupes_caps() {
        val idx = listOf("a", "b", "c")
        assertEquals(listOf("b", "a", "c"), ResumeStore.indexWithFirst(idx, "b"))
        assertEquals(listOf("z", "a", "b", "c"), ResumeStore.indexWithFirst(idx, "z"))
        val big = (1..60).map { "k$it" }
        assertEquals(ResumeStore.MAX_ENTRIES, ResumeStore.indexWithFirst(big, "new").size)
        assertEquals("new", ResumeStore.indexWithFirst(big, "new").first())
    }

    @Test
    fun evictedKeys_findsDroppedEntries() {
        val old = listOf("a", "b", "c")
        val next = listOf("c", "a")
        assertEquals(listOf("b"), ResumeStore.evictedKeys(old, next))
        assertTrue(ResumeStore.evictedKeys(next, next).isEmpty())
    }
}
