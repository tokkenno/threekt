package com.aitorgf.threekt.curves

import com.aitorgf.threekt.extras.core.Curve
import com.aitorgf.threekt.math.Vector3
import kotlin.js.JsExport
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** A granny knot. From the Three.js `CurveExtras` addon. */
@JsExport
class GrannyKnot() : Curve<Vector3>() {
    override fun createPoint(): Vector3 = Vector3()

    override fun getPoint(t: Double, optionalTarget: Vector3?): Vector3 {
        val point = optionalTarget ?: Vector3()
        val a = 2 * PI * t
        val x = -0.22 * cos(a) - 1.28 * sin(a) - 0.44 * cos(3 * a) - 0.78 * sin(3 * a)
        val y = -0.1 * cos(2 * a) - 0.27 * sin(2 * a) + 0.38 * cos(4 * a) + 0.46 * sin(4 * a)
        val z = 0.7 * cos(3 * a) - 0.4 * sin(3 * a)
        return point.set(x, y, z).multiplyScalar(20.0)
    }
}
