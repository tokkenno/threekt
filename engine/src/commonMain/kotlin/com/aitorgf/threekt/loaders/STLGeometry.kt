package com.aitorgf.threekt.loaders

import com.aitorgf.threekt.core.BufferGeometry
import kotlin.js.JsExport

/** STL triangle geometry with the default opacity from a colored binary header. */
@JsExport
class STLGeometry internal constructor(val alpha: Double) : BufferGeometry() {
    val hasColors: Boolean get() = hasAttribute("color")
}
