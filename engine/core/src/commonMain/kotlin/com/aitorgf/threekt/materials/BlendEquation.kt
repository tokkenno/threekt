package com.aitorgf.threekt.materials

import kotlin.js.JsExport

/** Equation used to combine scaled colors when [Blending.Custom] is selected. */
@JsExport
enum class BlendEquation {
    Add, Subtract, ReverseSubtract, Min, Max,
}
