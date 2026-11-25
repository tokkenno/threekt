package com.aitorgf.threekt.math.interpolants

import com.aitorgf.threekt.math.Interpolant

/**
 * Interpolates keyframes using a cubic spline whose tangents use neighboring samples and the configured
 * endpoint conditions.
 */
class CubicInterpolant(
    parameterPositions: DoubleArray, sampleValues: DoubleArray, sampleSize: Int,
    resultBuffer: DoubleArray = DoubleArray(sampleSize),
) : Interpolant(parameterPositions, sampleValues, sampleSize, resultBuffer) {
    /** Endpoint conditions. Assigning new settings invalidates the cached interval. */
    var settings = Interpolant.Settings()
        set(value) {
            field = value
            invalidateInterval()
        }

    private var previousOffset = 0
    private var nextOffset = 0
    private var previousWeight = 0.0
    private var nextWeight = 0.0

    /** Caches the neighboring sample offsets and weights for the active interval. */
    override fun onIntervalChanged(i1: Int, t0: Double, t1: Double) {
        val positions = parameterPositions
        val (previous, previousTime) = if (i1 > 1) {
            val index = i1 - 2
            index to positions[index]
        } else when (settings.endingStart) {
            Interpolant.Ending.ZeroSlope -> i1 to 2.0 * t0 - t1
            Interpolant.Ending.WrapAround -> positions.lastIndex - 1 to t0 + positions[positions.lastIndex - 1] - positions.last()
            Interpolant.Ending.ZeroCurvature -> i1 to t1
        }
        val (next, nextTime) = if (i1 < positions.lastIndex) {
            val index = i1 + 1
            index to positions[index]
        } else when (settings.endingEnd) {
            Interpolant.Ending.ZeroSlope -> i1 - 1 to 2.0 * t1 - t0
            Interpolant.Ending.WrapAround -> 1 to t1 + positions[1] - positions[0]
            Interpolant.Ending.ZeroCurvature -> i1 - 1 to t0
        }
        val halfInterval = (t1 - t0) * 0.5
        previousOffset = previous * valueSize
        nextOffset = next * valueSize
        previousWeight = halfInterval / (t0 - previousTime)
        nextWeight = halfInterval / (nextTime - t1)
    }

    /** Evaluates the active cubic segment at [t] and returns the result buffer. */
    override fun interpolate(i1: Int, t0: Double, t: Double, t1: Double): DoubleArray {
        val p = (t - t0) / (t1 - t0)
        val square = p * p
        val cube = square * p
        val wp = previousWeight
        val wn = nextWeight
        val sp = -wp * cube + 2.0 * wp * square - wp * p
        val s0 = (1.0 + wp) * cube + (-1.5 - 2.0 * wp) * square + (-0.5 + wp) * p + 1.0
        val s1 = (-1.0 - wn) * cube + (1.5 + wn) * square + 0.5 * p
        val sn = wn * cube - wn * square
        val upper = i1 * valueSize
        val lower = upper - valueSize
        for (index in 0 until valueSize) {
            resultBuffer[index] = sp * sampleValues[previousOffset + index] + s0 * sampleValues[lower + index] +
                s1 * sampleValues[upper + index] + sn * sampleValues[nextOffset + index]
        }
        return resultBuffer
    }
}
