package com.aitorgf.threekt.extras.curves

import com.aitorgf.threekt.math.Vector3
import com.aitorgf.threekt.math.Vector4
import kotlin.js.JsExport

/** A three-parameter rational B-spline volume whose control points store their weight in w. */
@JsExport
class NURBSVolume(
    val degree1: Int, val degree2: Int, val degree3: Int,
    val knots1: DoubleArray, val knots2: DoubleArray, val knots3: DoubleArray,
    controlPoints: Array<Array<Array<Vector4>>>,
) {
    val controlPoints = Array(controlPoints.size) { i ->
        Array(controlPoints[i].size) { j -> Array(controlPoints[i][j].size) { k -> controlPoints[i][j][k].copy() } }
    }

    /** Evaluates normalized parameters and writes the result into [target]. */
    fun getPoint(t1: Double, t2: Double, t3: Double, target: Vector3) {
        val u = knots1.first() + t1 * (knots1.last() - knots1.first())
        val v = knots2.first() + t2 * (knots2.last() - knots2.first())
        val w = knots3.first() + t3 * (knots3.last() - knots3.first())
        NURBSUtils.calcVolumePoint(degree1, degree2, degree3, knots1, knots2, knots3, controlPoints, u, v, w, target)
    }
}
