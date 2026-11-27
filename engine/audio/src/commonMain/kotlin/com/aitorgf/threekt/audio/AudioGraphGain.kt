package com.aitorgf.threekt.audio

import kotlin.js.JsExport

/** A node that scales its input, the platform-neutral counterpart of the Web Audio `GainNode`. */
@JsExport
interface AudioGraphGain : AudioGraphNode {
    val gain: AudioGraphParam
}
