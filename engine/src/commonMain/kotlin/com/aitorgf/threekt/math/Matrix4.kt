package com.aitorgf.threekt.math

import com.aitorgf.threekt.renderers.CoordinateSystem
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.js.JsExport
import kotlin.js.JsName

/**
 * A 4×4 matrix, initialized to the identity by default. [set] takes row-major arguments and [elements]
 * stores them in column-major order.
 */
@JsExport
class Matrix4() {
    /** Column-major storage. */
    val elements: DoubleArray = DoubleArray(16) { index ->
        if (index % 5 == 0) 1.0 else 0.0
    }

    /** Copies the supplied values into this instance and returns it. */
    @JsName("copyFrom")
    fun copy(source: Matrix4): Matrix4 {
        source.elements.copyInto(elements)
        return this
    }

    /**
     * Inverts this affine matrix in place. A singular matrix is replaced with zeros. Returns this matrix.
     */
    internal fun invertAffine(): Matrix4 {
        val a = elements[0]
        val b = elements[4]
        val c = elements[8]
        val d = elements[1]
        val e = elements[5]
        val f = elements[9]
        val g = elements[2]
        val h = elements[6]
        val i = elements[10]
        val x = elements[12]
        val y = elements[13]
        val z = elements[14]

        val determinant = a * (e * i - f * h) + d * (c * h - b * i) + g * (b * f - c * e)
        if (determinant == 0.0) {
            elements.fill(0.0)
            return this
        }

        val inverseDeterminant = 1.0 / determinant
        elements[0] = (e * i - f * h) * inverseDeterminant
        elements[4] = (c * h - b * i) * inverseDeterminant
        elements[8] = (b * f - c * e) * inverseDeterminant
        elements[1] = (f * g - d * i) * inverseDeterminant
        elements[5] = (a * i - c * g) * inverseDeterminant
        elements[9] = (c * d - a * f) * inverseDeterminant
        elements[2] = (d * h - e * g) * inverseDeterminant
        elements[6] = (b * g - a * h) * inverseDeterminant
        elements[10] = (a * e - b * d) * inverseDeterminant
        elements[12] = -(elements[0] * x + elements[4] * y + elements[8] * z)
        elements[13] = -(elements[1] * x + elements[5] * y + elements[9] * z)
        elements[14] = -(elements[2] * x + elements[6] * y + elements[10] * z)
        return this
    }

    /**
     * Stores the product of [left] and [right] in this matrix. Either operand may be this matrix. Returns
     * this matrix.
     */
    fun multiplyMatrices(left: Matrix4, right: Matrix4): Matrix4 {
        val a = left.elements
        val b = right.elements
        val product = if (left === this) DoubleArray(16) else elements
        for (column in 0 until 16 step 4) {
            val b0 = b[column]
            val b1 = b[column + 1]
            val b2 = b[column + 2]
            val b3 = b[column + 3]
            for (row in 0 until 4) {
                product[column + row] = a[row] * b0 + a[row + 4] * b1 + a[row + 8] * b2 + a[row + 12] * b3
            }
        }
        if (product !== elements) product.copyInto(elements)
        return this
    }

    /**
     * Sets this matrix to the transformation composed of the given position, rotation (Quaternion) and
     * scale. Returns this instance.
     */
    fun compose(position: Vector3, quaternion: Quaternion, scale: Vector3): Matrix4 {
        val x = quaternion.x
        val y = quaternion.y
        val z = quaternion.z
        val w = quaternion.w
        val xx = 2.0 * x * x
        val yy = 2.0 * y * y
        val zz = 2.0 * z * z
        val xy = 2.0 * x * y
        val xz = 2.0 * x * z
        val yz = 2.0 * y * z
        val wx = 2.0 * w * x
        val wy = 2.0 * w * y
        val wz = 2.0 * w * z

        elements[0] = (1.0 - (yy + zz)) * scale.x
        elements[1] = (xy + wz) * scale.x
        elements[2] = (xz - wy) * scale.x
        elements[3] = 0.0
        elements[4] = (xy - wz) * scale.y
        elements[5] = (1.0 - (xx + zz)) * scale.y
        elements[6] = (yz + wx) * scale.y
        elements[7] = 0.0
        elements[8] = (xz + wy) * scale.z
        elements[9] = (yz - wx) * scale.z
        elements[10] = (1.0 - (xx + yy)) * scale.z
        elements[11] = 0.0
        elements[12] = position.x
        elements[13] = position.y
        elements[14] = position.z
        elements[15] = 1.0
        return this
    }

