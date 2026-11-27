package com.aitorgf.threekt.extras.core

import com.aitorgf.threekt.math.Matrix4
import com.aitorgf.threekt.math.MutableVector
import com.aitorgf.threekt.math.Vector3
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
 * Cloning and JSON serialization are not supported yet.
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

    /** Tangent, normal and binormal of each sample of a 3D curve, as returned by [computeFrenetFrames]. */
    class FrenetFrames(val tangents: Array<Vector3>, val normals: Array<Vector3>, val binormals: Array<Vector3>)

    /**
     * Computes [segments] + 1 parallel-transport frames along a 3D curve at equal arc-length steps, as
     * Three.js does for tubes and path extrusion. The first normal is perpendicular to the tangent's smallest
     * axis; later normals rotate with the tangent. When [closed] is set, the twist is spread so the last frame
     * matches the first. Throws [IllegalArgumentException] for 2D curves.
     */
    fun computeFrenetFrames(segments: Int, closed: Boolean = false): FrenetFrames {
        require(createPoint() is Vector3) { "Frenet frames require a 3D curve." }
        val normal = Vector3()
        val vec = Vector3()
        val mat = Matrix4()
        // Expected tangents of the curve at equal arc-length steps.
        val tangents = Array(segments + 1) { i -> getTangentAt(i.toDouble() / segments) as Vector3 }
        val normals = Array(segments + 1) { Vector3() }
        val binormals = Array(segments + 1) { Vector3() }
        // Select an initial normal vector perpendicular to the first tangent vector, in the direction of the
        // smallest tangent xyz component.
        var min = Double.MAX_VALUE
        val tx = kotlin.math.abs(tangents[0].x)
        val ty = kotlin.math.abs(tangents[0].y)
        val tz = kotlin.math.abs(tangents[0].z)
        if (tx <= min) {
            min = tx
            normal.set(1.0, 0.0, 0.0)
        }
        if (ty <= min) {
            min = ty
            normal.set(0.0, 1.0, 0.0)
        }
        if (tz <= min) normal.set(0.0, 0.0, 1.0)
        vec.crossVectors(tangents[0], normal).normalize()
        normals[0].crossVectors(tangents[0], vec)
        binormals[0].crossVectors(tangents[0], normals[0])
        // Compute the slowly varying normal and binormal vectors for each segment on the curve.
        for (i in 1..segments) {
            normals[i].copy(normals[i - 1])
            binormals[i].copy(binormals[i - 1])
            vec.crossVectors(tangents[i - 1], tangents[i])
            if (vec.length() > EPSILON) {
                vec.normalize()
                val theta = kotlin.math.acos(tangents[i - 1].dot(tangents[i]).coerceIn(-1.0, 1.0))
                normals[i].applyMatrix4(mat.makeRotationAxis(vec, theta))
            }
            binormals[i].crossVectors(tangents[i], normals[i])
        }
        // When the curve is closed, distribute the twist so the last frame matches the first.
        if (closed) {
            var theta = kotlin.math.acos(normals[0].dot(normals[segments]).coerceIn(-1.0, 1.0))
            theta /= segments
            if (tangents[0].dot(vec.crossVectors(normals[0], normals[segments])) > 0) theta = -theta
            for (i in 1..segments) {
                normals[i].applyMatrix4(mat.makeRotationAxis(tangents[i], theta * i))
                binormals[i].crossVectors(tangents[i], normals[i])
            }
        }
        return FrenetFrames(tangents, normals, binormals)
    }

    internal companion object {
        /** JavaScript `Number.EPSILON`. */
        const val EPSILON = 2.220446049250313e-16

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
