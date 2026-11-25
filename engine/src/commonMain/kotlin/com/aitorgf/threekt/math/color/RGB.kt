package com.aitorgf.threekt.math.color

import com.aitorgf.threekt.math.Color
import kotlin.js.JsExport

/** Mutable red, green, and blue components. */
@JsExport
data class RGB(var r: Double = 0.0, var g: Double = 0.0, var b: Double = 0.0) {
    /** Creates a color by interpreting these components in [colorSpace]. */
    fun toColor(colorSpace: ColorSpace = ColorManagement.workingColorSpace): Color =
        Color().setRGB(r, g, b, colorSpace)
}