    /** Creates a matrix from sixteen elements in row-major order. */
    @JsName("fromRows")
    constructor(
        n11: Double,
        n12: Double,
        n13: Double,
        n14: Double,
        n21: Double,
        n22: Double,
        n23: Double,
        n24: Double,
        n31: Double,
        n32: Double,
        n33: Double,
        n34: Double,
        n41: Double,
        n42: Double,
        n43: Double,
        n44: Double
    ) : this() {
        set(n11, n12, n13, n14, n21, n22, n23, n24, n31, n32, n33, n34, n41, n42, n43, n44)
    }

    /**
     * Sets the elements of the matrix. The arguments are supposed to be in row-major order. Returns this
     * instance.
     */
    fun set(
        n11: Double,
        n12: Double,
        n13: Double,
        n14: Double,
        n21: Double,
        n22: Double,
        n23: Double,
        n24: Double,
        n31: Double,
        n32: Double,
        n33: Double,
        n34: Double,
        n41: Double,
        n42: Double,
        n43: Double,
        n44: Double
    ): Matrix4 {
        elements[0] = n11
        elements[1] = n21
        elements[2] = n31
        elements[3] = n41
        elements[4] = n12
        elements[5] = n22
        elements[6] = n32
        elements[7] = n42
        elements[8] = n13
        elements[9] = n23
        elements[10] = n33
        elements[11] = n43
        elements[12] = n14
        elements[13] = n24
        elements[14] = n34
        elements[15] = n44
        return this
    }

    /** Sets this matrix to the 4x4 identity matrix. Returns this instance. */
    fun identity(): Matrix4 {
        for (index in elements.indices) elements[index] = if (index % 5 == 0) 1.0 else 0.0
        return this
    }

    /** Copies the column-major elements from [array] starting at [offset] and returns this matrix. */
    fun fromArray(array: DoubleArray, offset: Int = 0): Matrix4 {
        array.copyInto(elements, startIndex = offset, endIndex = offset + elements.size)
        return this
    }

    /** Returns a new instance with the same values. */
    fun copy(): Matrix4 = Matrix4().copy(this)

    /** Returns `true` if this matrix is equal with the given one. */
    override fun equals(other: Any?): Boolean = other is Matrix4 && elements.contentEquals(other.elements)

    /** Returns a hash based on the current component values. */
    override fun hashCode(): Int = elements.contentHashCode()

    /** Writes the column-major elements to [array] starting at [offset] and returns the array. */
    fun toArray(array: DoubleArray = DoubleArray(16), offset: Int = 0): DoubleArray =
        elements.copyInto(array, offset)

    /** Post-multiplies this matrix by the given 4x4 matrix. Returns this instance. */
    fun multiply(matrix: Matrix4): Matrix4 = multiplyMatrices(this, matrix)

    /** Pre-multiplies this matrix by the given 4x4 matrix. Returns this instance. */
    fun premultiply(matrix: Matrix4): Matrix4 = multiplyMatrices(matrix, this)

    /** Multiplies every component of the matrix by the given scalar. Returns this instance. */
    fun multiplyScalar(scalar: Double): Matrix4 {
        for (index in elements.indices) elements[index] *= scalar
        return this
    }

