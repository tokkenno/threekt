package com.aitorgf.threekt.scenes

import com.aitorgf.threekt.math.Color
import kotlin.js.JsExport

/**
 * Linear fog that blends objects toward [color] between the distances [near] and [far] from the camera.
 * It affects the built-in materials whose `fog` property is set; raw shader materials apply it only if
 * their shaders do.
 */
@JsExport
class Fog(color: Color, var near: Double = 1.0, var far: Double = 1000.0) {
    /** Fog color in the working color space. A copy of the color passed to the constructor. */
    val color = Color(color.r, color.g, color.b)

    /** Name of the fog, for debugging. */
    var name = ""

    /** Returns a new fog with the same color and distances. */
    fun clone(): Fog = Fog(color, near, far).also { it.name = name }
}
