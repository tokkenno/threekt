package com.aitorgf.threekt.extras.core

import com.aitorgf.threekt.extras.curves.EllipseCurve
import com.aitorgf.threekt.extras.curves.LineCurve
import com.aitorgf.threekt.extras.curves.LineCurve3
import com.aitorgf.threekt.extras.curves.SplineCurve
import com.aitorgf.threekt.math.MutableVector
import com.aitorgf.threekt.math.Vector2
import com.aitorgf.threekt.math.Vector3
import kotlin.js.JsExport

/**
 * A sequence of connected [curves] treated as one curve, as Three.js `CurvePath`. With [autoClose], the
 * sampled points end at the first point. Where Three.js returns null for a parameter beyond the total length, Threekt returns
 * the end of the last curve.
 */
@JsExport
open class CurvePath<T : MutableVector<T>> : Curve<T>() {
    /** The curves in order. */
    val curves: MutableList<Curve<T>> = mutableListOf()

    /** Whether [getPoints] and [getSpacedPoints] repeat the first point at the end. */
    var autoClose = false

    private var cacheLengths: DoubleArray? = null

    override fun createPoint(): T = curves.first().createPoint()

    override val defaultPointDivisions: Int get() = 12
    override val defaultSpacedDivisions: Int get() = 40

    /** Appends [curve]. */
    fun add(curve: Curve<T>) {
        curves.add(curve)
    }

    /** Adds a line from the end of the last curve to the start of the first one if they differ. */
    @Suppress("UNCHECKED_CAST")
    fun closePath(): CurvePath<T> {
        val startPoint = curves[0].getPoint(0.0)
        val endPoint = curves[curves.size - 1].getPoint(1.0)
        if (startPoint != endPoint) {
            val line: Curve<*> = if (startPoint is Vector2) LineCurve(endPoint as Vector2, startPoint)
            else LineCurve3(endPoint as Vector3, startPoint as Vector3)
            curves.add(line as Curve<T>)
        }
        return this
    }

    override fun getPoint(t: Double, optionalTarget: T?): T {
        val d = t * getLength()
        val curveLengths = getCurveLengths()
        for (i in curveLengths.indices) {
            if (curveLengths[i] >= d) {
                val diff = curveLengths[i] - d
                val curve = curves[i]
                val segmentLength = curve.getLength()
                val u = if (segmentLength == 0.0) 0.0 else 1 - diff / segmentLength
                return curve.getPointAt(u, optionalTarget)
            }
        }
        return curves.last().getPoint(1.0, optionalTarget)
    }

    override fun getLength(): Double {
        val lengths = getCurveLengths()
        return lengths[lengths.size - 1]
    }

    override fun updateArcLengths() {
        needsUpdate = true
        cacheLengths = null
        getCurveLengths()
    }

    /** Returns the cumulative length at the end of each curve. The returned array is cached. */
    fun getCurveLengths(): DoubleArray {
        val cached = cacheLengths
        if (cached != null && cached.size == curves.size) return cached
        var sums = 0.0
        val lengths = DoubleArray(curves.size) { i ->
            sums += curves[i].getLength()
            sums
        }
        cacheLengths = lengths
        return lengths
    }

    override fun getSpacedPoints(divisions: Int): List<T> {
        val points = MutableList(divisions + 1) { getPoint(it.toDouble() / divisions) }
        if (autoClose) points.add(points[0])
        return points
    }

    /**
     * Samples each curve with a resolution that suits its type: one segment for lines, twice [divisions]
     * for ellipses, and [divisions] per control point for splines. Consecutive duplicates are skipped.
     */
    override fun getPoints(divisions: Int): List<T> {
        val points = mutableListOf<T>()
        var last: T? = null
        for (curve in curves) {
            val resolution = when (curve) {
                is EllipseCurve -> divisions * 2
                is LineCurve, is LineCurve3 -> 1
                is SplineCurve -> divisions * curve.points.size
                else -> divisions
            }
            for (point in curve.getPoints(resolution)) {
                if (last != null && last == point) continue
                points.add(point)
                last = point
            }
        }
        if (autoClose && points.size > 1 && points[points.size - 1] != points[0]) points.add(points[0])
        return points
    }
}
