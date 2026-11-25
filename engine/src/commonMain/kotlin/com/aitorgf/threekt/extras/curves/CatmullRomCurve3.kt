package com.aitorgf.threekt.extras.curves

import com.aitorgf.threekt.math.Vector3
import kotlin.js.JsExport
import kotlin.math.floor
import kotlin.math.sqrt

/**
 * Open centripetal Catmull–Rom spline. Points are retained by reference and [getPoint] writes to
 * its supplied target. Threekt currently exposes the default open centripetal variant.
 */
@JsExport
class CatmullRomCurve3(val points: Array<Vector3>) {
    private val before = Vector3()
    private val after = Vector3()

    init {
        require(points.size >= 2)
    }

    /** Evaluates the spline at [t] in [0, 1]. */
    fun getPoint(t: Double, target: Vector3 = Vector3()): Vector3 {
        require(t in 0.0..1.0)
        val position = (points.size - 1) * t
        val index = minOf(floor(position).toInt(), points.size - 2)
        val weight = position - index
        val p1 = points[index]
        val p2 = points[index + 1]
        val p0 = if (index > 0) points[index - 1] else before.subVectors(p1, p2).add(p1)
        val p3 = if (index + 2 < points.size) points[index + 2] else after.subVectors(p2, p1).add(p2)
        var dt0 = sqrt(sqrt(p0.distanceToSquared(p1)))
        var dt1 = sqrt(sqrt(p1.distanceToSquared(p2)))
        var dt2 = sqrt(sqrt(p2.distanceToSquared(p3)))
        if (dt1 < 1e-4) dt1 = 1.0
        if (dt0 < 1e-4) dt0 = dt1
        if (dt2 < 1e-4) dt2 = dt1

        fun interpolate(x0: Double, x1: Double, x2: Double, x3: Double): Double {
            val t1 = ((x1 - x0) / dt0 - (x2 - x0) / (dt0 + dt1) + (x2 - x1) / dt1) * dt1
            val t2 = ((x2 - x1) / dt1 - (x3 - x1) / (dt1 + dt2) + (x3 - x2) / dt2) * dt1
            val c2 = -3 * x1 + 3 * x2 - 2 * t1 - t2
            val c3 = 2 * x1 - 2 * x2 + t1 + t2
            return x1 + t1 * weight + c2 * weight * weight + c3 * weight * weight * weight
        }

        return target.set(
            interpolate(p0.x, p1.x, p2.x, p3.x),
            interpolate(p0.y, p1.y, p2.y, p3.y),
            interpolate(p0.z, p1.z, p2.z, p3.z),
        )
    }
}
