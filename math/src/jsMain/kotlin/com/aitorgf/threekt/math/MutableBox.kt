package com.aitorgf.threekt.math

import com.aitorgf.threekt.types.ComplexBuffer

@ExperimentalJsExport
@JsExport
actual external interface MutableBox : Box {
    @JsName("setFromBuffer")
    actual fun set(buffer: ComplexBuffer): MutableBox
}
