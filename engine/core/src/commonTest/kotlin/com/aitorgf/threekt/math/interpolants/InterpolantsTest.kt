package com.aitorgf.threekt.math.interpolants

import com.aitorgf.threekt.math.Interpolant
import com.aitorgf.threekt.math.Quaternion
import com.aitorgf.threekt.math.Vector3
import kotlin.math.PI
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class InterpolantsTest {
    @Test
    fun linearInterpolationSelectsTheLaterDuplicateKeyframeAndSupportsBackwardSeeking() {
        val times = doubleArrayOf(0.0, 1.0, 1.0, 3.0)
        val samples = doubleArrayOf(0.0, 10.0, 20.0, 40.0)
        val interpolant = LinearInterpolant(times, samples, 1)

        assertEquals(30.0, interpolant.evaluate(2.0)[0])
        assertEquals(20.0, interpolant.evaluate(1.0)[0])
        assertEquals(5.0, interpolant.evaluate(0.5)[0])
        assertEquals(40.0, interpolant.evaluate(Double.POSITIVE_INFINITY)[0])
        assertEquals(0.0, interpolant.evaluate(Double.NEGATIVE_INFINITY)[0])
    }

    @Test
    fun resultBufferCanBeReplacedAndOnlyItsSampleComponentsAreWritten() {
        val interpolant = LinearInterpolant(
            doubleArrayOf(0.0, 2.0),
            doubleArrayOf(2.0, 6.0, 4.0, 10.0),
            2,
        )
        val buffer = doubleArrayOf(-1.0, -1.0, -7.0)
        interpolant.resultBuffer = buffer
        assertSame(buffer, interpolant.evaluate(1.0))
        assertContentEquals(doubleArrayOf(3.0, 8.0, -7.0), buffer)
        assertSame(buffer, interpolant.evaluate(4.0))
        assertContentEquals(doubleArrayOf(4.0, 10.0, -7.0), buffer)
        assertFailsWith<IllegalArgumentException> { interpolant.resultBuffer = DoubleArray(1) }
        assertSame(buffer, interpolant.resultBuffer)
    }

    @Test
    fun interpolantsRejectInvalidLayoutsAndNanQueries() {
        assertFailsWith<IllegalArgumentException> {
            LinearInterpolant(doubleArrayOf(), doubleArrayOf(), 1)
        }
        assertFailsWith<IllegalArgumentException> {
            LinearInterpolant(doubleArrayOf(2.0, 1.0), doubleArrayOf(0.0, 1.0), 1)
        }
        assertFailsWith<IllegalArgumentException> {
            LinearInterpolant(doubleArrayOf(0.0, Double.POSITIVE_INFINITY), doubleArrayOf(0.0, 1.0), 1)
        }
        assertFailsWith<IllegalArgumentException> {
            LinearInterpolant(doubleArrayOf(0.0), doubleArrayOf(0.0), 2)
        }
        assertFailsWith<IllegalArgumentException> {
            LinearInterpolant(doubleArrayOf(0.0), doubleArrayOf(0.0), 1, DoubleArray(0))
        }
        assertFailsWith<IllegalArgumentException> {
            QuaternionLinearInterpolant(doubleArrayOf(0.0), doubleArrayOf(0.0, 0.0, 1.0), 3)
        }

        val track = DiscreteInterpolant(doubleArrayOf(0.0), doubleArrayOf(2.0), 1)
        assertFailsWith<IllegalArgumentException> { track.evaluate(Double.NaN) }
    }

    @Test
    fun discreteInterpolationSwitchesAtKeysAndClampsSingleSampleTracks() {
        val times = doubleArrayOf(0.0, 1.0, 3.0)
        val samples = doubleArrayOf(2.0, 4.0, -7.0)
        val discrete = DiscreteInterpolant(times, samples, 1)
        assertEquals(2.0, discrete.evaluate(0.999)[0])
        assertEquals(4.0, discrete.evaluate(1.0)[0])
        assertEquals(4.0, discrete.evaluate(2.999)[0])
        assertEquals(-7.0, discrete.evaluate(3.0)[0])
        val constant = DiscreteInterpolant(doubleArrayOf(5.0), doubleArrayOf(-1.0, 7.0), 2)
        for (time in listOf(-10.0, 5.0, 20.0)) {
            assertContentEquals(doubleArrayOf(-1.0, 7.0), constant.evaluate(time))
        }
    }

    @Test
    fun cubicWrapAroundTangentsMatchThreeJsAtBothEnds() {
        val cubic = CubicInterpolant(doubleArrayOf(0.0, 1.0, 3.0, 7.0), doubleArrayOf(2.0, 4.0, -2.0, 2.0), 1)
        cubic.settings = Interpolant.Settings(Interpolant.Ending.WrapAround, Interpolant.Ending.WrapAround)

        assertEquals(2.546875, cubic.evaluate(0.25)[0], 1e-12)
        assertEquals(-1.25, cubic.evaluate(5.0)[0], 1e-12)
        assertEquals(1.19921875, cubic.evaluate(6.5)[0], 1e-12)
        assertEquals(2.0, cubic.evaluate(9.0)[0])
        assertEquals(2.546875, cubic.evaluate(0.25)[0], 1e-12)
    }

    @Test
    fun bezierUsesSeparateControlPairsForEachComponentAndFallsBackToLinear() {
        val bezier = BezierInterpolant(doubleArrayOf(0.0, 1.0), doubleArrayOf(0.0, 2.0, 1.0, 8.0), 2)
        assertContentEquals(doubleArrayOf(0.25, 3.5), bezier.evaluate(0.25))
        bezier.inTangents = doubleArrayOf(0.0, 0.0, 0.0, 2.0, 2.0 / 3.0, 1.0, 2.0 / 3.0, 6.0)
        assertContentEquals(doubleArrayOf(0.25, 3.5), bezier.evaluate(0.25))
        bezier.outTangents = doubleArrayOf(1.0 / 3.0, 0.0, 1.0 / 3.0, 4.0, 1.0, 1.0, 1.0, 8.0)
        assertEquals(0.15625, bezier.evaluate(0.25)[0], 1e-12)
        assertEquals(3.5, bezier.evaluate(0.25)[1], 1e-12)
        assertContentEquals(doubleArrayOf(1.0, 8.0), bezier.evaluate(1.0))
    }

    @Test
    fun quaternionTracksHandleIdenticalAndAntipodalSamples() {
        val rotation = Quaternion().setFromAxisAngle(Vector3(0.0, 1.0, 0.0), PI / 3)
        val opposite = Quaternion(-rotation.x, -rotation.y, -rotation.z, -rotation.w)
        val track = QuaternionLinearInterpolant(
            doubleArrayOf(0.0, 1.0, 2.0),
            rotation.toArray() + rotation.toArray() + opposite.toArray(),
            4,
        )
        assertContentEquals(rotation.toArray(), track.evaluate(0.5))
        val blended = Quaternion().fromArray(track.evaluate(1.5))
        assertEquals(1.0, blended.length(), 1e-12)
        assertEquals(1.0, abs(blended.dot(rotation)), 1e-12)
        assertContentEquals(opposite.toArray(), track.evaluate(2.0))
    }
}
