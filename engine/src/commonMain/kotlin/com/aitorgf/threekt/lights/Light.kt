package com.aitorgf.threekt.lights

import com.aitorgf.threekt.core.Object3D
import com.aitorgf.threekt.math.Color
import kotlin.js.JsExport

/** Base class for lights. The light's contribution is [color] multiplied by [intensity]. */
@JsExport
abstract class Light(color: Color, var intensity: Double) : Object3D() {
    /** Light color in the working color space. A copy of the color passed to the constructor. */
    val color = Color(color.r, color.g, color.b)
}
