package com.aitorgf.threekt.extras.curves

import com.aitorgf.threekt.extras.core.Curve
import com.aitorgf.threekt.math.Vector2
import kotlin.js.JsExport

/** A straight 2D segment from [v1] to [v2], as Three.js `LineCurve`. The points are retained. */
@JsExport
class LineCurve(var v1: Vector2 = Vector2(), var v2: Vector2 = Vector2()) : Curve<Vector2>() {
    override fun createPoint(): Vector2 = Vector2()

    override fun getPoint(t: Double, optionalTarget: Vector2?): Vector2 {
        val point = optionalTarget ?: Vector2()
        if (t == 1.0) {
            point.copy(v2)
        } else {
            point.copy(v2).sub(v1)
            point.multiplyScalar(t).add(v1)
        }
        return point
    }

    /** A line is uniform in arc length, so this equals [getPoint]. */
    override fun getPointAt(u: Double, optionalTarget: Vector2?): Vector2 = getPoint(u, optionalTarget)

    override fun getTangent(t: Double, optionalTarget: Vector2?): Vector2 =
        (optionalTarget ?: Vector2()).subVectors(v2, v1).normalize()

    override fun getTangentAt(u: Double, optionalTarget: Vector2?): Vector2 = getTangent(u, optionalTarget)
}
