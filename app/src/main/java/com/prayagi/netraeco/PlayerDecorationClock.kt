package com.prayagi.netraplayer

import androidx.compose.runtime.staticCompositionLocalOf
import com.prayagi.netraplayer.power.HeaderClock
import java.util.Date

/** One app-scaffold clock, shared by all decoration. No media position or playback state. */
internal val LocalPlayerDecorationClock = staticCompositionLocalOf { HeaderClock(Date(), false) }
