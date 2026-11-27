package com.aitorgf.threekt.audio

import kotlin.js.JsExport

/** Decoded audio data, the platform-neutral counterpart of the Web Audio `AudioBuffer`. */
@JsExport
interface AudioGraphBuffer {
    /** Length of the data in seconds. */
    val duration: Double
}
