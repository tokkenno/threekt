package com.aitorgf.threekt.extras.curves

import com.aitorgf.threekt.extras.core.Curve
import com.aitorgf.threekt.extras.core.Interpolations
import com.aitorgf.threekt.math.Vector3
import kotlin.js.JsExport

/** A 3D cubic Bézier curve with control points [v1] and [v2], as Three.js `CubicBezierCurve3`. */
@JsExport
class CubicBezierCurve3(
    var v0: Vector3 = Vector3(),
    var v1: Vector3 = Vector3(),
    var v2: Vector3 = Vector3(),
    var v3: Vector3 = Vector3(),
) : Curve<Vector3>() {
    override fun createPoint(): Vector3 = Vector3()

    override fun getPoint(t: Double, optionalTarget: Vector3?): Vector3 = (optionalTarget ?: Vector3()).set(
        Interpolations.cubicBezier(t, v0.x, v1.x, v2.x, v3.x),
        Interpolations.cubicBezier(t, v0.y, v1.y, v2.y, v3.y),
        Interpolations.cubicBezier(t, v0.z, v1.z, v2.z, v3.z),
    )
}
