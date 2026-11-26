package com.aitorgf.threekt.curves

import com.aitorgf.threekt.extras.core.Curve
import com.aitorgf.threekt.math.Vector3
import kotlin.js.JsExport
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** A trefoil knot. From the Three.js `CurveExtras` addon. */
@JsExport
class TrefoilKnot(
    /** Factor applied to the curve. */
    var scale: Double = 10.0,
) : Curve<Vector3>() {
    override fun createPoint(): Vector3 = Vector3()

    override fun getPoint(t: Double, optionalTarget: Vector3?): Vector3 {
        val point = optionalTarget ?: Vector3()
        val a = t * PI * 2
        val x = (2 + cos(3 * a)) * cos(2 * a)
        val y = (2 + cos(3 * a)) * sin(2 * a)
        val z = sin(3 * a)
        return point.set(x, y, z).multiplyScalar(scale)
    }
}
