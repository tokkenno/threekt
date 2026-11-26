package com.aitorgf.threekt.extras.curves

import com.aitorgf.threekt.math.Vector3
import com.aitorgf.threekt.math.Vector4
import kotlin.js.JsExport

/** A two-parameter rational B-spline surface whose control points store their weight in w. */
@JsExport
class NURBSSurface(
    val degree1: Int, val degree2: Int,
    val knots1: DoubleArray, val knots2: DoubleArray,
    controlPoints: Array<Array<Vector4>>,
) {
    val controlPoints = Array(controlPoints.size) { i -> Array(controlPoints[i].size) { j -> controlPoints[i][j].copy() } }

    /** Evaluates normalized parameters and writes the result into [target]. */
    fun getPoint(t1: Double, t2: Double, target: Vector3) {
        val u = knots1.first() + t1 * (knots1.last() - knots1.first())
        val v = knots2.first() + t2 * (knots2.last() - knots2.first())
        NURBSUtils.calcSurfacePoint(degree1, degree2, knots1, knots2, controlPoints, u, v, target)
    }
}
