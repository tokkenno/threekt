package com.aitorgf.threekt.controls

import kotlin.js.JsExport

/** Actions for one and two touch points. A null action ignores that gesture. */
@JsExport
class Touches(var one: TouchAction? = TouchAction.Rotate, var two: TouchAction? = TouchAction.DollyPan)
