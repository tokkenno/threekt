package com.aitorgf.threekt.curves

import com.aitorgf.threekt.extras.core.Curve
import com.aitorgf.threekt.math.Vector3
import kotlin.js.JsExport
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** A decorated torus knot, variant 5c. From the Three.js `CurveExtras` addon. */
@JsExport
class DecoratedTorusKnot5c(
    /** Factor applied to the curve. */
    var scale: Double = 40.0,
) : Curve<Vector3>() {
    override fun createPoint(): Vector3 = Vector3()

    override fun getPoint(t: Double, optionalTarget: Vector3?): Vector3 {
        val point = optionalTarget ?: Vector3()
        val fi = t * PI * 2
        val x = cos(4 * fi) * (1 + 0.5 * (cos(5 * fi) + 0.4 * cos(20 * fi)))
        val y = sin(4 * fi) * (1 + 0.5 * (cos(5 * fi) + 0.4 * cos(20 * fi)))
        val z = 0.35 * sin(15 * fi)
        return point.set(x, y, z).multiplyScalar(scale)
    }
}
