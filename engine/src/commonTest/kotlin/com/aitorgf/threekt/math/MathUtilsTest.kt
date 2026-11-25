package com.aitorgf.threekt.math

import kotlin.math.PI
import kotlin.math.ln
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MathUtilsTest {
    @Test
    fun interpolationAndRangeMappingWorkAtAndBeyondEndpoints() {
        assertEquals(0.0, MathUtils.clamp(-5.0, 0.0, 1.0))
        assertEquals(1.0, MathUtils.clamp(5.0, 0.0, 1.0))
        assertEquals(2.0, MathUtils.euclideanModulo(-4.0, 3.0))
        assertEquals(-10.0, MathUtils.mapLinear(5.0, 0.0, 10.0, 0.0, -20.0))
        assertEquals(1.5, MathUtils.inverseLerp(2.0, 6.0, 8.0))
        assertEquals(0.0, MathUtils.inverseLerp(2.0, 2.0, 100.0))
        assertEquals(8.0, MathUtils.lerp(2.0, 6.0, 1.5))
        assertEquals(1.0, MathUtils.pingpong(-1.0, 3.0))
        assertEquals(2.0, MathUtils.pingpong(4.0, 3.0))
    }

    @Test
    fun smoothCurvesClampAndHaveDifferentInteriorShapes() {
        assertEquals(0.0, MathUtils.smoothstep(-1.0, 0.0, 4.0))
        assertEquals(1.0, MathUtils.smoothstep(5.0, 0.0, 4.0))
        assertEquals(0.15625, MathUtils.smoothstep(1.0, 0.0, 4.0))
        assertEquals(0.0, MathUtils.smootherstep(-1.0, 0.0, 4.0))
        assertEquals(1.0, MathUtils.smootherstep(5.0, 0.0, 4.0))
        assertEquals(0.103515625, MathUtils.smootherstep(1.0, 0.0, 4.0))
        assertEquals(4.0, MathUtils.damp(2.0, 6.0, ln(2.0), 1.0), 1e-12)
        assertEquals(2.0, MathUtils.damp(2.0, 6.0, 5.0, 0.0))
    }

    @Test
    fun powerOfTwoAndAngleConversionsHandleBoundaries() {
        assertTrue(MathUtils.isPowerOfTwo(1))
        assertTrue(MathUtils.isPowerOfTwo(1 shl 30))
        assertFalse(MathUtils.isPowerOfTwo(0))
        assertFalse(MathUtils.isPowerOfTwo(15))
        assertEquals(16.0, MathUtils.ceilPowerOfTwo(12.0), 1e-12)
        assertEquals(8.0, MathUtils.floorPowerOfTwo(12.0), 1e-12)
        assertEquals(PI, MathUtils.degToRad(180.0), 1e-12)
        assertEquals(-90.0, MathUtils.radToDeg(-PI / 2.0), 1e-12)
    }

    @Test
    fun randomHelpersUseTheirCallerSuppliedGenerator() {
        val first = Random(101)
        val second = Random(101)
        assertEquals(Int.MAX_VALUE, MathUtils.randInt(Int.MAX_VALUE, Int.MAX_VALUE, first))
        assertEquals(Int.MAX_VALUE, MathUtils.randInt(Int.MAX_VALUE, Int.MAX_VALUE, second))
        repeat(16) {
            val value = MathUtils.randInt(-2, 3, first)
            assertEquals(value, MathUtils.randInt(-2, 3, second))
            assertTrue(value in -2..3)
            val scalar = MathUtils.randFloat(-2.0, 3.0, first)
            assertEquals(scalar, MathUtils.randFloat(-2.0, 3.0, second))
            assertTrue(scalar >= -2.0 && scalar < 3.0)
            val spread = MathUtils.randFloatSpread(6.0, first)
            assertEquals(spread, MathUtils.randFloatSpread(6.0, second))
            assertTrue(spread > -3.0 && spread <= 3.0)
        }
    }
}
