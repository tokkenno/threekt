package com.aitorgf.threekt.lights

import com.aitorgf.threekt.core.Object3D
import com.aitorgf.threekt.math.Color
import kotlin.js.JsExport

/** Blends sky and ground illumination according to the surface normal and the light position. */
@JsExport
class HemisphereLight(
    skyColor: Color = Color(1.0, 1.0, 1.0),
    groundColor: Color = Color(1.0, 1.0, 1.0),
    intensity: Double = 1.0,
) : Light(skyColor, intensity) {
    /** A copy of the supplied ground color. The sky color is [color]. */
    val groundColor = Color(groundColor.r, groundColor.g, groundColor.b)

    init {
        position.copy(Object3D.DEFAULT_UP)
        updateMatrix()
    }
}
