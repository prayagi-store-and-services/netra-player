package com.prayagi.netraplayer

import java.util.Calendar
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Test

class HeaderTextTest {
    private fun at(h: Int, m: Int, s: Int, ms: Int): Date = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 3, h, m, s); set(Calendar.MILLISECOND, ms) }.time

    @Test fun dateIsDayMonthYear() = assertEquals("03 10 2026", HeaderText.date(at(1, 2, 3, 4)))

    @Test fun clockHasMilliseconds() = assertEquals("18:05:09.007", HeaderText.clock(at(18, 5, 9, 7)))
}
