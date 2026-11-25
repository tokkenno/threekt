package com.aitorgf.threekt.lights

import com.aitorgf.threekt.math.Color
import kotlin.js.JsExport
import kotlin.math.PI

/**
 * Light emitted from its world position in every direction, as Three.js `PointLight`. Its intensity is
 * in candela and falls off with the distance raised to [decay]; a positive [distance] also fades it to
 * zero at that range. Shadows are not supported yet. The color is copied.
 */
@JsExport
class PointLight(
    color: Color = Color(1.0, 1.0, 1.0),
    intensity: Double = 1.0,
    /** Range at which the light reaches zero, or zero for no limit. */
    var distance: Double = 0.0,
    /** Exponent of the distance falloff. 2 is physically correct. */
    var decay: Double = 2.0,
) : Light(color, intensity) {
    /** Luminous power in lumens, equal to `intensity * 4π`. */
    var power: Double
        get() = intensity * 4 * PI
        set(value) {
            intensity = value / (4 * PI)
        }
}
