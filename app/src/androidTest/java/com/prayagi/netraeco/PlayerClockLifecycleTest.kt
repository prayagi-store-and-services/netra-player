package com.prayagi.netraplayer

import androidx.activity.compose.setContent
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlayerClockLifecycleTest {
    @Test fun clockStopsInBackgroundAndResumesWithoutChangingMillisecondText() {
        val ticks = AtomicInteger()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.setContent { PlayerClockTicks { ticks.incrementAndGet() } }
            }
            val deadline = android.os.SystemClock.elapsedRealtime() + 5000L
            while (ticks.get() < 3 && android.os.SystemClock.elapsedRealtime() < deadline) Thread.sleep(20)
            assertTrue("Visible clock must tick", ticks.get() >= 3)
            scenario.moveToState(Lifecycle.State.CREATED)
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            // Let an already queued composition cancellation finish before measuring the stopped interval.
            Thread.sleep(150)
            val stopped = ticks.get()
            Thread.sleep(400)
            assertEquals("Stopped activity must not keep a 33 ms clock running", stopped, ticks.get())
            scenario.moveToState(Lifecycle.State.RESUMED)
            val resumeDeadline = android.os.SystemClock.elapsedRealtime() + 5000L
            while (ticks.get() <= stopped && android.os.SystemClock.elapsedRealtime() < resumeDeadline) Thread.sleep(20)
            assertTrue("Clock resumes after returning", ticks.get() > stopped)
            assertTrue(HeaderText.clock(java.util.Date()).matches(Regex("\\d{2}:\\d{2}:\\d{2}\\.\\d{3}")))
        }
    }
}
