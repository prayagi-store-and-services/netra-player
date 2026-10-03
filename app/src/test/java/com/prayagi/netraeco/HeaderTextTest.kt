package com.prayagi.netraplayer

import java.util.Calendar
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Test

class HeaderTextTest {
    private fun at(h: Int, m: Int, s: Int, ms: Int): Date = Calendar.getInstance().apply { set(2026, Calendar.OCTOBER, 3, h, m, s); set(Calendar.MILLISECOND, ms) }.time

    @Test fun dateIsDayMonthYear() = assertEquals("03 10 2026", HeaderText.date(at(1, 2, 3, 4)))

    @Test fun clockHasMilliseconds() = assertEquals("18:05:09.007", HeaderText.clock(at(18, 5, 9, 7)))

    @Test fun placeShowsRealCoordinates() = assertEquals("Lat 12.3457, Long 78.9012", HeaderText.place(12.34567, 78.90123))

    @Test fun placeIsUnavailableWithoutFixOrWithBadValues() {
        assertEquals("Location Unavailable", HeaderText.place(null, null))
        assertEquals("Location Unavailable", HeaderText.place(95.0, 10.0))
        assertEquals("Location Unavailable", HeaderText.place(10.0, Double.NaN))
    }
}
