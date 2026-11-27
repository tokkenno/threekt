package com.aitorgf.threekt.audio

import kotlin.js.JsExport

/**
 * A one-shot player of an [AudioGraphBuffer], the platform-neutral counterpart of the Web Audio
 * `AudioBufferSourceNode`. Like its Web Audio counterpart it can only be started once.
 */
@JsExport
interface AudioGraphBufferSource : AudioGraphNode {
    var buffer: AudioGraphBuffer?
    var loop: Boolean
    var loopStart: Double
    var loopEnd: Double

    /** Detune in cents, or null when the platform does not support it. */
    val detune: AudioGraphParam?
    val playbackRate: AudioGraphParam

    /** Called when playback ends on its own or after [stop]. */
    var onEnded: (() -> Unit)?

    /** Starts playback at [time] seconds, from [offset] seconds into the buffer, for [duration] seconds or to the end. */
    fun start(time: Double, offset: Double, duration: Double?)

    /** Stops playback at [time] seconds. */
    fun stop(time: Double)
}
