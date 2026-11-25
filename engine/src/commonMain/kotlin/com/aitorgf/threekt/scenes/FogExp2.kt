package com.aitorgf.threekt.scenes

import com.aitorgf.threekt.math.Color
import kotlin.js.JsExport

/** Fog whose opacity rises with the square of view depth times [density]. */
@JsExport
class FogExp2(color: Color, var density: Double = 0.00025) {
    /** A copy of the supplied color in the working color space. */
    val color = Color(color.r, color.g, color.b)
    var name = ""

    fun clone(): FogExp2 = FogExp2(color, density).also { it.name = name }
}
