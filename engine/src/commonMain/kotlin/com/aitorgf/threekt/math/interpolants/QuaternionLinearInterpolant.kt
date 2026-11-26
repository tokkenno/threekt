package com.aitorgf.threekt.math.interpolants

import com.aitorgf.threekt.math.Interpolant
import com.aitorgf.threekt.math.Quaternion

import kotlin.js.JsExport

/** Interpolates each group of four components as a quaternion using spherical linear interpolation. */

@JsExport
class QuaternionLinearInterpolant(
    parameterPositions: DoubleArray, sampleValues: DoubleArray, sampleSize: Int,
    resultBuffer: DoubleArray = DoubleArray(sampleSize),
) : Interpolant(parameterPositions, sampleValues, sampleSize, resultBuffer) {
    private val start = Quaternion()
    private val end = Quaternion()

    init {
        require(sampleSize % 4 == 0) { "Quaternion samples must have four components per rotation." }
    }

    /** Interpolates the active interval's rotations at [t] into the result buffer. */
    override fun interpolate(i1: Int, t0: Double, t: Double, t1: Double): DoubleArray {
        val alpha = (t - t0) / (t1 - t0)
        val upper = i1 * valueSize
        val lower = upper - valueSize
        for (index in 0 until valueSize step 4) {
            start.fromArray(sampleValues, lower + index)
            end.fromArray(sampleValues, upper + index)
            if (start != end) start.slerp(end, alpha)
            start.toArray(resultBuffer, index)
        }
        return resultBuffer
    }
}
