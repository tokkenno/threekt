package com.aitorgf.threekt.extras.curves

import com.aitorgf.threekt.extras.core.Curve
import com.aitorgf.threekt.extras.core.Interpolations
import com.aitorgf.threekt.math.Vector3
import kotlin.js.JsExport

/** A 3D quadratic Bézier curve with control point [v1], as Three.js `QuadraticBezierCurve3`. */
@JsExport
class QuadraticBezierCurve3(
    var v0: Vector3 = Vector3(),
    var v1: Vector3 = Vector3(),
    var v2: Vector3 = Vector3(),
) : Curve<Vector3>() {
    override fun createPoint(): Vector3 = Vector3()

    override fun getPoint(t: Double, optionalTarget: Vector3?): Vector3 = (optionalTarget ?: Vector3()).set(
        Interpolations.quadraticBezier(t, v0.x, v1.x, v2.x),
        Interpolations.quadraticBezier(t, v0.y, v1.y, v2.y),
        Interpolations.quadraticBezier(t, v0.z, v1.z, v2.z),
    )
}
