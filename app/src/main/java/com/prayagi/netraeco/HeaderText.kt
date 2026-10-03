package com.prayagi.netraplayer

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Text for the fixed header: date as DD MM YYYY and a live clock as HH:MM:SS.mmm. */
object HeaderText {
    fun date(now: Date): String = SimpleDateFormat("dd MM yyyy", Locale.US).format(now)

    fun clock(now: Date): String = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(now)

}
