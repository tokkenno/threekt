package com.aitorgf.threekt.core

import kotlin.js.JsExport

/**
 * Measures the time between frames. It reads the time once per frame in [update], so every [getDelta]
 * and [getElapsed] call in a frame returns the same value. Times are in seconds and are scaled by the
 * [timescale][setTimescale]. Three.js deprecated its `Clock` in favor of this class in r183, so Threekt
 * does not port `Clock`.
 *
 * Three.js `connect()` takes a DOM `document`; Threekt takes a [PageVisibility], which
 * `DocumentPageVisibility` implements in the browser.
 */
@JsExport
class Timer {
    private var previousTime = 0.0
    private var currentTime = 0.0
    private val startTime = performanceNow()
    private var delta = 0.0
    private var elapsed = 0.0
    private var timescale = 1.0
    private var visibility: PageVisibility? = null
    private var pageVisibilityHandler: (() -> Unit)? = null

    /**
     * Uses [visibility] to stop the timer while the page is hidden. While it is hidden, [update] reports a
     * delta of zero, and when it becomes visible again the time that passed is skipped.
     */
    fun connect(visibility: PageVisibility) {
        this.visibility = visibility
        val handler = { if (!visibility.hidden) reset() }
        pageVisibilityHandler = handler
        visibility.addVisibilityChangeListener(handler)
    }

    /** Stops observing the [PageVisibility] passed to [connect]. */
    fun disconnect() {
        pageVisibilityHandler?.let { visibility?.removeVisibilityChangeListener(it) }
        pageVisibilityHandler = null
        visibility = null
    }

    /** Returns the scaled seconds between the last two [update] calls. */
    fun getDelta(): Double = delta / 1000.0

    /** Returns the scaled seconds accumulated by all [update] calls. */
    fun getElapsed(): Double = elapsed / 1000.0

    /** Returns the factor applied to the time deltas. */
    fun getTimescale(): Double = timescale

    /** Sets the factor applied to future time deltas, for example `0.5` for half speed. Returns this timer. */
    fun setTimescale(timescale: Double): Timer {
        this.timescale = timescale
        return this
    }

    /** Makes the next [update] measure from now, skipping the time since the previous update. Returns this timer. */
    fun reset(): Timer {
        currentTime = performanceNow() - startTime
        return this
    }

    /** Disconnects the timer from its [PageVisibility]. */
    fun dispose() {
        disconnect()
    }

    /**
     * Advances the timer, usually once per frame. [timestamp] is a time in milliseconds on the same clock
     * as `performance.now()`, such as the argument of a `requestAnimationFrame` callback; when it is null,
     * the current time is read. Returns this timer.
     */
    fun update(timestamp: Double? = null): Timer {
        if (pageVisibilityHandler != null && visibility?.hidden == true) {
            delta = 0.0
        } else {
            previousTime = currentTime
            currentTime = (timestamp ?: performanceNow()) - startTime
            delta = (currentTime - previousTime) * timescale
            elapsed += delta
        }
        return this
    }
}
