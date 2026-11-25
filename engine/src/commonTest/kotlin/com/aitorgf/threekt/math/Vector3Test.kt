package com.aitorgf.threekt.math

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class Vector3Test {
    @Test
    fun setKeepsZWhenOmitted() {
        val vector = Vector3(1.0, 2.0, 3.0)

        assertSame(vector, vector.set(4.0, 5.0))
        assertEquals(4.0, vector.x)
        assertEquals(5.0, vector.y)
        assertEquals(3.0, vector.z)
    }

    @Test
    fun copyReadsCoordinatesWithoutChangingSource() {
        val source = Vector3(1.0, 2.0, 3.0)
        val vector = Vector3()

        assertSame(vector, vector.copy(source))
        assertEquals(1.0, vector.x)
        assertEquals(2.0, vector.y)
        assertEquals(3.0, vector.z)
        vector.set(4.0, 5.0, 6.0)
        assertEquals(1.0, source.x)
        assertEquals(2.0, source.y)
        assertEquals(3.0, source.z)
    }

    @Test
    fun arithmeticMutatesAndReturnsReceiver() {
        val vector = Vector3(1.0, 2.0, 3.0)
        val offset = Vector3(2.0, 3.0, 4.0)

        assertSame(vector, vector.add(offset))
        assertEquals(3.0, vector.x)
        assertEquals(5.0, vector.y)
        assertEquals(7.0, vector.z)
        assertSame(vector, vector.sub(offset))
        assertSame(vector, vector.multiplyScalar(2.0))
        assertEquals(2.0, vector.x)
        assertEquals(4.0, vector.y)
        assertEquals(6.0, vector.z)
    }

    @Test
    fun normalizeProducesUnitVector() {
        val vector = Vector3(3.0, 4.0, 0.0)

        assertEquals(5.0, vector.length())
        assertSame(vector, vector.normalize())
        assertEquals(0.6, vector.x, 1e-12)
        assertEquals(0.8, vector.y, 1e-12)
        assertEquals(0.0, vector.z)
        assertEquals(1.0, vector.length(), 1e-12)
    }

    @Test
    fun normalizePreservesZeroVector() {
        val vector = Vector3()

        assertSame(vector, vector.normalize())
        assertEquals(0.0, vector.x)
        assertEquals(0.0, vector.y)
        assertEquals(0.0, vector.z)
    }
}
