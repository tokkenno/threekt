package com.aitorgf.threekt.math

@ExperimentalJsExport
@JsExport
actual external interface MutableVector : Vector {
    @JsName("setArray")
    actual fun set(vararg values: Double): Vector
    actual fun setIndex(index: Int, value: Double): Vector
}
