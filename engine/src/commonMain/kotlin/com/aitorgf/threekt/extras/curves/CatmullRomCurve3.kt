package com.aitorgf.threekt.extras.curves

import com.aitorgf.threekt.math.Vector3
import kotlin.js.JsExport
import kotlin.math.floor
import kotlin.math.sqrt

/** Centripetal Catmull–Rom curve through the retained [points]. */
@JsExport
class CatmullRomCurve3(val points: Array<Vector3>, var closed: Boolean = false) {
    init {
        require(points.size >= 2)
    }

    private val first = Vector3()
    private val last = Vector3()

    /** Samples normalized parameter [t] in [0, 1], writing the result to [target]. */
    fun getPoint(t: Double, target: Vector3 = Vector3()): Vector3 {
        require(t in 0.0..1.0)
        val length = points.size
        val p = (if (closed) length else length - 1) * t
        var index = floor(p).toInt()
        var weight = p - index
        if (!closed && index == length - 1) {
            index--
            weight = 1.0
        }

        val p1 = points[index % length]
        val p2 = points[(index + 1) % length]
        val p0 = if (closed || index > 0) points[(index + length - 1) % length] else first.subVectors(p1, p2).add(p1)
        val p3 = if (closed || index + 2 < length) points[(index + 2) % length] else last.subVectors(p2, p1).add(p2)

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
            val squared = weight * weight
            return x1 + t1 * weight + c2 * squared + c3 * squared * weight
        }

        return target.set(
            interpolate(p0.x, p1.x, p2.x, p3.x),
            interpolate(p0.y, p1.y, p2.y, p3.y),
            interpolate(p0.z, p1.z, p2.z, p3.z),
        )
    }
}
