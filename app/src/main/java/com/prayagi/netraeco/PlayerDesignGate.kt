package com.prayagi.netraplayer

import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import java.time.LocalDateTime

/** Device-local calendar switch. No server, timezone conversion or stored rollout flag. */
internal object PlayerDesignGate {
    val activation: LocalDateTime = LocalDateTime.of(2026, 10, 11, 0, 0)
    fun enabled(now: LocalDateTime): Boolean = !now.isBefore(activation)
}

@Composable
internal fun rememberPlayerDesign(): State<Boolean> {
    val enabled = remember { mutableStateOf(PlayerDesignGate.enabled(LocalDateTime.now())) }
    LaunchedEffect(Unit) {
        while (true) {
            enabled.value = PlayerDesignGate.enabled(LocalDateTime.now())
            delay(1000)
        }
    }
    return enabled
}
