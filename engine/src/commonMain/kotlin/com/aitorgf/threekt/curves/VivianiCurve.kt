package com.aitorgf.threekt.curves

import com.aitorgf.threekt.extras.core.Curve
import com.aitorgf.threekt.math.Vector3
import kotlin.js.JsExport
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Viviani's curve, the intersection of a sphere and a cylinder. From the Three.js `CurveExtras` addon. */
@JsExport
class VivianiCurve(
    /** Factor applied to the curve. */
    var scale: Double = 70.0,
) : Curve<Vector3>() {
    override fun createPoint(): Vector3 = Vector3()

    override fun getPoint(t: Double, optionalTarget: Vector3?): Vector3 {
        val point = optionalTarget ?: Vector3()
        val a = t * 4 * PI // Normalized to 0..1.
        val r = scale / 2
        val x = r * (1 + cos(a))
        val y = r * sin(a)
        val z = 2 * r * sin(a / 2)
        return point.set(x, y, z)
    }
}
