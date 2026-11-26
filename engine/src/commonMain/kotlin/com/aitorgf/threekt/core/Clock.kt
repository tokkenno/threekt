package com.aitorgf.threekt.core

import kotlin.js.JsExport
import kotlin.jvm.JvmName

/**
 * Measures elapsed time in seconds. Three.js deprecated this class in r183 in favor of [Timer], which it
 * reports with a console warning; Threekt marks it with [Deprecated] instead.
 *
 * Times come from a monotonic clock in milliseconds, as `performance.now()` in Three.js.
 */
@Deprecated("Use Timer instead.", ReplaceWith("Timer()"))
@JsExport
class Clock(
    /** Whether the clock starts on the first call to [getDelta] or [getElapsedTime]. */
    var autoStart: Boolean = true,
) {
    /** Monotonic time in milliseconds when [start] was last called. */
    var startTime = 0.0

    /** Monotonic time in milliseconds of the last [start], [getDelta] or [getElapsedTime] call. */
    var oldTime = 0.0

    /**
     * Seconds accumulated while the clock was running. On the JVM its getter is named `elapsedTime()`
     * to avoid clashing with [getElapsedTime].
     */
    @get:JvmName("elapsedTime")
    var elapsedTime = 0.0

    /** Whether the clock is running. */
    var running = false

    /** Starts the clock and resets [elapsedTime]. Also called by the first [getDelta] when [autoStart] is set. */
    fun start() {
        startTime = performanceNow()
        oldTime = startTime
        elapsedTime = 0.0
        running = true
    }

    /** Stops the clock, accumulating the time since the last call, and disables [autoStart]. */
    fun stop() {
        getElapsedTime()
        running = false
        autoStart = false
    }

    /** Updates the clock and returns the seconds elapsed since it started. */
    fun getElapsedTime(): Double {
        getDelta()
        return elapsedTime
    }

    /**
     * Returns the seconds elapsed since the previous call, or since the clock started, and adds them to
     * [elapsedTime]. Returns `0` when the clock is stopped, and on the call that auto-starts it.
     */
    fun getDelta(): Double {
        var diff = 0.0
        if (autoStart && !running) {
            start()
            return 0.0
        }
        if (running) {
            val newTime = performanceNow()
            diff = (newTime - oldTime) / 1000.0
            oldTime = newTime
            elapsedTime += diff
        }
        return diff
    }
}
