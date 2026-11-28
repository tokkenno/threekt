package com.aitorgf.threekt.extras.curves

import com.aitorgf.threekt.extras.core.Curve
import com.aitorgf.threekt.math.Vector3
import kotlin.js.JsExport
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.pow

/**
 * A smooth 3D spline through the retained [points], as Three.js `CatmullRomCurve3`. [curveType] selects the
 * parameterization: centripetal avoids cusps and self-intersections, chordal follows the point spacing more
 * closely, and uniform Catmull–Rom uses [tension]. Three.js names the types with strings; Threekt uses
 * [CurveType].
 */
@JsExport
class CatmullRomCurve3(
    var points: Array<Vector3> = emptyArray(),
    /** Whether the curve loops back from the last point to the first. */
    var closed: Boolean = false,
    var curveType: CurveType = CurveType.Centripetal,
    /** Tension of [CurveType.CatmullRom] curves. */
    var tension: Double = 0.5,
) : Curve<Vector3>() {
    /** Parameterization of the spline between control points. */
    enum class CurveType { Centripetal, Chordal, CatmullRom }

    private val tmp = Vector3()
    private val tmp2 = Vector3()
    private val px = CubicPoly()
    private val py = CubicPoly()
    private val pz = CubicPoly()

    override fun createPoint(): Vector3 = Vector3()

    override fun getPoint(t: Double, optionalTarget: Vector3?): Vector3 {
        val point = optionalTarget ?: Vector3()
        val l = points.size
        val p = (l - if (closed) 0 else 1) * t
        var intPoint = floor(p).toInt()
        var weight = p - intPoint
        if (closed) {
            intPoint += if (intPoint > 0) 0 else (floor(abs(intPoint).toDouble() / l).toInt() + 1) * l
        } else if (weight == 0.0 && intPoint == l - 1) {
            intPoint = l - 2
            weight = 1.0
        }
        val p0 = if (closed || intPoint > 0) points[(intPoint - 1) % l]
        else tmp2.subVectors(points[0], points[1]).add(points[0]) // Extrapolate the first point.
        val p1 = points[intPoint % l]
        val p2 = points[(intPoint + 1) % l]
        val p3 = if (closed || intPoint + 2 < l) points[(intPoint + 2) % l]
        else tmp.subVectors(points[l - 1], points[l - 2]).add(points[l - 1]) // Extrapolate the last point.

        if (curveType == CurveType.Centripetal || curveType == CurveType.Chordal) {
            // Centripetal or chordal Catmull–Rom.
            val pow = if (curveType == CurveType.Chordal) 0.5 else 0.25
            var dt0 = p0.distanceToSquared(p1).pow(pow)
            var dt1 = p1.distanceToSquared(p2).pow(pow)
            var dt2 = p2.distanceToSquared(p3).pow(pow)
            // Safety check for repeated points.
            if (dt1 < 1e-4) dt1 = 1.0
            if (dt0 < 1e-4) dt0 = dt1
            if (dt2 < 1e-4) dt2 = dt1
            px.initNonuniformCatmullRom(p0.x, p1.x, p2.x, p3.x, dt0, dt1, dt2)
            py.initNonuniformCatmullRom(p0.y, p1.y, p2.y, p3.y, dt0, dt1, dt2)
            pz.initNonuniformCatmullRom(p0.z, p1.z, p2.z, p3.z, dt0, dt1, dt2)
        } else {
            px.initCatmullRom(p0.x, p1.x, p2.x, p3.x, tension)
            py.initCatmullRom(p0.y, p1.y, p2.y, p3.y, tension)
            pz.initCatmullRom(p0.z, p1.z, p2.z, p3.z, tension)
        }
        return point.set(px.calc(weight), py.calc(weight), pz.calc(weight))
    }

    /**
     * Cubic polynomial `c0 + c1·s + c2·s² + c3·s³` through two values with given tangents, as the
     * `CubicPoly` helper of Three.js.
     */
    private class CubicPoly {
        private var c0 = 0.0
        private var c1 = 0.0
        private var c2 = 0.0
        private var c3 = 0.0

        private fun init(x0: Double, x1: Double, t0: Double, t1: Double) {
            c0 = x0
            c1 = t0
            c2 = -3 * x0 + 3 * x1 - 2 * t0 - t1
            c3 = 2 * x0 - 2 * x1 + t0 + t1
        }

        fun initCatmullRom(x0: Double, x1: Double, x2: Double, x3: Double, tension: Double) =
            init(x1, x2, tension * (x2 - x0), tension * (x3 - x1))

        fun initNonuniformCatmullRom(x0: Double, x1: Double, x2: Double, x3: Double, dt0: Double, dt1: Double, dt2: Double) {
            // Tangents when parameterized in [t1, t2], rescaled for [0, 1].
            var t1 = (x1 - x0) / dt0 - (x2 - x0) / (dt0 + dt1) + (x2 - x1) / dt1
            var t2 = (x2 - x1) / dt1 - (x3 - x1) / (dt1 + dt2) + (x3 - x2) / dt2
            t1 *= dt1
            t2 *= dt1
            init(x1, x2, t1, t2)
        }

        fun calc(t: Double): Double {
            val t2 = t * t
            val t3 = t2 * t
            return c0 + c1 * t + c2 * t2 + c3 * t3
        }
    }
}
