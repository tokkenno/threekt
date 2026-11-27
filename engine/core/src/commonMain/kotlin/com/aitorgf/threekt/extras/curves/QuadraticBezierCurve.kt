package com.aitorgf.threekt.extras.curves

import com.aitorgf.threekt.extras.core.Curve
import com.aitorgf.threekt.extras.core.Interpolations
import com.aitorgf.threekt.math.Vector2
import kotlin.js.JsExport

/** A 2D quadratic Bézier curve with control point [v1], as Three.js `QuadraticBezierCurve`. */
@JsExport
class QuadraticBezierCurve(
    var v0: Vector2 = Vector2(),
    var v1: Vector2 = Vector2(),
    var v2: Vector2 = Vector2(),
) : Curve<Vector2>() {
    override fun createPoint(): Vector2 = Vector2()

    override fun getPoint(t: Double, optionalTarget: Vector2?): Vector2 = (optionalTarget ?: Vector2()).set(
        Interpolations.quadraticBezier(t, v0.x, v1.x, v2.x),
        Interpolations.quadraticBezier(t, v0.y, v1.y, v2.y),
    )
}
