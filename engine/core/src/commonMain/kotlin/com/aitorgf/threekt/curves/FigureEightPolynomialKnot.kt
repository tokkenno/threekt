package com.aitorgf.threekt.curves

import com.aitorgf.threekt.extras.core.Curve
import com.aitorgf.threekt.math.Vector3
import kotlin.js.JsExport
import kotlin.math.PI
import kotlin.math.pow

/** A figure-eight knot from polynomials. From the Three.js `CurveExtras` addon. */
@JsExport
class FigureEightPolynomialKnot(
    /** Factor applied to the curve. */
    var scale: Double = 1.0,
) : Curve<Vector3>() {
    override fun createPoint(): Vector3 = Vector3()

    override fun getPoint(t: Double, optionalTarget: Vector3?): Vector3 {
        val point = optionalTarget ?: Vector3()
        // Scale t from [0, 1] to [-4, 4].
        val a = t * 8 - 4
        val x = 2.0 / 5 * a * (a * a - 7) * (a * a - 10)
        val y = a.pow(4) - 13 * a * a
        val z = 1.0 / 10 * a * (a * a - 4) * (a * a - 9) * (a * a - 12)
        return point.set(x, y, z).multiplyScalar(scale)
    }
}
