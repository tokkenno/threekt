package com.aitorgf.threekt.controls

import kotlin.js.JsExport

/**
 * Receives input from an [InputSource]. Methods that return a Boolean report whether the event was
 * handled, in which case the source suppresses the platform's default action, such as scrolling.
 */
@JsExport
interface InputListener {
    /** A pointer was pressed on the surface. The surface keeps delivering its moves until it is released. */
    fun onPointerDown(input: PointerInput)
    /** A pressed pointer moved, possibly outside the surface. */
    fun onPointerMove(input: PointerInput)
    /** A pointer was released or its gesture was cancelled. */
    fun onPointerUp(input: PointerInput)
    /** The wheel moved over the surface. */
    fun onWheel(input: WheelInput): Boolean
    /** The platform is about to show a context menu over the surface. */
    fun onContextMenu(): Boolean
    /** A key was pressed. */
    fun onKeyDown(input: KeyInput): Boolean
}
