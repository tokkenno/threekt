package com.aitorgf.threekt.materials

import kotlin.js.JsExport

/** Equation used to combine scaled colors when [Blending.Custom] is selected. */
@JsExport
enum class BlendEquation(internal val glValue: Int) {
    Add(0x8006), Subtract(0x800A), ReverseSubtract(0x800B), Min(0x8007), Max(0x8008),
}
