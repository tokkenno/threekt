package com.aitorgf.threekt.extras.curves

import com.aitorgf.threekt.extras.core.Curve
import com.aitorgf.threekt.extras.core.Interpolations
import com.aitorgf.threekt.math.Vector2
import kotlin.js.JsExport
import kotlin.math.floor

/** A smooth 2D Catmull–Rom spline through the retained [points], as Three.js `SplineCurve`. */
@JsExport
class SplineCurve(var points: Array<Vector2> = emptyArray()) : Curve<Vector2>() {
    override fun createPoint(): Vector2 = Vector2()

    override fun getPoint(t: Double, optionalTarget: Vector2?): Vector2 {
        val point = optionalTarget ?: Vector2()
        val p = (points.size - 1) * t
        val intPoint = floor(p).toInt()
        val weight = p - intPoint
        val p0 = points[if (intPoint == 0) intPoint else intPoint - 1]
        val p1 = points[intPoint]
        val p2 = points[if (intPoint > points.size - 2) points.size - 1 else intPoint + 1]
        val p3 = points[if (intPoint > points.size - 3) points.size - 1 else intPoint + 2]
        return point.set(
            Interpolations.catmullRom(weight, p0.x, p1.x, p2.x, p3.x),
            Interpolations.catmullRom(weight, p0.y, p1.y, p2.y, p3.y),
        )
    }
}
