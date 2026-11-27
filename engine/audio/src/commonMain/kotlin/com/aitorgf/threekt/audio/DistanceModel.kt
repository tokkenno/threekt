package com.aitorgf.threekt.audio

import kotlin.js.JsExport

/** How a panner attenuates a source with distance, as the Web Audio `DistanceModelType`. */
@JsExport
enum class DistanceModel {
    Linear,
    Inverse,
    Exponential,
}
