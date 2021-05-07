package com.aitorgf.threekt.math

import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

@ExperimentalJsExport
@JsExport
external interface MutableVector : Vector {
    fun set(vararg values: Double): Vector
    fun setIndex(index: Int, value: Double): Vector
}
