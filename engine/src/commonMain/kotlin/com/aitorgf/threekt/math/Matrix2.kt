package com.aitorgf.threekt.math

import kotlin.math.*

/**
 * A 2×2 matrix, initialized to the identity by default. [set] takes row-major arguments and [elements]
 * stores them in column-major order.
 */
class Matrix2() {
    val elements = DoubleArray(4) { if (it % 3 == 0) 1.0 else 0.0 }

    /** Creates a matrix from four elements in row-major order. */
    constructor(n11: Double, n12: Double, n21: Double, n22: Double) : this() {
        set(n11, n12, n21, n22)
    }

    /**
     * Sets the elements of the matrix. The arguments are supposed to be in row-major order. Returns this
     * instance.
     */
    fun set(n11: Double, n12: Double, n21: Double, n22: Double): Matrix2 {
        elements[0] = n11
        elements[1] = n21
        elements[2] = n12
        elements[3] = n22
        return this
    }

    /** Sets this matrix to the 2x2 identity matrix. Returns this instance. */
    fun identity(): Matrix2 {
        for (index in elements.indices) elements[index] = if (index % 3 == 0) 1.0 else 0.0
        return this
    }

    /** Copies the column-major elements from [array] starting at [offset] and returns this matrix. */
    fun fromArray(array: DoubleArray, offset: Int = 0): Matrix2 {
        array.copyInto(elements, startIndex = offset, endIndex = offset + elements.size)
        return this
    }

    /** Compares this value with [other] for component-wise equality. */
    override fun equals(other: Any?): Boolean = other is Matrix2 && elements.contentEquals(other.elements)

    /** Returns a hash based on the current component values. */
    override fun hashCode(): Int = elements.contentHashCode()
}
