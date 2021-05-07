package com.aitorgf.threekt.math

import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

@ExperimentalJsExport
@JsExport
external interface Vector {
    val dimension: Int
    fun get(index: Int): Double
    fun toArray(): Array<Double>
    fun toMutable(): MutableVector
}
