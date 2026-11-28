package com.aitorgf.threekt.curves

import com.aitorgf.threekt.extras.core.Curve
import com.aitorgf.threekt.math.Vector3
import kotlin.js.JsExport
import kotlin.math.PI
import kotlin.math.pow

/** A trefoil knot from polynomials. From the Three.js `CurveExtras` addon. */
@JsExport
class TrefoilPolynomialKnot(
    /** Factor applied to the curve. */
    var scale: Double = 10.0,
) : Curve<Vector3>() {
    override fun createPoint(): Vector3 = Vector3()

    override fun getPoint(t: Double, optionalTarget: Vector3?): Vector3 {
        val point = optionalTarget ?: Vector3()
        val a = t * 4 - 2
        val x = a.pow(3) - 3 * a
        val y = a.pow(4) - 4 * a * a
        val z = 1.0 / 5 * a.pow(5) - 2 * a
        return point.set(x, y, z).multiplyScalar(scale)
    }
}
