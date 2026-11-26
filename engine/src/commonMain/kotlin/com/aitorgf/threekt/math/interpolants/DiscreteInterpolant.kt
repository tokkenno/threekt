package com.aitorgf.threekt.math.interpolants

import com.aitorgf.threekt.math.Interpolant

import kotlin.js.JsExport

/** Samples a step function that keeps the previous keyframe value until the next keyframe is reached. */

@JsExport
class DiscreteInterpolant(
    parameterPositions: DoubleArray, sampleValues: DoubleArray, sampleSize: Int,
    resultBuffer: DoubleArray = DoubleArray(sampleSize),
) : Interpolant(parameterPositions, sampleValues, sampleSize, resultBuffer) {

    /** Copies the lower keyframe of the active interval into the result buffer. */
    override fun interpolate(i1: Int, t0: Double, t: Double, t1: Double): DoubleArray = copySample(i1 - 1)
}
