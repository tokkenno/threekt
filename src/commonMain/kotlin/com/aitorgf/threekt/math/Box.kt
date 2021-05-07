package com.aitorgf.threekt.math

import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

@ExperimentalJsExport
@JsExport
interface Box {
    val center: Vector
    val size: Vector
    val empty: Boolean
}
