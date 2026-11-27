package com.aitorgf.threekt.controls

import kotlin.js.JsExport

/**
 * Rectangular area, such as a canvas, that delivers pointer and wheel input. Its bounds use the same
 * coordinate space as [PointerInput.x] and [PointerInput.y]. While it has listeners, it captures pressed
 * pointers and disables platform gestures such as touch scrolling.
 */
@JsExport
interface InputSurface : InputSource {
    /** Left edge of the surface. */
    val left: Double
    /** Top edge of the surface. */
    val top: Double
    /** Width of the surface. */
    val width: Double
    /** Height of the surface. */
    val height: Double
    /** Cursor shown over the surface. */
    var cursor: Cursor
}
