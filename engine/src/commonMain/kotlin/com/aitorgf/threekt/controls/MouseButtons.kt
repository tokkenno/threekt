package com.aitorgf.threekt.controls

import kotlin.js.JsExport

/** Actions for each mouse button. A null action ignores that button. */
@JsExport
class MouseButtons(
    var left: MouseAction? = MouseAction.Rotate,
    var middle: MouseAction? = MouseAction.Dolly,
    var right: MouseAction? = MouseAction.Pan,
)
