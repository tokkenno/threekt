package com.aitorgf.threekt.math

import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

@ExperimentalJsExport
@JsExport
object MathUtils {
    fun clamp(value: Double, min: Double, max: Double): Double {
        return kotlin.math.max(min, kotlin.math.min(max, value))
    }
}
