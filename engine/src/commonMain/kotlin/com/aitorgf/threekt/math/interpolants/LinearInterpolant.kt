package com.aitorgf.threekt.math.interpolants

import com.aitorgf.threekt.math.Interpolant

import kotlin.js.JsExport

/** Interpolates each component linearly between successive keyframes. */

@JsExport
class LinearInterpolant(
    parameterPositions: DoubleArray, sampleValues: DoubleArray, sampleSize: Int,
    resultBuffer: DoubleArray = DoubleArray(sampleSize),
) : Interpolant(parameterPositions, sampleValues, sampleSize, resultBuffer) {

    /** Blends the active interval's keyframes according to [t] and returns the result buffer. */
    override fun interpolate(i1: Int, t0: Double, t: Double, t1: Double): DoubleArray =
        interpolateLinearly(i1, (t - t0) / (t1 - t0))
}
