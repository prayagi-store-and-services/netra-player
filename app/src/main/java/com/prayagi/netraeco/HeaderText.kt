package com.prayagi.netraplayer

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Text for the fixed header: date as DD MM YYYY and a live clock as HH:MM:SS.mmm, and the latitude and longitude. */
object HeaderText {
    fun date(now: Date): String = SimpleDateFormat("dd MM yyyy", Locale.US).format(now)

    fun clock(now: Date): String = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(now)

    /** Real coordinates only. When there is no fix the header says Unavailable and never shows a made-up position. */
    fun place(lat: Double?, lon: Double?): String =
        if (lat == null || lon == null || lat.isNaN() || lon.isNaN() || lat !in -90.0..90.0 || lon !in -180.0..180.0) "Location Unavailable"
        else String.format(Locale.US, "Lat %.4f, Long %.4f", lat, lon)
}
