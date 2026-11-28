package com.aitorgf.threekt.extras.curves

import com.aitorgf.threekt.extras.core.Curve
import com.aitorgf.threekt.math.Vector3
import com.aitorgf.threekt.math.Vector4
import kotlin.js.JsExport

/** A rational B-spline curve whose control points store their weight in w. */
@JsExport
class NURBSCurve(
    val degree: Int,
    val knots: DoubleArray,
    controlPoints: Array<Vector4>,
    val startKnot: Int = 0,
    val endKnot: Int = knots.lastIndex,
) : Curve<Vector3>() {
    val controlPoints = Array(controlPoints.size) { controlPoints[it].copy() }

    internal override fun createPoint(): Vector3 = Vector3()

    /** Evaluates t between the start and end knots and writes into the optional target. */
    override fun getPoint(t: Double, optionalTarget: Vector3?): Vector3 {
        val u = knots[startKnot] + t * (knots[endKnot] - knots[startKnot])
        val point = NURBSUtils.calcBSplinePoint(degree, knots, controlPoints, u)
        return (optionalTarget ?: Vector3()).set(point.x / point.w, point.y / point.w, point.z / point.w)
    }

    /** Writes the normalized rational derivative into the optional target. */
    override fun getTangent(t: Double, optionalTarget: Vector3?): Vector3 {
        val u = knots.first() + t * (knots.last() - knots.first())
        return NURBSUtils.calcCurveTangent(degree, knots, controlPoints, u, optionalTarget ?: Vector3())
    }
}
