package com.aitorgf.threekt.scenes

import com.aitorgf.threekt.math.Color
import kotlin.js.JsExport

/** Common type for the linear and exponential fog accepted by [Scene.fog]. */
@JsExport
sealed class SceneFog(color: Color) {
    /** Fog color in the working color space. A copy of the supplied color. */
    val color = Color(color.r, color.g, color.b)

    /** Optional name for debugging. */
    var name = ""

    /** Returns independent fog with the same settings. */
    abstract fun clone(): SceneFog
}
