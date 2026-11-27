package com.aitorgf.threekt.controls

import kotlin.js.JsExport

/** Platform-independent key press. [code] names the physical key, such as `ArrowLeft`. */
@JsExport
class KeyInput(
    val code: String,
    val ctrlKey: Boolean = false,
    val metaKey: Boolean = false,
    val shiftKey: Boolean = false,
) {
    /** Whether Ctrl, Meta, or Shift was held. */
    val hasModifier: Boolean get() = ctrlKey || metaKey || shiftKey
}
