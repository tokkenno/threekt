package com.aitorgf.threekt.audio

import kotlin.js.JsExport

/**
 * Holds the [AudioGraphContext] shared by every [AudioListener] and by `AudioLoader`.
 *
 * Threekt deviations: upstream creates a browser `AudioContext` on first use. Threekt creates the platform default
 * (a `WebAudioContext` in the browser; the JVM has no audio backend yet and throws), and [setContext] also accepts
 * null so that a closed context can be replaced by a new one on the next [getContext].
 */
@JsExport
object AudioContext {
    private var context: AudioGraphContext? = null

    /** Returns the shared context, creating the platform default on first use. */
    fun getContext(): AudioGraphContext = context ?: createDefaultAudioContext().also { context = it }

    /** Replaces the shared context. Objects created before keep the context they were created with. */
    fun setContext(value: AudioGraphContext?) {
        context = value
    }
}

/** Creates the default context of the platform, or throws if the platform has no audio backend. */
internal expect fun createDefaultAudioContext(): AudioGraphContext
