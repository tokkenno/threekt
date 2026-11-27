package com.aitorgf.threekt.audio

import kotlin.js.JsExport

/** An audio processing graph and its clock, the platform-neutral counterpart of the Web Audio `BaseAudioContext`. */
@JsExport
interface AudioGraphContext {
    /** Time in seconds of the context's clock, which only advances while it is running. */
    val currentTime: Double

    /** The final node, connected to the audio output. */
    val destination: AudioGraphNode
    val listener: AudioGraphListener

    fun createGain(): AudioGraphGain
    fun createPanner(): AudioGraphPanner
    fun createBufferSource(): AudioGraphBufferSource
    fun createAnalyser(): AudioGraphAnalyser
}
