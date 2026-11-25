package com.aitorgf.threekt.math

import kotlin.math.*
import kotlin.random.Random

/** Scalar utilities for interpolation, angle conversion, and random sampling. */
object MathUtils {
    const val DEG2RAD = PI / 180.0
    const val RAD2DEG = 180.0 / PI

    /** Clamps the given value between min and max. */
    fun clamp(value: Double, min: Double, max: Double): Double = maxOf(min, minOf(max, value))

    /** Computes the Euclidean modulo of the given parameters that is `( ( n % m ) + m ) % m`. */
    fun euclideanModulo(n: Double, m: Double): Double = ((n % m) + m) % m

    /** Performs a linear mapping from range `<a1, a2>` to range `<b1, b2>` for the given value. */
    fun mapLinear(x: Double, a1: Double, a2: Double, b1: Double, b2: Double): Double =
        b1 + (x - a1) * (b2 - b1) / (a2 - a1)

    /**
     * Returns the percentage in the closed interval `[0, 1]` of the given value between the start and end
     * point.
     */
    fun inverseLerp(x: Double, y: Double, value: Double): Double = if (x == y) 0.0 else (value - x) / (y - x)

    /**
     * Returns a value linearly interpolated from two known points based on the given interval - `t = 0`
     * will return `x` and `t = 1` will return `y`.
     */
    fun lerp(x: Double, y: Double, t: Double): Double = (1.0 - t) * x + t * y

    /**
     * Smoothly interpolate a number from `x` to `y` in a spring-like manner using a delta time to maintain
     * frame rate independent movement. For details, see [Frame rate independent damping using
     * lerp](http://www.rorydriscoll.com/2016/03/07/frame-rate-independent-damping-using-lerp/). and a
     * lower value will make the movement more gradual.
     */
    fun damp(x: Double, y: Double, lambda: Double, dt: Double): Double = lerp(x, y, 1.0 - exp(-lambda * dt))

    /** Returns a value that alternates between `0` and the given `length` parameter. */
    fun pingpong(x: Double, length: Double = 1.0): Double = length - abs(euclideanModulo(x, length * 2) - length)

    /**
     * Returns a value in the range `[0,1]` that represents the percentage that `x` has moved between `min`
     * and `max`, but smoothed or slowed down the closer `x` is to the `min` and `max`. See
     * [Smoothstep](http://en.wikipedia.org/wiki/Smoothstep) for more details.
     */
    fun smoothstep(x: Double, min: Double, max: Double): Double {
        val t = if (x <= min) return 0.0 else if (x >= max) return 1.0 else (x - min) / (max - min)
        return t * t * (3.0 - 2.0 * t)
    }

    /**
     * A [variation on smoothstep](https://en.wikipedia.org/wiki/Smoothstep#Variations) that has zero 1st
     * and 2nd order derivatives at x=0 and x=1.
     */
    fun smootherstep(x: Double, min: Double, max: Double): Double {
        val t = if (x <= min) return 0.0 else if (x >= max) return 1.0 else (x - min) / (max - min)
        return t * t * t * (t * (t * 6.0 - 15.0) + 10.0)
    }

    /** Samples an integer between [low] and [high], both inclusive, using [random]. */
    fun randInt(low: Int, high: Int, random: Random = Random.Default): Int =
        random.nextLong(low.toLong(), high.toLong() + 1).toInt()

    /** Samples a value from [low] inclusive to [high] exclusive using [random]. */
    fun randFloat(low: Double, high: Double, random: Random = Random.Default): Double =
        low + random.nextDouble() * (high - low)

    /** Samples a value in an interval of width [range] centered on zero, using [random]. */
    fun randFloatSpread(range: Double, random: Random = Random.Default): Double =
        range * (0.5 - random.nextDouble())

    /** Converts degrees to radians. */
    fun degToRad(degrees: Double): Double = degrees * DEG2RAD

    /** Converts radians to degrees. */
    fun radToDeg(radians: Double): Double = radians * RAD2DEG

    /** Returns `true` if the given number is a power of two. */
    fun isPowerOfTwo(value: Int): Boolean = value != 0 && (value and (value - 1)) == 0

    /** Returns the smallest power of two that is greater than or equal to the given number. */
    fun ceilPowerOfTwo(value: Double): Double = 2.0.pow(ceil(log2(value)))

    /** Returns the largest power of two that is less than or equal to the given number. */
    fun floorPowerOfTwo(value: Double): Double = 2.0.pow(floor(log2(value)))

    /** Axis sequences of proper Euler rotations, which repeat the first axis. */
    enum class ProperEulerOrder { XYX, YZY, ZXZ, XZX, YXY, ZYZ }

    /**
     * Sets [q] from three angles in radians, in sequence [a], [b], [c], about the axes specified by
     * [order].
     */
    fun setQuaternionFromProperEuler(q: Quaternion, a: Double, b: Double, c: Double, order: ProperEulerOrder) {
        val cb = cos(b / 2)
        val sb = sin(b / 2)
        val sum = (a + c) / 2
        val difference = (a - c) / 2
        val axis = cb * sin(sum)
        val middle = sb * cos(difference)
        val cross = sb * sin(difference)
        val w = cb * cos(sum)
        when (order) {
            ProperEulerOrder.XYX -> q.set(axis, middle, cross, w)
            ProperEulerOrder.YZY -> q.set(cross, axis, middle, w)
            ProperEulerOrder.ZXZ -> q.set(middle, cross, axis, w)
            ProperEulerOrder.XZX -> q.set(axis, -cross, middle, w)
            ProperEulerOrder.YXY -> q.set(middle, axis, -cross, w)
            ProperEulerOrder.ZYZ -> q.set(-cross, middle, axis, w)
        }
    }

    /** Computes the determinant of a 3×3 matrix stored in column-major order. */
    internal fun determinant3(m: DoubleArray): Double =
        m[0] * (m[4] * m[8] - m[7] * m[5]) +
            m[3] * (m[7] * m[2] - m[1] * m[8]) +
            m[6] * (m[1] * m[5] - m[4] * m[2])
}
