package com.aitorgf.threekt.curves

import com.aitorgf.threekt.extras.core.Curve
import com.aitorgf.threekt.math.Vector3
import kotlin.js.JsExport
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** A knot that winds around a torus. From the Three.js `CurveExtras` addon. */
@JsExport
class KnotCurve() : Curve<Vector3>() {
    override fun createPoint(): Vector3 = Vector3()

    override fun getPoint(t: Double, optionalTarget: Vector3?): Vector3 {
        val point = optionalTarget ?: Vector3()
        val a = t * 2 * PI
        val r = 10.0
        val s = 50.0
        val x = s * sin(a)
        val y = cos(a) * (r + s * cos(a))
        val z = sin(a) * (r + s * cos(a))
        return point.set(x, y, z)
    }
}