    /** Transposes this matrix in place. Returns this instance. */
    fun transpose(): Matrix4 {
        for (column in 1 until 4) {
            for (row in 0 until column) {
                val index = column * 4 + row
                val opposite = row * 4 + column
                val value = elements[index]
                elements[index] = elements[opposite]
                elements[opposite] = value
            }
        }
        return this
    }

    /**
     * Copies the translation component of the given matrix into this matrix's translation component.
     * Returns this instance.
     */
    fun copyPosition(matrix: Matrix4): Matrix4 =
        setPosition(matrix.elements[12], matrix.elements[13], matrix.elements[14])

    /** Replaces the translation column with the supplied position and returns this matrix. */
    @JsName("setPositionFromVector")
    fun setPosition(vector: Vector3): Matrix4 = setPosition(vector.x, vector.y, vector.z)

    /** Replaces the translation column with the supplied position and returns this matrix. */
    fun setPosition(x: Double, y: Double, z: Double): Matrix4 {
        elements[12] = x
        elements[13] = y
        elements[14] = z
        return this
    }

    /**
     * Set the upper 3x3 elements of this matrix to the values of given 3x3 matrix. Returns this instance.
     */
    fun setFromMatrix3(matrix: Matrix3): Matrix4 {
        val e = matrix.elements
        return set(e[0], e[3], e[6], 0.0, e[1], e[4], e[7], 0.0, e[2], e[5], e[8], 0.0, 0.0, 0.0, 0.0, 1.0)
    }

    /** Extracts the basis vectors of this matrix into the three vectors provided. Returns this instance. */
    fun extractBasis(xAxis: Vector3, yAxis: Vector3, zAxis: Vector3): Matrix4 {
        xAxis.setFromMatrixColumn(this, 0)
        yAxis.setFromMatrixColumn(this, 1)
        zAxis.setFromMatrixColumn(this, 2)
        return this
    }

    /** Sets the given basis vectors to this matrix. Returns this instance. */
    fun makeBasis(x: Vector3, y: Vector3, z: Vector3): Matrix4 =
        set(x.x, y.x, z.x, 0.0, x.y, y.y, z.y, 0.0, x.z, y.z, z.z, 0.0, 0.0, 0.0, 0.0, 1.0)

    /**
     * Extracts the rotation component of the given matrix into this matrix's rotation component. Note:
     * This method does not support reflection matrices. Returns this instance.
     */
    fun extractRotation(matrix: Matrix4): Matrix4 = makeBasis(
        Vector3().setFromMatrixColumn(matrix, 0).let { it.divideScalar(it.length()) },
        Vector3().setFromMatrixColumn(matrix, 1).let { it.divideScalar(it.length()) },
        Vector3().setFromMatrixColumn(matrix, 2).let { it.divideScalar(it.length()) },
    )

    /** Sets this matrix to the rotation represented by [euler]. Returns this matrix. */
    fun makeRotationFromEuler(euler: Euler): Matrix4 = makeRotationFromQuaternion(Quaternion().setFromEuler(euler))

    /** Sets this matrix to the rotation represented by the unit [quaternion]. Returns this matrix. */
    fun makeRotationFromQuaternion(quaternion: Quaternion): Matrix4 =
        compose(Vector3(), quaternion, Vector3(1.0, 1.0, 1.0))

    /**
     * Sets the rotation component of the transformation matrix, looking from `eye` towards `target`, and
     * oriented by the up-direction. Returns this instance.
     */
    fun lookAt(eye: Vector3, target: Vector3, up: Vector3): Matrix4 {
        val z = Vector3().subVectors(eye, target)
        if (z.lengthSq() == 0.0) z.z = 1.0
        z.normalize()
        val x = Vector3().crossVectors(up, z)
        if (x.lengthSq() == 0.0) {
            if (abs(up.z) == 1.0) z.x += 0.0001 else z.z += 0.0001
            z.normalize()
            x.crossVectors(up, z)
        }
        x.normalize()
        val y = Vector3().crossVectors(z, x)
        x.toArray(elements, 0)
        y.toArray(elements, 4)
        z.toArray(elements, 8)
        return this
    }

