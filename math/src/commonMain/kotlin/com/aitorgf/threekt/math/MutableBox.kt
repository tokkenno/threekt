package com.aitorgf.threekt.math

import com.aitorgf.threekt.types.ComplexBuffer
import kotlin.js.JsName

expect interface MutableBox : Box {
    @JsName("setFromBuffer")
    fun set(buffer: ComplexBuffer): MutableBox
}
