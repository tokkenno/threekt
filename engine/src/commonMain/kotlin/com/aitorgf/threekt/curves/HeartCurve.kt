package com.aitorgf.threekt.curves

import com.aitorgf.threekt.extras.core.Curve
import com.aitorgf.threekt.math.Vector3
import kotlin.js.JsExport
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.pow

/** A heart outline in the XY plane. From the Three.js `CurveExtras` addon. */
@JsExport
class HeartCurve(
    /** Factor applied to the curve. */
    var scale: Double = 5.0,
) : Curve<Vector3>() {
    override fun createPoint(): Vector3 = Vector3()

    override fun getPoint(t: Double, optionalTarget: Vector3?): Vector3 {
        val point = optionalTarget ?: Vector3()
        val a = t * 2 * PI
        val x = 16 * sin(a).pow(3)
        val y = 13 * cos(a) - 5 * cos(2 * a) - 2 * cos(3 * a) - cos(4 * a)
        return point.set(x, y, 0.0).multiplyScalar(scale)
    }
}
