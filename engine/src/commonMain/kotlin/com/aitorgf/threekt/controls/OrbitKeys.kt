package com.aitorgf.threekt.controls

import kotlin.js.JsExport

/** `KeyboardEvent.code` values that pan or, with Ctrl, Meta, or Shift, rotate the camera. */
@JsExport
class OrbitKeys(
    var left: String = "ArrowLeft",
    var up: String = "ArrowUp",
    var right: String = "ArrowRight",
    var bottom: String = "ArrowDown",
)
