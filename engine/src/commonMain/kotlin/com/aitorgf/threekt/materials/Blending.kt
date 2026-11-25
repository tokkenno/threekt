package com.aitorgf.threekt.materials

import kotlin.js.JsExport

/** Blend modes of [Material.blending], as the Three.js `NoBlending` to `MultiplyBlending` constants. */
@JsExport
enum class Blending {
    /** Fragments replace the colors behind them. */
    None,
    /** Fragments are mixed over the colors behind them by their alpha. */
    Normal,
    /** Fragment colors, weighted by their alpha, are added to the colors behind them. */
    Additive,
    /** Fragment colors are subtracted from the colors behind them. Requires premultiplied alpha. */
    Subtractive,
    /** Fragment colors multiply the colors behind them. Requires premultiplied alpha. */
    Multiply,
}
