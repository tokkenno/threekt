package com.aitorgf.threekt.audio

import kotlin.js.JsExport

/** How a panner spatializes a source, as the Web Audio `PanningModelType`. */
@JsExport
enum class PanningModel {
    EqualPower,

    /** Head-related transfer function: higher quality, used by [PositionalAudio]. */
    HRTF,
}
