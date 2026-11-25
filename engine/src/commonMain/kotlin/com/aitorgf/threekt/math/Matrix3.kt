package com.aitorgf.threekt.math

import kotlin.math.*
import kotlin.js.JsExport
import kotlin.js.JsName

/**
 * A 3×3 matrix, initialized to the identity by default. [set] takes row-major arguments and [elements]
 * stores them in column-major order.
 */
@JsExport
class Matrix3() {
    val elements = DoubleArray(9) { if (it % 4 == 0) 1.0 else 0.0 }

    /** Creates a matrix from nine elements in row-major order. */
    @JsName("fromRows")
    constructor(n11: Double, n12: Double, n13: Double, n21: Double, n22: Double, n23: Double, n31: Double, n32: Double, n33: Double) : this() {
        set(n11, n12, n13, n21, n22, n23, n31, n32, n33)
    }

    /**
     * Sets the elements of the matrix. The arguments are supposed to be in row-major order. Returns this
     * instance.
     */
    fun set(n11: Double, n12: Double, n13: Double, n21: Double, n22: Double, n23: Double, n31: Double, n32: Double, n33: Double): Matrix3 {
        elements[0] = n11
        elements[1] = n21
        elements[2] = n31
        elements[3] = n12
        elements[4] = n22
        elements[5] = n32
        elements[6] = n13
        elements[7] = n23
        elements[8] = n33
        return this
    }

    /** Sets this matrix to the 3x3 identity matrix. Returns this instance. */
    fun identity(): Matrix3 {
        for (index in elements.indices) elements[index] = if (index % 4 == 0) 1.0 else 0.0
        return this
    }

    /** Copies the column-major elements from [array] starting at [offset] and returns this matrix. */
    fun fromArray(array: DoubleArray, offset: Int = 0): Matrix3 {
        array.copyInto(elements, startIndex = offset, endIndex = offset + elements.size)
        return this
    }

    /** Returns a new instance with the same values. */
    fun copy(): Matrix3 = Matrix3().copy(this)

    /** Returns `true` if this matrix is equal with the given one. */
    override fun equals(other: Any?): Boolean = other is Matrix3 && elements.contentEquals(other.elements)

    /** Returns a hash based on the current component values. */
    override fun hashCode(): Int = elements.contentHashCode()

    /** Writes the column-major elements to [array] starting at [offset] and returns the array. */
    fun toArray(array: DoubleArray = DoubleArray(9), offset: Int = 0): DoubleArray =
        elements.copyInto(array, offset)

    /** Post-multiplies this matrix by the given 3x3 matrix. Returns this instance. */
    fun multiply(matrix: Matrix3): Matrix3 = multiplyMatrices(this, matrix)

    /** Pre-multiplies this matrix by the given 3x3 matrix. Returns this instance. */
    fun premultiply(matrix: Matrix3): Matrix3 = multiplyMatrices(matrix, this)

    /** Multiplies every component of the matrix by the given scalar. Returns this instance. */
    fun multiplyScalar(scalar: Double): Matrix3 {
        for (index in elements.indices) elements[index] *= scalar
        return this
    }

    /** Transposes this matrix in place. Returns this instance. */
    fun transpose(): Matrix3 {
        for (column in 1 until 3) {
            for (row in 0 until column) {
                val index = column * 3 + row
                val opposite = row * 3 + column
                val value = elements[index]
                elements[index] = elements[opposite]
                elements[opposite] = value
            }
        }
        return this
    }

    /** Copies the supplied values into this instance and returns it. */
    @JsName("copyFrom")
    fun copy(matrix: Matrix3): Matrix3 = fromArray(matrix.elements)

    /** Set this matrix to the upper 3x3 matrix of the given 4x4 matrix. Returns this instance. */
    fun setFromMatrix4(matrix: Matrix4): Matrix3 {
        val e = matrix.elements
        return set(e[0], e[4], e[8], e[1], e[5], e[9], e[2], e[6], e[10])
    }

    /** Extracts the basis of this matrix into the three axis vectors provided. Returns this instance. */
    fun extractBasis(xAxis: Vector3, yAxis: Vector3, zAxis: Vector3): Matrix3 {
        xAxis.setFromMatrix3Column(this, 0)
        yAxis.setFromMatrix3Column(this, 1)
        zAxis.setFromMatrix3Column(this, 2)
        return this
    }

