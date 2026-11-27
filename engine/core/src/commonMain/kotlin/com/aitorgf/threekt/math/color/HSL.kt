package com.aitorgf.threekt.math.color

import com.aitorgf.threekt.math.Color
import kotlin.js.JsExport

/** Mutable hue, saturation, and lightness components, expressed as fractions. */
@JsExport
data class HSL(var h: Double = 0.0, var s: Double = 0.0, var l: Double = 0.0) {
    /** Creates a color by interpreting these components in [colorSpace]. */
    fun toColor(colorSpace: ColorSpace = ColorManagement.workingColorSpace): Color =
        Color().setHSL(h, s, l, colorSpace)
}