    /**
     * Computes and returns the determinant of the 4x4 matrix, but assumes the matrix is affine, saving
     * some computations. For affine matrices (like an object's world matrix), this value equals the full
     * 4x4 [Matrix4.determinant] but is cheaper to compute. Assumes the bottom row is [0, 0, 0, 1].
     */
    fun determinantAffine(): Double {
        val m = elements
        return m[0] * (m[5] * m[10] - m[9] * m[6]) +
            m[4] * (m[9] * m[2] - m[1] * m[10]) +
            m[8] * (m[1] * m[6] - m[5] * m[2])
    }

    /** Computes and returns the determinant of this matrix. */
    fun determinant(): Double = (0..3).sumOf { column -> elements[column * 4] * cofactor(0, column) }

    /**
     * Inverts this matrix in place, using an affine fast path when applicable. A singular matrix is
     * replaced with zeros. Returns this matrix.
     */
    fun invert(): Matrix4 {
        if (elements[3] == 0.0 && elements[7] == 0.0 && elements[11] == 0.0 && elements[15] == 1.0) {
            return invertAffine()
        }
        val determinant = determinant()
        if (determinant == 0.0) {
            elements.fill(0.0)
            return this
        }
        val inverse = DoubleArray(16) { index -> cofactor(index / 4, index % 4) / determinant }
        return fromArray(inverse)
    }

    private fun cofactor(excludedRow: Int, excludedColumn: Int): Double {
        val minor = DoubleArray(9)
        var index = 0
        for (column in 0..3) {
            if (column == excludedColumn) continue
            for (row in 0..3) {
                if (row != excludedRow) minor[index++] = elements[column * 4 + row]
            }
        }
        val sign = if ((excludedRow + excludedColumn) % 2 == 0) 1.0 else -1.0
        return sign * MathUtils.determinant3(minor)
    }

    /**
     * Scales each of the first three columns of this matrix by the corresponding component of the given
     * vector. Returns this instance.
     */
    fun scale(vector: Vector3): Matrix4 {
        for (column in 0..2) {
            val scalar = vector[column]
            for (row in 0..3) elements[column * 4 + row] *= scalar
        }
        return this
    }

    /** Gets the maximum scale value of the three axes. */
    fun getMaxScaleOnAxis(): Double {
        val column = Vector3()
        return sqrt(
            maxOf(
                column.setFromMatrixColumn(this, 0).lengthSq(),
                column.setFromMatrixColumn(this, 1).lengthSq(),
                column.setFromMatrixColumn(this, 2).lengthSq(),
            )
        )
    }

    /** Sets this matrix to the supplied 3D translation and returns it. */
    @JsName("makeTranslationFromVector")
    fun makeTranslation(vector: Vector3): Matrix4 = makeTranslation(vector.x, vector.y, vector.z)

    /** Sets this matrix to the supplied 3D translation and returns it. */
    fun makeTranslation(x: Double, y: Double, z: Double): Matrix4 = identity().setPosition(x, y, z)

    /**
     * Sets this matrix as a rotational transformation around the X axis by the given angle. Returns this
     * instance.
     */
    fun makeRotationX(theta: Double): Matrix4 = makeRotationAxis(Vector3(1.0, 0.0, 0.0), theta)

    /**
     * Sets this matrix as a rotational transformation around the Y axis by the given angle. Returns this
     * instance.
     */
    fun makeRotationY(theta: Double): Matrix4 = makeRotationAxis(Vector3(0.0, 1.0, 0.0), theta)

    /**
     * Sets this matrix as a rotational transformation around the Z axis by the given angle. Returns this
     * instance.
     */
    fun makeRotationZ(theta: Double): Matrix4 = makeRotationAxis(Vector3(0.0, 0.0, 1.0), theta)

