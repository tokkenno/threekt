package com.aitorgf.threekt.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TimerTest {
    private class FakeVisibility : PageVisibility {
        override var hidden = false
        val listeners = mutableListOf<() -> Unit>()
        override fun addVisibilityChangeListener(listener: () -> Unit) {
            listeners.add(listener)
        }
        override fun removeVisibilityChangeListener(listener: () -> Unit) {
            listeners.remove(listener)
        }
        fun change(hidden: Boolean) {
            this.hidden = hidden
            listeners.toList().forEach { it() }
        }
    }

    @Test
    fun deltasFollowTimestampsAndTimescale() {
        val timer = Timer()
        val start = performanceNow()
        timer.update(start + 1000.0)
        timer.update(start + 1500.0)
        assertEquals(0.5, timer.getDelta(), 1e-9)
        timer.setTimescale(2.0)
        timer.update(start + 2000.0)
        assertEquals(1.0, timer.getDelta(), 1e-9)
        assertTrue(timer.getElapsed() > 2.4)
    }

    @Test
    fun hiddenPagesProduceZeroDeltas() {
        val timer = Timer()
        val visibility = FakeVisibility()
        timer.connect(visibility)
        val start = performanceNow()
        timer.update(start + 100.0)
        visibility.change(true)
        timer.update(start + 5000.0)
        assertEquals(0.0, timer.getDelta())
        timer.dispose()
        assertTrue(visibility.listeners.isEmpty())
    }

    @Suppress("DEPRECATION")
    @Test
    fun clockStartsOnFirstDelta() {
        val clock = Clock()
        assertEquals(0.0, clock.getDelta())
        assertTrue(clock.running)
        assertTrue(clock.getElapsedTime() >= 0.0)
        clock.stop()
        assertEquals(0.0, clock.getDelta())
    }
}
