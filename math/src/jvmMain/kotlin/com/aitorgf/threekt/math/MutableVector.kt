package com.aitorgf.threekt.math

actual interface MutableVector : Vector {
    actual fun set(vararg values: Double): Vector
    actual fun setIndex(index: Int, value: Double): Vector
}
