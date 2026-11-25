package com.aitorgf.threekt.math.color

import kotlin.js.JsExport
/** Built-in color-space identifiers used by the color registry and CSS. */
@JsExport
enum class ColorSpace(val id: String) {
    SRGB("srgb"),
    LinearSRGB("srgb-linear"),
    None(""),
}
