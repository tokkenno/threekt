package com.aitorgf.threekt.math

expect interface MutableVector : Vector {
    fun set(vararg values: Double): Vector
    fun setIndex(index: Int, value: Double): Vector
}