    /**
     * Sets this matrix as a rotational transformation around the given axis by the given angle. Returns
     * this instance.
     */
    fun makeRotationAxis(axis: Vector3, angle: Double): Matrix4 =
        makeRotationFromQuaternion(Quaternion().setFromAxisAngle(axis, angle))

    /** Sets this matrix as a scale transformation. Returns this instance. */
    fun makeScale(x: Double, y: Double, z: Double): Matrix4 {
        identity()
        elements[0] = x
        elements[5] = y
        elements[10] = z
        return this
    }

    /** Sets this matrix as a shear transformation. Returns this instance. */
    fun makeShear(xy: Double, xz: Double, yx: Double, yz: Double, zx: Double, zy: Double): Matrix4 =
        set(1.0, yx, zx, 0.0, xy, 1.0, zy, 0.0, xz, yz, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0)

    /**
     * Decomposes this matrix into its position, rotation and scale components and provides the result in
     * the given objects. Note: Not all matrices are decomposable in this way. For example, if an object
     * has a non-uniformly scaled parent, then the object's world matrix may not be decomposable, and this
     * method may not be appropriate. Returns this instance.
     */
    fun decompose(position: Vector3, quaternion: Quaternion, scale: Vector3): Matrix4 {
        position.setFromMatrixPosition(this)
        if (determinantAffine() == 0.0) {
            scale.setScalar(1.0)
            quaternion.identity()
            return this
        }
        scale.setFromMatrixScale(this)
        if (determinantAffine() < 0.0) scale.x = -scale.x
        val rotation = copy().scale(Vector3(1.0 / scale.x, 1.0 / scale.y, 1.0 / scale.z))
        quaternion.setFromRotationMatrix(rotation)
        return this
    }

    /**
     * Creates a perspective projection matrix. This is used internally by
     * [PerspectiveCamera.updateProjectionMatrix]. Returns this instance.
     */
    fun makePerspective(
        left: Double, right: Double, top: Double, bottom: Double, near: Double, far: Double,
        coordinateSystem: CoordinateSystem = CoordinateSystem.WebGL, reversedDepth: Boolean = false,
    ): Matrix4 {
        val width = right - left
        val height = top - bottom
        val depth = far - near
        val c = when {
            reversedDepth -> near / depth
            coordinateSystem == CoordinateSystem.WebGL -> -(far + near) / depth
            else -> -far / depth
        }
        val d = when {
            reversedDepth -> far * near / depth
            coordinateSystem == CoordinateSystem.WebGL -> -2.0 * far * near / depth
            else -> -far * near / depth
        }
        return set(
            2.0 * near / width, 0.0, (right + left) / width, 0.0,
            0.0, 2.0 * near / height, (top + bottom) / height, 0.0,
            0.0, 0.0, c, d, 0.0, 0.0, -1.0, 0.0,
        )
    }

    /**
     * Creates an orthographic projection matrix. This is used internally by
     * [OrthographicCamera.updateProjectionMatrix]. Returns this instance.
     */
    fun makeOrthographic(
        left: Double, right: Double, top: Double, bottom: Double, near: Double, far: Double,
        coordinateSystem: CoordinateSystem = CoordinateSystem.WebGL, reversedDepth: Boolean = false,
    ): Matrix4 {
        val width = right - left
        val height = top - bottom
        val depth = far - near
        val c = when {
            reversedDepth -> 1.0 / depth
            coordinateSystem == CoordinateSystem.WebGL -> -2.0 / depth
            else -> -1.0 / depth
        }
        val d = when {
            reversedDepth -> far / depth
            coordinateSystem == CoordinateSystem.WebGL -> -(far + near) / depth
            else -> -near / depth
        }
        return set(
            2.0 / width, 0.0, 0.0, -(right + left) / width,
            0.0, 2.0 / height, 0.0, -(top + bottom) / height,
            0.0, 0.0, c, d, 0.0, 0.0, 0.0, 1.0,
        )
    }
}
