package com.aitorgf.threekt.math

import kotlin.math.*
import kotlin.random.Random
import kotlin.js.JsExport
import kotlin.js.JsName

/**
 * A mutable two-component vector. It can represent a point, displacement, direction, or any pair of
 * values.
 */
@JsExport
class Vector2(
    var x: Double = 0.0,
    var y: Double = 0.0,
) : MutableVector<Vector2>() {
    override val self: Vector2 get() = this
    override val dimension: Int get() = 2

    /** Sets the supplied components and returns this vector. */
    fun set(x: Double, y: Double): Vector2 {
        this.x = x
        this.y = y
        return this
    }

    /** Sets the vector components to the same value. Returns this instance. */
    override fun setScalar(scalar: Double): Vector2 = set(scalar, scalar)

    /** Returns the component at the zero-based [index], in X, Y order. */
    override operator fun get(index: Int): Double = when (index) {
        0 -> x
        1 -> y
        else -> throw IndexOutOfBoundsException("Component index: $index")
    }

    /** Sets the component at the zero-based [index]. */
    override operator fun set(index: Int, value: Double) {
        when (index) {
            0 -> x = value
            1 -> y = value
            else -> throw IndexOutOfBoundsException("Component index: $index")
        }
    }

    /** Returns a new instance with the same values. */
    override fun copy(): Vector2 = Vector2(x, y)

    /** Copies the supplied values into this instance and returns it. */
    @JsName("copyFrom")
    fun copy(vector: Vector2): Vector2 = set(vector.x, vector.y)

    /** Adds the given vector to this instance. Returns this instance. */
    fun add(vector: Vector2): Vector2 = set(x + vector.x, y + vector.y)

    /** Subtracts the given vector from this instance. Returns this instance. */
    fun sub(vector: Vector2): Vector2 = set(x - vector.x, y - vector.y)

    /** Multiplies the given scalar value with all components of this instance. Returns this instance. */
    override fun multiplyScalar(scalar: Double): Vector2 = set(x * scalar, y * scalar)

    /** Adds the given scalar value to all components of this instance. Returns this instance. */
    fun addScalar(scalar: Double): Vector2 = set(x + scalar, y + scalar)

    /** Adds the given vectors and stores the result in this instance. Returns this instance. */
    fun addVectors(a: Vector2, b: Vector2): Vector2 = set(a.x + b.x, a.y + b.y)

    /** Subtracts the given scalar value from all components of this instance. Returns this instance. */
    fun subScalar(scalar: Double): Vector2 = set(x - scalar, y - scalar)

    /** Subtracts the given vectors and stores the result in this instance. Returns this instance. */
    fun subVectors(a: Vector2, b: Vector2): Vector2 = set(a.x - b.x, a.y - b.y)

    /** Multiplies the given vector with this instance. Returns this instance. */
    fun multiply(vector: Vector2): Vector2 = set(x * vector.x, y * vector.y)

    /** Divides this instance by the given vector. Returns this instance. */
    fun divide(vector: Vector2): Vector2 = set(x / vector.x, y / vector.y)

    /** Adds the given vector scaled by the given factor to this instance. Returns this instance. */
    fun addScaledVector(vector: Vector2, scalar: Double): Vector2 = set(x + vector.x * scalar, y + vector.y * scalar)

    /**
     * If this vector's x or y value is greater than the given vector's x or y value, replace that value
     * with the corresponding min value. Returns this instance.
     */
    fun min(vector: Vector2): Vector2 = set(min(x, vector.x), min(y, vector.y))

    /**
     * If this vector's x or y value is less than the given vector's x or y value, replace that value with
     * the corresponding max value. Returns this instance.
     */
    fun max(vector: Vector2): Vector2 = set(max(x, vector.x), max(y, vector.y))

    /**
     * If this vector's x or y value is greater than the max vector's x or y value, it is replaced by the
     * corresponding value. If this vector's x or y value is less than the min vector's x or y value, it is
     * replaced by the corresponding value. Returns this instance.
     */
    fun clamp(min: Vector2, max: Vector2): Vector2 = set(MathUtils.clamp(x, min.x, max.x), MathUtils.clamp(y, min.y, max.y))

    /**
     * If this vector's x or y values are greater than the max value, they are replaced by the max value.
     * If this vector's x or y values are less than the min value, they are replaced by the min value.
     * Returns this instance.
     */
    fun clampScalar(min: Double, max: Double): Vector2 = set(MathUtils.clamp(x, min, max), MathUtils.clamp(y, min, max))

    /**
     * The components of this vector are rounded down to the nearest integer value. Returns this instance.
     */
    fun floor(): Vector2 = set(floor(x), floor(y))

    /** The components of this vector are rounded up to the nearest integer value. Returns this instance. */
    fun ceil(): Vector2 = set(ceil(x), ceil(y))

    /** Rounds each component to the nearest integer, with ties rounded to even. Returns this vector. */
    fun round(): Vector2 = set(round(x), round(y))

    /**
     * The components of this vector are rounded towards zero (up if negative, down if positive) to an
     * integer value. Returns this instance.
     */
    fun roundToZero(): Vector2 = set(truncate(x), truncate(y))

    /** Inverts this vector - i.e. sets x = -x and y = -y. Returns this instance. */
    fun negate(): Vector2 = set(-x, -y)

    /** Calculates the dot product of the given vector with this instance. */
    override fun dot(vector: Vector2): Double = x * vector.x + y * vector.y

    /** Computes the Manhattan length of this vector. */
    fun manhattanLength(): Double = abs(x) + abs(y)

    /**
     * Linearly interpolates between the given vector and this instance, where alpha is the percent
     * distance along the line - alpha = 0 will be this vector, and alpha = 1 will be the given one.
     * Returns this instance.
     */
    fun lerp(vector: Vector2, alpha: Double): Vector2 = set(x + (vector.x - x) * alpha, y + (vector.y - y) * alpha)

    /**
     * Linearly interpolates between the given vectors, where alpha is the percent distance along the line
     * - alpha = 0 will be first vector, and alpha = 1 will be the second one. The result is stored in this
     * instance. Returns this instance.
     */
    fun lerpVectors(a: Vector2, b: Vector2, alpha: Double): Vector2 = set(a.x + (b.x - a.x) * alpha, a.y + (b.y - a.y) * alpha)

    /** Returns `true` if this vector is equal with the given one. */
    override fun equals(other: Any?): Boolean = other is Vector2 && sameComponents(other)

    /** Returns a hash based on the current component values. */
    override fun hashCode(): Int = componentHash()

    /**
     * Sets each component to an independent sample in the interval `[0, 1)` using [random]. Returns this
     * vector.
     */
    @JsExport.Ignore
    fun random(random: Random = Random.Default): Vector2 = set(random.nextDouble(), random.nextDouble())

    /** Computes the distance from the given vector to this instance. */
    fun distanceTo(vector: Vector2): Double = sqrt(distanceToSquared(vector))

    /**
     * Computes the squared distance from the given vector to this instance. If you are just comparing the
     * distance with another distance, you should compare the distance squared instead as it is slightly
     * more efficient to calculate.
     */
    fun distanceToSquared(vector: Vector2): Double = (x - vector.x).let { it * it } + (y - vector.y).let { it * it }

    /** Computes the Manhattan distance from the given vector to this instance. */
    fun manhattanDistanceTo(vector: Vector2): Double = abs(x - vector.x) + abs(y - vector.y)

    var width: Double
        get() = x
        set(value) { x = value }
    var height: Double
        get() = y
        set(value) { y = value }

    /**
     * Multiplies this vector (with an implicit 1 as the 3rd component) by the given 3x3 matrix. Returns
     * this instance.
     */
    fun applyMatrix3(matrix: Matrix3): Vector2 {
        val e = matrix.elements
        return set(e[0] * x + e[3] * y + e[6], e[1] * x + e[4] * y + e[7])
    }

    /** Calculates the cross product of the given vector with this instance. */
    fun cross(vector: Vector2): Double = x * vector.y - y * vector.x

    /** Computes the angle in radians of this vector with respect to the positive x-axis. */
    fun angle(): Double = atan2(-y, -x) + PI

    /** Returns the angle between the given vector and this instance in radians. */
    fun angleTo(vector: Vector2): Double {
        val denominator = sqrt(lengthSq() * vector.lengthSq())
        return if (denominator == 0.0) PI / 2.0 else acos(MathUtils.clamp(dot(vector) / denominator, -1.0, 1.0))
    }

    /** Rotates this vector around the given center by the given angle. Returns this instance. */
    fun rotateAround(center: Vector2, angle: Double): Vector2 {
        val cosine = cos(angle)
        val sine = sin(angle)
        val dx = x - center.x
        val dy = y - center.y
        return set(dx * cosine - dy * sine + center.x, dx * sine + dy * cosine + center.y)
    }
}
