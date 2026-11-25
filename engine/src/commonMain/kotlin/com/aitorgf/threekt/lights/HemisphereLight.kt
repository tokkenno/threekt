package com.aitorgf.threekt.lights

import com.aitorgf.threekt.math.Color
import kotlin.js.JsExport

/**
 * Indirect light that blends its [groundColor] and sky [color] according to the surface normal.
 * Its world position defines the sky direction and defaults to +Y. Colors are copied.
 */
@JsExport
class HemisphereLight(
    skyColor: Color = Color(1.0, 1.0, 1.0),
    groundColor: Color = Color(1.0, 1.0, 1.0),
    intensity: Double = 1.0,
) : Light(skyColor, intensity) {
    /** Color on the hemisphere opposite the light's direction. */
    val groundColor = Color(groundColor.r, groundColor.g, groundColor.b)

    init {
        position.copy(DEFAULT_UP)
        updateMatrix()
    }
}