    /** Multiples the given 3x3 matrices and stores the result in this matrix. Returns this instance. */
    fun multiplyMatrices(left: Matrix3, right: Matrix3): Matrix3 {
        val a = left.elements
        val b = right.elements
        val product = DoubleArray(9)
        for (column in 0 until 9 step 3) {
            for (row in 0 until 3) {
                product[column + row] = a[row] * b[column] + a[row + 3] * b[column + 1] + a[row + 6] * b[column + 2]
            }
        }
        return fromArray(product)
    }

    /** Computes and returns the determinant of this matrix. */
    fun determinant(): Double = MathUtils.determinant3(elements)

    /** Inverts this matrix in place. A singular matrix is replaced with zeros. Returns this matrix. */
    fun invert(): Matrix3 {
        val m = elements
        val c0 = m[4] * m[8] - m[7] * m[5]
        val c1 = m[7] * m[2] - m[1] * m[8]
        val c2 = m[1] * m[5] - m[4] * m[2]
        val determinant = m[0] * c0 + m[3] * c1 + m[6] * c2
        if (determinant == 0.0) {
            elements.fill(0.0)
            return this
        }
        return set(
            c0, m[6] * m[5] - m[3] * m[8], m[3] * m[7] - m[6] * m[4],
            c1, m[0] * m[8] - m[6] * m[2], m[6] * m[1] - m[0] * m[7],
            c2, m[3] * m[2] - m[0] * m[5], m[0] * m[4] - m[3] * m[1],
        ).multiplyScalar(1.0 / determinant)
    }

    /**
     * Computes the normal matrix which is the inverse transpose of the upper left 3x3 portion of the given
     * 4x4 matrix. Returns this instance.
     */
    fun getNormalMatrix(matrix: Matrix4): Matrix3 = setFromMatrix4(matrix).invert().transpose()

    /**
     * Transposes this matrix into the supplied array, and returns itself unchanged. Returns this instance.
     */
    fun transposeIntoArray(array: DoubleArray): Matrix3 {
        for (column in 0 until 3) for (row in 0 until 3) array[column * 3 + row] = elements[row * 3 + column]
        return this
    }

    /** Sets the UV transform matrix from offset, repeat, rotation, and center. Returns this instance. */
    fun setUvTransform(tx: Double, ty: Double, sx: Double, sy: Double, rotation: Double, cx: Double, cy: Double): Matrix3 {
        val c = cos(rotation)
        val s = sin(rotation)
        return set(
            sx * c, sx * s, -sx * (c * cx + s * cy) + cx + tx,
            -sy * s, sy * c, -sy * (-s * cx + c * cy) + cy + ty,
            0.0, 0.0, 1.0,
        )
    }

    /** Scales this matrix with the given scalar values. Returns this instance. */
    fun scale(sx: Double, sy: Double): Matrix3 = premultiply(Matrix3().makeScale(sx, sy))

    /** Rotates this matrix by the given angle. Returns this instance. */
    fun rotate(theta: Double): Matrix3 = premultiply(Matrix3().makeRotation(-theta))

    /** Translates this matrix by the given scalar values. Returns this instance. */
    fun translate(tx: Double, ty: Double): Matrix3 = premultiply(Matrix3().makeTranslation(tx, ty))

    /** Sets this matrix to the supplied 2D translation and returns it. */
    @JsName("makeTranslationFromVector")
    fun makeTranslation(vector: Vector2): Matrix3 = makeTranslation(vector.x, vector.y)

    /** Sets this matrix to the supplied 2D translation and returns it. */
    fun makeTranslation(x: Double, y: Double): Matrix3 = set(1.0, 0.0, x, 0.0, 1.0, y, 0.0, 0.0, 1.0)

    /** Sets this matrix as a 2D scale transform. Returns this instance. */
    fun makeScale(x: Double, y: Double): Matrix3 = set(x, 0.0, 0.0, 0.0, y, 0.0, 0.0, 0.0, 1.0)

    /** Sets this matrix as a 2D rotational transformation. Returns this instance. */
    fun makeRotation(theta: Double): Matrix3 {
        val c = cos(theta)
        val s = sin(theta)
        return set(c, -s, 0.0, s, c, 0.0, 0.0, 0.0, 1.0)
    }
}
