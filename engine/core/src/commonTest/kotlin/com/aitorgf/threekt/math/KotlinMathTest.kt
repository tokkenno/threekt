package com.aitorgf.threekt.math

import com.aitorgf.threekt.math.interpolants.CubicInterpolant
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertSame

class KotlinMathTest {
    @Test
    fun vectorIndexingAndCopiesWorkWithKotlinEquality() {
        val vector = Vector3(1.0, 2.0, 3.0)
        val copy = vector.copy()
        assertEquals(vector, copy)
        assertEquals(vector.hashCode(), copy.hashCode())

        copy[1] = 4.0
        assertEquals(2.0, vector[1])
        assertNotEquals(vector, copy)
        assertSame(copy, copy.clampLength(0.0, 10.0))
    }

    @Test
    fun multiplicationWorksWhenDestinationAliasesEitherOperand() {
        val left = Matrix4().makeTranslation(1.0, 2.0, 3.0)
        val right = Matrix4().makeScale(2.0, 3.0, 4.0)
        val expected = Matrix4().multiplyMatrices(left, right)

        assertEquals(expected, left.copy().multiply(right))
        assertEquals(expected, right.copy().premultiply(left))
        val both = expected.copy()
        assertEquals(Matrix4().multiplyMatrices(expected, expected), both.multiply(both))
    }

    @Test
    fun seededRandomIsLocalToTheCaller() {
        val first = Random(42)
        val second = Random(42)
        assertEquals(Vector3().random(first), Vector3().random(second))
        assertEquals(Quaternion().random(first), Quaternion().random(second))
    }

    @Test
    fun cubicSettingsRefreshTheCachedInterval() {
        val times = doubleArrayOf(0.0, 1.0)
        val samples = doubleArrayOf(0.0, 4.0)
        val interpolant = CubicInterpolant(times, samples, 1)
        val original = interpolant.evaluate(0.25)[0]

        interpolant.settings = Interpolant.Settings(
            endingStart = Interpolant.Ending.ZeroSlope,
            endingEnd = Interpolant.Ending.ZeroSlope,
        )
        val fresh = CubicInterpolant(times, samples, 1).apply { settings = interpolant.settings }
        assertNotEquals(original, interpolant.evaluate(0.25)[0])
        assertEquals(fresh.evaluate(0.25)[0], interpolant.evaluate(0.25)[0])
        assertEquals(0.625, interpolant.evaluate(0.25)[0], 1e-12)
    }
}
