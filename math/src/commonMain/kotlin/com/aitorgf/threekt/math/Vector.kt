package com.aitorgf.threekt.math

expect interface Vector {
    /** The vector dimensionality */
    val dimension: Int

    /** Get the value in the vector [index]. */
    fun get(index: Int): Double

    /** Get the vector as array */
    fun toArray(): DoubleArray

    /** Converts the vector to mutable */
    fun toMutable(): MutableVector
}
