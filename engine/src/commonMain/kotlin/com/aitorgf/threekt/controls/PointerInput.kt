package com.aitorgf.threekt.controls

import kotlin.js.JsExport

/**
 * Platform-independent pointer event. [x] and [y] are in the same coordinate space as
 * [InputSurface.left] and [InputSurface.top]. [button] is `0` for the primary, `1` for the middle, and
 * `2` for the secondary button; it is ignored for move and up events.
 */
@JsExport
class PointerInput(
    val pointerId: Int,
    val pointerType: PointerType,
    val x: Double,
    val y: Double,
    val button: Int = 0,
    val ctrlKey: Boolean = false,
    val metaKey: Boolean = false,
    val shiftKey: Boolean = false,
) {
    /** Whether Ctrl, Meta, or Shift was held, which swaps rotation and panning. */
    val hasModifier: Boolean get() = ctrlKey || metaKey || shiftKey
}
