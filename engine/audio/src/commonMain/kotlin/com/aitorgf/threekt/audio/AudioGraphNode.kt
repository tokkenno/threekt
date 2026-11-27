package com.aitorgf.threekt.audio

import kotlin.js.JsExport

/** A node of an audio processing graph, the platform-neutral counterpart of the Web Audio `AudioNode`. */
@JsExport
interface AudioGraphNode {
    /** Sends this node's output to [destination]. */
    fun connect(destination: AudioGraphNode)

    /** Stops sending this node's output to [destination]. */
    fun disconnect(destination: AudioGraphNode)
}
