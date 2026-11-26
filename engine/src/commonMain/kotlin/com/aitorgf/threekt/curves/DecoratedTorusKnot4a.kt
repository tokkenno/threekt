package com.aitorgf.threekt.curves

import com.aitorgf.threekt.extras.core.Curve
import com.aitorgf.threekt.math.Vector3
import kotlin.js.JsExport
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** A decorated torus knot, variant 4a. From the Three.js `CurveExtras` addon. */
@JsExport
class DecoratedTorusKnot4a(
    /** Factor applied to the curve. */
    var scale: Double = 40.0,
) : Curve<Vector3>() {
    override fun createPoint(): Vector3 = Vector3()

    override fun getPoint(t: Double, optionalTarget: Vector3?): Vector3 {
        val point = optionalTarget ?: Vector3()
        val a = t * PI * 2
        val x = cos(2 * a) * (1 + 0.6 * (cos(5 * a) + 0.75 * cos(10 * a)))
        val y = sin(2 * a) * (1 + 0.6 * (cos(5 * a) + 0.75 * cos(10 * a)))
        val z = 0.35 * sin(5 * a)
        return point.set(x, y, z).multiplyScalar(scale)
    }
}
