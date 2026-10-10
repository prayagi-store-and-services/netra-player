package com.prayagi.netraplayer

import androidx.compose.runtime.*
import kotlinx.coroutines.delay
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
    val enabled = remember { mutableStateOf(PlayerDesignGate.enabled(clock())) }
    val lifecycle = androidx.compose.ui.platform.LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, clock) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) enabled.value = PlayerDesignGate.enabled(clock())
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(clock) {
        while (true) {
            enabled.value = PlayerDesignGate.enabled(clock())
            delay(1000)
        }
    }
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
