package com.aitorgf.threekt.lights

import com.aitorgf.threekt.math.Color
import kotlin.js.JsExport

/** Light that illuminates every surface equally from all directions. It has no direction or shadows. */
@JsExport
class AmbientLight(color: Color = Color(1.0, 1.0, 1.0), intensity: Double = 1.0) : Light(color, intensity) {
    /** Creates a light from a packed sRGB [hex] color, as `0xcccccc`. */
    @JsExport.Ignore
    constructor(hex: Int, intensity: Double = 1.0) : this(Color(hex), intensity)
}
