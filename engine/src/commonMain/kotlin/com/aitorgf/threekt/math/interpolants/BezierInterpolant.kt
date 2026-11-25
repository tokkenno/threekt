package com.aitorgf.threekt.math.interpolants

import com.aitorgf.threekt.math.Interpolant
import com.aitorgf.threekt.math.MathUtils
import kotlin.math.abs

/**
 * Samples a cubic Bézier curve using per-component control points stored as time/value pairs in
 * [inTangents] and [outTangents]. Falls back to a linear blend when either tangent array is absent.
 */
class BezierInterpolant(
    parameterPositions: DoubleArray, sampleValues: DoubleArray, sampleSize: Int,
    resultBuffer: DoubleArray = DoubleArray(sampleSize),
) : Interpolant(parameterPositions, sampleValues, sampleSize, resultBuffer) {
    /** Incoming control points as consecutive time/value pairs for each sample component. */
    var inTangents: DoubleArray? = null
    /** Outgoing control points in the same layout as [inTangents]. */
    var outTangents: DoubleArray? = null

    /** Solves the Bézier parameter at time [t] and evaluates each component into the result buffer. */
    override fun interpolate(i1: Int, t0: Double, t: Double, t1: Double): DoubleArray {
        val incoming = inTangents ?: return interpolateLinearly(i1, (t - t0) / (t1 - t0))
        val outgoing = outTangents ?: return interpolateLinearly(i1, (t - t0) / (t1 - t0))
        val upper = i1 * valueSize
        val lower = upper - valueSize
        for (index in 0 until valueSize) {
            val outOffset = (lower + index) * 2
            val inOffset = (upper + index) * 2
            val s = solveParameter(t, t0, outgoing[outOffset], incoming[inOffset], t1)
            resultBuffer[index] = cubic(s, sampleValues[lower + index], outgoing[outOffset + 1],
                incoming[inOffset + 1], sampleValues[upper + index])
        }
        return resultBuffer
    }

    private fun cubic(s: Double, p0: Double, p1: Double, p2: Double, p3: Double): Double {
        val k = 1.0 - s
        return k * k * k * p0 + 3.0 * k * k * s * p1 + 3.0 * k * s * s * p2 + s * s * s * p3
    }

    private fun solveParameter(x: Double, x0: Double, x1: Double, x2: Double, x3: Double): Double {
        var s = (x - x0) / (x3 - x0)
        repeat(8) {
            val error = cubic(s, x0, x1, x2, x3) - x
            if (abs(error) < 1e-10) return s
            val k = 1.0 - s
            val slope = 3.0 * k * k * (x1 - x0) + 6.0 * k * s * (x2 - x1) + 3.0 * s * s * (x3 - x2)
            if (abs(slope) < 1e-10) return s
            s = MathUtils.clamp(s - error / slope, 0.0, 1.0)
        }
        return s
    }
}
