package com.aitorgf.threekt.extras.core

import com.aitorgf.threekt.math.MutableVector
import kotlin.js.JsExport
import kotlin.math.floor
import kotlin.math.sqrt

/**
 * Base class for parametric curves of 2D or 3D points [T], as Three.js `Curve`. Subclasses implement
 * [getPoint] for `t ∈ [0, 1]`; the base class adds arc-length parameterization through [getPointAt],
 * sampling, lengths and tangents. Arc lengths are cached; call [updateArcLengths] after changing the
 * curve.
 *
 * Three.js returns arrays of points. Threekt returns [List]s, because arrays of a generic point type cannot
 * be created on every platform; from JavaScript call `asJsReadonlyArrayView()` on the result.
 * `computeFrenetFrames()`, cloning and JSON serialization are not supported yet.
 */
@JsExport
abstract class Curve<T : MutableVector<T>> {
    /** Number of samples used to approximate the arc length. */
    var arcLengthDivisions = 200

    /** Forces the next [getLengths] call to recompute the cached arc lengths. */
    var needsUpdate = false

    private var cacheArcLengths: DoubleArray? = null

    /** Creates a zero point of this curve's dimension. */
    internal abstract fun createPoint(): T

    /** Returns the point at parameter [t] in `[0, 1]`, written into [optionalTarget] when given. */
    abstract fun getPoint(t: Double, optionalTarget: T? = null): T

    /** Returns the point at the fraction [u] of the arc length, written into [optionalTarget] when given. */
    open fun getPointAt(u: Double, optionalTarget: T? = null): T = getPoint(getUtoTmapping(u), optionalTarget)

    /**
     * Default divisions of [getPoints] and [getSpacedPoints]: 5 for single curves, and 12 and 40 for
     * [CurvePath], as in Three.js, where overrides change the default arguments.
     */
    internal open val defaultPointDivisions: Int get() = 5
    internal open val defaultSpacedDivisions: Int get() = 5

    /** Returns [divisions] + 1 points at equal steps of the parameter `t`. */
    open fun getPoints(divisions: Int = defaultPointDivisions): List<T> = List(divisions + 1) { getPoint(it.toDouble() / divisions) }

    /** Returns [divisions] + 1 points at equal steps of arc length. */
    open fun getSpacedPoints(divisions: Int = defaultSpacedDivisions): List<T> = List(divisions + 1) { getPointAt(it.toDouble() / divisions) }

    /** Returns the approximate arc length of the curve. */
    open fun getLength(): Double {
        val lengths = getLengths()
        return lengths[lengths.size - 1]
    }

    /**
     * Returns the cumulative arc lengths at [divisions] + 1 equally spaced parameter values. The returned
     * array is cached and must not be modified.
     */
    open fun getLengths(divisions: Int = arcLengthDivisions): DoubleArray {
        val cached = cacheArcLengths
        if (cached != null && cached.size == divisions + 1 && !needsUpdate) return cached
        needsUpdate = false
        val cache = DoubleArray(divisions + 1)
        var last = getPoint(0.0)
        var sum = 0.0
        for (p in 1..divisions) {
            val current = getPoint(p.toDouble() / divisions)
            sum += distance(current, last)
            cache[p] = sum
            last = current
        }
        cacheArcLengths = cache
        return cache
    }

    /** Recomputes the cached arc lengths. */
    open fun updateArcLengths() {
        needsUpdate = true
        getLengths()
    }

    /**
     * Maps the arc-length fraction [u] to the curve parameter `t`, or maps [distance] along the curve when it
     * is given and not zero.
     */
    fun getUtoTmapping(u: Double, distance: Double? = null): Double {
        val arcLengths = getLengths()
        val il = arcLengths.size
        val targetArcLength = if (distance != null && distance != 0.0) distance else u * arcLengths[il - 1]
        var low = 0
        var high = il - 1
        while (low <= high) {
            val i = floor(low + (high - low) / 2.0).toInt()
            val comparison = arcLengths[i] - targetArcLength
            if (comparison < 0) {
                low = i + 1
            } else if (comparison > 0) {
                high = i - 1
            } else {
                high = i
                break
            }
        }
        val i = high
        if (arcLengths[i] == targetArcLength) return i.toDouble() / (il - 1)
        val lengthBefore = arcLengths[i]
        val lengthAfter = arcLengths[i + 1]
        val segmentFraction = (targetArcLength - lengthBefore) / (lengthAfter - lengthBefore)
        return (i + segmentFraction) / (il - 1)
    }

    /** Returns the unit tangent at parameter [t], estimated from two nearby points. */
    open fun getTangent(t: Double, optionalTarget: T? = null): T {
        val delta = 0.0001
        val pt1 = getPoint((t - delta).coerceAtLeast(0.0))
        val pt2 = getPoint((t + delta).coerceAtMost(1.0))
        val tangent = optionalTarget ?: createPoint()
        for (i in 0 until tangent.dimension) tangent[i] = pt2[i] - pt1[i]
        return tangent.normalize()
    }

    /** Returns the unit tangent at the arc-length fraction [u]. */
    open fun getTangentAt(u: Double, optionalTarget: T? = null): T = getTangent(getUtoTmapping(u), optionalTarget)

    internal companion object {
        fun <T : MutableVector<T>> distance(a: T, b: T): Double {
            var sum = 0.0
            for (i in 0 until a.dimension) {
                val d = a[i] - b[i]
                sum += d * d
            }
            return sqrt(sum)
        }

        fun <T : MutableVector<T>> copyInto(target: T, source: T): T {
            for (i in 0 until target.dimension) target[i] = source[i]
            return target
        }
    }
}
