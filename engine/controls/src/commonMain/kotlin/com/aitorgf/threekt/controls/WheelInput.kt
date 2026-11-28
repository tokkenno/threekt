package com.aitorgf.threekt.controls

import kotlin.js.JsExport

/**
 * Platform-independent wheel event. [deltaY] is in pixels, positive when scrolling down. [pinch] is true
 * when the platform reports a trackpad pinch through the wheel, which controls treat as a stronger zoom.
 */
@JsExport
class WheelInput(val x: Double, val y: Double, val deltaY: Double, val pinch: Boolean = false)
