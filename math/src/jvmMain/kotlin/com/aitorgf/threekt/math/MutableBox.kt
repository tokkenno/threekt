package com.aitorgf.threekt.math

import com.aitorgf.threekt.types.ComplexBuffer

actual interface MutableBox : Box {
    actual fun set(buffer: ComplexBuffer): MutableBox
}
