package com.aitorgf.threekt.extras.curves

import com.aitorgf.threekt.extras.core.Curve
import com.aitorgf.threekt.math.Vector3
import kotlin.js.JsExport

/** A straight 3D segment from [v1] to [v2], as Three.js `LineCurve3`. The points are retained. */
@JsExport
class LineCurve3(var v1: Vector3 = Vector3(), var v2: Vector3 = Vector3()) : Curve<Vector3>() {
    override fun createPoint(): Vector3 = Vector3()

    override fun getPoint(t: Double, optionalTarget: Vector3?): Vector3 {
        val point = optionalTarget ?: Vector3()
        if (t == 1.0) {
            point.copy(v2)
        } else {
            point.copy(v2).sub(v1)
            point.multiplyScalar(t).add(v1)
        }
        return point
    }

    /** A line is uniform in arc length, so this equals [getPoint]. */
    override fun getPointAt(u: Double, optionalTarget: Vector3?): Vector3 = getPoint(u, optionalTarget)

    override fun getTangent(t: Double, optionalTarget: Vector3?): Vector3 =
        (optionalTarget ?: Vector3()).subVectors(v2, v1).normalize()

    override fun getTangentAt(u: Double, optionalTarget: Vector3?): Vector3 = getTangent(u, optionalTarget)
}
