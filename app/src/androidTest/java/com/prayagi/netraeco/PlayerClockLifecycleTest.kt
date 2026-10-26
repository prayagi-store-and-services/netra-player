package com.prayagi.netraplayer

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.prayagi.netraplayer.power.LifecycleCallbackLease
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Tests the exact callback owner used by adaptiveHeaderClock, not a retired 33ms loop. */
@RunWith(AndroidJUnit4::class)
class PlayerClockLifecycleTest {
    @Test fun callbackLeaseStopsResumesAndDisposesWithoutLeaking() {
        var starts = 0
        var stops = 0
        var visible = false
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            lateinit var lease: LifecycleCallbackLease
            scenario.onActivity { activity ->
                lease = LifecycleCallbackLease(activity.lifecycle, { starts++ }, { stops++ }, { visible = it })
                assertEquals(1, starts)
                assertTrue(visible)
            }
            scenario.moveToState(Lifecycle.State.CREATED)
            scenario.onActivity {
                assertEquals(1, stops)
                assertFalse(visible)
            }
            scenario.moveToState(Lifecycle.State.RESUMED)
            scenario.onActivity {
                assertEquals(2, starts)
                assertTrue(visible)
                lease.close()
                lease.close()
                assertEquals(2, stops)
                assertFalse(visible)
            }
            scenario.moveToState(Lifecycle.State.CREATED)
            scenario.moveToState(Lifecycle.State.RESUMED)
            scenario.onActivity {
                assertEquals(2, starts)
                assertEquals(2, stops)
            }
        }
    }
}
