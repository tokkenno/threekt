package com.aitorgf.threekt.math

import kotlin.math.*
import kotlin.random.Random

/**
 * A mutable four-component vector. It can represent homogeneous coordinates, a viewport, or an axis and
 * angle.
 */
class Vector4(
    var x: Double = 0.0,
    var y: Double = 0.0,
    var z: Double = 0.0,
    var w: Double = 1.0,
) : MutableVector<Vector4>() {
    override val self: Vector4 get() = this
    override val dimension: Int get() = 4

    /** Sets the supplied components and returns this vector. */
    fun set(x: Double, y: Double, z: Double, w: Double): Vector4 {
        this.x = x
        this.y = y
        this.z = z
        this.w = w
        return this
    }

    /** Sets the vector components to the same value. Returns this instance. */
    override fun setScalar(scalar: Double): Vector4 = set(scalar, scalar, scalar, scalar)

    /** Returns the component at the zero-based [index], in X, Y, Z, W order. */
    override operator fun get(index: Int): Double = when (index) {
        0 -> x
        1 -> y
        2 -> z
        3 -> w
        else -> throw IndexOutOfBoundsException("Component index: $index")
    }

    /** Sets the component at the zero-based [index]. */
    override operator fun set(index: Int, value: Double) {
        when (index) {
            0 -> x = value
            1 -> y = value
            2 -> z = value
            3 -> w = value
            else -> throw IndexOutOfBoundsException("Component index: $index")
        }
    }

    /** Returns a new instance with the same values. */
    override fun copy(): Vector4 = Vector4(x, y, z, w)

    /** Copies the supplied values into this instance and returns it. */
    fun copy(vector: Vector4): Vector4 = set(vector.x, vector.y, vector.z, vector.w)

    /** Adds the given vector to this instance. Returns this instance. */
    fun add(vector: Vector4): Vector4 = set(x + vector.x, y + vector.y, z + vector.z, w + vector.w)

    /** Subtracts the given vector from this instance. Returns this instance. */
    fun sub(vector: Vector4): Vector4 = set(x - vector.x, y - vector.y, z - vector.z, w - vector.w)

    /** Multiplies the given scalar value with all components of this instance. Returns this instance. */
    override fun multiplyScalar(scalar: Double): Vector4 = set(x * scalar, y * scalar, z * scalar, w * scalar)

    /** Adds the given scalar value to all components of this instance. Returns this instance. */
    fun addScalar(scalar: Double): Vector4 = set(x + scalar, y + scalar, z + scalar, w + scalar)

    /** Adds the given vectors and stores the result in this instance. Returns this instance. */
    fun addVectors(a: Vector4, b: Vector4): Vector4 = set(a.x + b.x, a.y + b.y, a.z + b.z, a.w + b.w)

    /** Subtracts the given scalar value from all components of this instance. Returns this instance. */
    fun subScalar(scalar: Double): Vector4 = set(x - scalar, y - scalar, z - scalar, w - scalar)

    /** Subtracts the given vectors and stores the result in this instance. Returns this instance. */
    fun subVectors(a: Vector4, b: Vector4): Vector4 = set(a.x - b.x, a.y - b.y, a.z - b.z, a.w - b.w)

    /** Multiplies the given vector with this instance. Returns this instance. */
    fun multiply(vector: Vector4): Vector4 = set(x * vector.x, y * vector.y, z * vector.z, w * vector.w)

    /** Divides this instance by the given vector. Returns this instance. */
    fun divide(vector: Vector4): Vector4 = set(x / vector.x, y / vector.y, z / vector.z, w / vector.w)

    /** Adds the given vector scaled by the given factor to this instance. Returns this instance. */
    fun addScaledVector(vector: Vector4, scalar: Double): Vector4 = set(x + vector.x * scalar, y + vector.y * scalar, z + vector.z * scalar, w + vector.w * scalar)

    /**
     * If this vector's x, y, z or w value is greater than the given vector's x, y, z or w value, replace
     * that value with the corresponding min value. Returns this instance.
     */
    fun min(vector: Vector4): Vector4 = set(min(x, vector.x), min(y, vector.y), min(z, vector.z), min(w, vector.w))

    /**
     * If this vector's x, y, z or w value is less than the given vector's x, y, z or w value, replace that
     * value with the corresponding max value. Returns this instance.
     */
    fun max(vector: Vector4): Vector4 = set(max(x, vector.x), max(y, vector.y), max(z, vector.z), max(w, vector.w))

    /**
     * If this vector's x, y, z or w value is greater than the max vector's x, y, z or w value, it is
     * replaced by the corresponding value. If this vector's x, y, z or w value is less than the min
     * vector's x, y, z or w value, it is replaced by the corresponding value. Returns this instance.
     */
    fun clamp(min: Vector4, max: Vector4): Vector4 = set(MathUtils.clamp(x, min.x, max.x), MathUtils.clamp(y, min.y, max.y), MathUtils.clamp(z, min.z, max.z), MathUtils.clamp(w, min.w, max.w))

    /**
     * If this vector's x, y, z or w values are greater than the max value, they are replaced by the max
     * value. If this vector's x, y, z or w values are less than the min value, they are replaced by the
     * min value. Returns this instance.
     */
    fun clampScalar(min: Double, max: Double): Vector4 = set(MathUtils.clamp(x, min, max), MathUtils.clamp(y, min, max), MathUtils.clamp(z, min, max), MathUtils.clamp(w, min, max))

    /**
     * The components of this vector are rounded down to the nearest integer value. Returns this instance.
     */
    fun floor(): Vector4 = set(floor(x), floor(y), floor(z), floor(w))

    /** The components of this vector are rounded up to the nearest integer value. Returns this instance. */
    fun ceil(): Vector4 = set(ceil(x), ceil(y), ceil(z), ceil(w))

    /** Rounds each component to the nearest integer, with ties rounded to even. Returns this vector. */
    fun round(): Vector4 = set(round(x), round(y), round(z), round(w))

    /**
     * The components of this vector are rounded towards zero (up if negative, down if positive) to an
     * integer value. Returns this instance.
     */
    fun roundToZero(): Vector4 = set(truncate(x), truncate(y), truncate(z), truncate(w))

    /** Inverts this vector - i.e. sets x = -x, y = -y, z = -z, w = -w. Returns this instance. */
    fun negate(): Vector4 = set(-x, -y, -z, -w)

    /** Calculates the dot product of the given vector with this instance. */
    override fun dot(vector: Vector4): Double = x * vector.x + y * vector.y + z * vector.z + w * vector.w

    /** Computes the Manhattan length of this vector. */
    fun manhattanLength(): Double = abs(x) + abs(y) + abs(z) + abs(w)

    /**
     * Linearly interpolates between the given vector and this instance, where alpha is the percent
     * distance along the line - alpha = 0 will be this vector, and alpha = 1 will be the given one.
     * Returns this instance.
     */
    fun lerp(vector: Vector4, alpha: Double): Vector4 = set(x + (vector.x - x) * alpha, y + (vector.y - y) * alpha, z + (vector.z - z) * alpha, w + (vector.w - w) * alpha)

    /**
     * Linearly interpolates between the given vectors, where alpha is the percent distance along the line
     * - alpha = 0 will be first vector, and alpha = 1 will be the second one. The result is stored in this
     * instance. Returns this instance.
     */
    fun lerpVectors(a: Vector4, b: Vector4, alpha: Double): Vector4 = set(a.x + (b.x - a.x) * alpha, a.y + (b.y - a.y) * alpha, a.z + (b.z - a.z) * alpha, a.w + (b.w - a.w) * alpha)

    /** Returns `true` if this vector is equal with the given one. */
    override fun equals(other: Any?): Boolean = other is Vector4 && sameComponents(other)

    /** Returns a hash based on the current component values. */
    override fun hashCode(): Int = componentHash()

    /**
     * Sets each component to an independent sample in the interval `[0, 1)` using [random]. Returns this
     * vector.
     */
    fun random(random: Random = Random.Default): Vector4 = set(random.nextDouble(), random.nextDouble(), random.nextDouble(), random.nextDouble())

    var width: Double
        get() = z
        set(value) { z = value }
    var height: Double
        get() = w
        set(value) { w = value }

    /** Multiplies this vector with the given 4x4 matrix. Returns this instance. */
    fun applyMatrix4(matrix: Matrix4): Vector4 {
        val e = matrix.elements
        return set(
            e[0] * x + e[4] * y + e[8] * z + e[12] * w,
            e[1] * x + e[5] * y + e[9] * z + e[13] * w,
            e[2] * x + e[6] * y + e[10] * z + e[14] * w,
            e[3] * x + e[7] * y + e[11] * z + e[15] * w,
        )
    }

    /**
     * Sets the vector components to the position elements of the given transformation matrix. Returns this
     * instance.
     */
    fun setFromMatrixPosition(matrix: Matrix4): Vector4 = fromArray(matrix.elements, 12)

    /**
     * Sets the x, y and z components of this vector to the quaternion's axis and w to the angle. Returns
     * this instance.
     */
    fun setAxisAngleFromQuaternion(quaternion: Quaternion): Vector4 {
        val angle = 2.0 * acos(quaternion.w)
        val sine = sqrt(1.0 - quaternion.w * quaternion.w)
        return if (sine < 0.0001) set(1.0, 0.0, 0.0, angle)
        else set(quaternion.x / sine, quaternion.y / sine, quaternion.z / sine, angle)
    }

    /**
     * Sets the x, y and z components of this vector to the axis of rotation and w to the angle. Returns
     * this instance.
     */
    fun setAxisAngleFromRotationMatrix(matrix: Matrix4): Vector4 {
        val e = matrix.elements
        val axisX = e[6] - e[9]
        val axisY = e[8] - e[2]
        val axisZ = e[1] - e[4]
        val trace = e[0] + e[5] + e[10]
        val epsilon = 0.01
        if (abs(axisX) < epsilon && abs(axisY) < epsilon && abs(axisZ) < epsilon) {
            val xy = (e[4] + e[1]) / 4.0
            val xz = (e[8] + e[2]) / 4.0
            val yz = (e[9] + e[6]) / 4.0
            if (abs(xy) < 0.025 && abs(xz) < 0.025 && abs(yz) < 0.025 && abs(trace - 3.0) < 0.1) {
                return set(1.0, 0.0, 0.0, 0.0)
            }
            val xx = (e[0] + 1.0) / 2.0
            val yy = (e[5] + 1.0) / 2.0
            val zz = (e[10] + 1.0) / 2.0
            val diagonal = 0.707106781
            return when {
                xx > yy && xx > zz -> {
                    if (xx < epsilon) set(0.0, diagonal, diagonal, PI)
                    else sqrt(xx).let { set(it, xy / it, xz / it, PI) }
                }
                yy > zz -> {
                    if (yy < epsilon) set(diagonal, 0.0, diagonal, PI)
                    else sqrt(yy).let { set(xy / it, it, yz / it, PI) }
                }
                else -> {
                    if (zz < epsilon) set(diagonal, diagonal, 0.0, PI)
                    else sqrt(zz).let { set(xz / it, yz / it, it, PI) }
                }
            }
        }
        val length = sqrt(axisX * axisX + axisY * axisY + axisZ * axisZ)
        val divisor = if (length < 0.001) 1.0 else length
        return set(axisX / divisor, axisY / divisor, axisZ / divisor, acos((trace - 1.0) / 2.0))
    }
}
