package com.prayagi.netraplayer

import androidx.compose.runtime.*
import java.time.LocalDateTime

/** Device-local calendar switch. No server, timezone conversion or stored rollout flag. */
internal object PlayerDesignGate {
    val activation: LocalDateTime = LocalDateTime.of(2026, 10, 11, 0, 0)
    fun enabled(now: LocalDateTime): Boolean = !now.isBefore(activation)
}

/** Tests supply a clock through composition only; app entry points always use device time. */
internal val LocalPlayerDesignClock = staticCompositionLocalOf<() -> LocalDateTime> { { LocalDateTime.now() } }

@Composable
internal fun rememberPlayerDesign(): State<Boolean> {
    val clock = LocalPlayerDesignClock.current
    val sample = LocalPlayerDecorationClock.current.now
    val enabled = remember(clock, sample) { mutableStateOf(PlayerDesignGate.enabled(clock())) }
    return enabled
}

@Composable
internal fun playerCardColors(): androidx.compose.material3.CardColors {
    val active by rememberPlayerDesign()
    return if (active) androidx.compose.material3.CardDefaults.cardColors(
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
        contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
    ) else androidx.compose.material3.CardDefaults.cardColors()
}
