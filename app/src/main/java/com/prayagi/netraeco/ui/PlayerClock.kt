package com.prayagi.netraplayer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.Date
import kotlinx.coroutines.delay

/** Only the app-owned notification PendingIntent can open this non-exported entry. */
class UpdateEntryActivity : MainActivity()

/** Preserve the millisecond display while visible; no clock work while the activity is stopped. */
@Composable
internal fun PlayerClockTicks(onTick: (Date) -> Unit) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var started by remember(lifecycle) { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, _ ->
            started = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(started) {
        if (started) while (true) {
            onTick(Date())
            delay(33)
        }
    }
}
