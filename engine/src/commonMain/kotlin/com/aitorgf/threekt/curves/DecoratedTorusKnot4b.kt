package com.aitorgf.threekt.curves

import com.aitorgf.threekt.extras.core.Curve
import com.aitorgf.threekt.math.Vector3
import kotlin.js.JsExport
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** A decorated torus knot, variant 4b. From the Three.js `CurveExtras` addon. */
@JsExport
class DecoratedTorusKnot4b(
    /** Factor applied to the curve. */
    var scale: Double = 40.0,
) : Curve<Vector3>() {
    override fun createPoint(): Vector3 = Vector3()

    override fun getPoint(t: Double, optionalTarget: Vector3?): Vector3 {
        val point = optionalTarget ?: Vector3()
        val fi = t * PI * 2
        val x = cos(2 * fi) * (1 + 0.45 * cos(3 * fi) + 0.4 * cos(9 * fi))
        val y = sin(2 * fi) * (1 + 0.45 * cos(3 * fi) + 0.4 * cos(9 * fi))
        val z = 0.2 * sin(9 * fi)
        return point.set(x, y, z).multiplyScalar(scale)
    }
}
