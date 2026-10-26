package com.prayagi.netraplayer.power

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

/** One callback registration per visible lifecycle; close() is also safe before/after STOP. */
internal class LifecycleCallbackLease(
    private val lifecycle: Lifecycle,
    private val start: () -> Unit,
    private val stop: () -> Unit,
    private val onVisible: (Boolean) -> Unit
) : AutoCloseable {
    private var active = false
    private var closed = false
    private val observer = LifecycleEventObserver { _, _ -> reconcile() }

    init {
        lifecycle.addObserver(observer)
        reconcile()
    }

    private fun reconcile() {
        if (closed) return
        val visible = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        if (visible && !active) { start(); active = true }
        if (!visible && active) { stop(); active = false }
        onVisible(visible)
    }

    override fun close() {
        if (closed) return
        closed = true
        lifecycle.removeObserver(observer)
        if (active) { stop(); active = false }
        onVisible(false)
    }
}
