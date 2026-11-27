package com.aitorgf.threekt.audio

import kotlin.js.JsExport

/**
 * An automatable audio value, the platform-neutral counterpart of the Web Audio `AudioParam`.
 *
 * Threekt deviation: upstream uses Web Audio objects directly. The audio classes go through the `AudioGraph*`
 * interfaces so that their logic stays in common code; `WebAudioContext` implements them in the browser.
 */
@JsExport
interface AudioGraphParam {
    var value: Double

    /** Starts an exponential approach to [target] at [startTime] seconds with the given time constant. */
    fun setTargetAtTime(target: Double, startTime: Double, timeConstant: Double)

    /** Ramps linearly from the previous scheduled value to [value], reached at [endTime] seconds. */
    fun linearRampToValueAtTime(value: Double, endTime: Double)

    /** Schedules an instant change to [value] at [startTime] seconds. */
    fun setValueAtTime(value: Double, startTime: Double)
}
