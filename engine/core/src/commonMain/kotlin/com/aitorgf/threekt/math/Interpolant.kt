package com.aitorgf.threekt.math

import kotlin.js.JsExport

/**
 * Base class for sampling keyframes. Times must be finite and sorted, and each sample contains [valueSize]
 * consecutive values.
 */

@JsExport
abstract class Interpolant(
    val parameterPositions: DoubleArray,
    val sampleValues: DoubleArray,
    sampleSize: Int,
    resultBuffer: DoubleArray = DoubleArray(sampleSize),
) {
    /** Boundary condition used to determine the tangent behavior of a cubic interpolant. */
    enum class Ending { ZeroCurvature, ZeroSlope, WrapAround }

    /** Endpoint conditions for cubic interpolation. */
    data class Settings(
        val endingStart: Ending = Ending.ZeroCurvature,
        val endingEnd: Ending = Ending.ZeroCurvature,
    )

    val valueSize = sampleSize

    /** Reusable output storage with room for at least [valueSize] components. */
    var resultBuffer: DoubleArray = resultBuffer
        set(value) {
            require(value.size >= valueSize)
            field = value
        }

    private var cachedIndex = 0

    init {
        require(resultBuffer.size >= valueSize)
        require(parameterPositions.isNotEmpty() && valueSize > 0)
        require(sampleValues.size.toLong() == parameterPositions.size.toLong() * valueSize)
        require(parameterPositions[0].isFinite())
        for (index in 1 until parameterPositions.size) {
            require(parameterPositions[index].isFinite() && parameterPositions[index - 1] <= parameterPositions[index]) {
                "Sample times must be finite and sorted."
            }
        }
    }

    /** Discards the cached interval so its state is rebuilt on the next interior evaluation. */
    protected fun invalidateInterval() {
        cachedIndex = 0
    }

    /**
     * Samples the track at [t], clamping to the nearest endpoint outside the keyframe range. Returns the
     * reused [resultBuffer], whose contents are overwritten by subsequent evaluations.
     */
    fun evaluate(t: Double): DoubleArray {
        require(!t.isNaN()) { "The sample time must be a number." }
        val positions = parameterPositions
        if (t < positions.first()) return copySample(0)
        if (t >= positions.last()) return copySample(positions.lastIndex)

        val previous = cachedIndex
        var index = previous
        if (index == 0 || t < positions[index - 1] || t >= positions[index]) {
            var low = 1
            var high = positions.lastIndex
            while (low < high) {
                val mid = low + (high - low) / 2
                if (t < positions[mid]) high = mid else low = mid + 1
            }
            index = low
        }
        val t0 = positions[index - 1]
        val t1 = positions[index]
        if (index != previous) {
            cachedIndex = index
            onIntervalChanged(index, t0, t1)
        }
        return interpolate(index, t0, t, t1)
    }

    /** Copies the sample at [index] into the result buffer and returns it. */
    protected fun copySample(index: Int): DoubleArray {
        val offset = index * valueSize
        return sampleValues.copyInto(resultBuffer, startIndex = offset, endIndex = offset + valueSize)
    }

    /** Updates subclass state after entering an interval ending at sample [i1]. */
    protected open fun onIntervalChanged(i1: Int, t0: Double, t1: Double) = Unit

    /** Writes the value at [t] into the result buffer for the interval from [t0] to [t1]. */
    protected abstract fun interpolate(i1: Int, t0: Double, t: Double, t1: Double): DoubleArray

    /** Blends the samples on either side of [i1] using [weight] for the upper sample. */
    protected fun interpolateLinearly(i1: Int, weight: Double): DoubleArray {
        val upper = i1 * valueSize
        val lower = upper - valueSize
        for (index in 0 until valueSize) {
            resultBuffer[index] = MathUtils.lerp(sampleValues[lower + index], sampleValues[upper + index], weight)
        }
        return resultBuffer
    }
}
