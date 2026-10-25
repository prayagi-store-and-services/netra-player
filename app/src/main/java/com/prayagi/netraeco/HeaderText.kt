package com.prayagi.netraplayer

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Text for the fixed header: date as DD MM YYYY. The accidental clock feature is removed. */
object HeaderText {
    fun date(now: Date): String = SimpleDateFormat("dd MM yyyy", Locale.US).format(now)


}
