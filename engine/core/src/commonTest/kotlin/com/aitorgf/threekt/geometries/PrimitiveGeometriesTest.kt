package com.aitorgf.threekt.geometries

import com.aitorgf.threekt.core.BufferGeometry
import com.aitorgf.threekt.core.Float32BufferAttribute
import com.aitorgf.threekt.math.Vector2
import kotlin.math.abs
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals

/** Compares vertex counts and absolute attribute sums with Three.js 0.187.0. */
class PrimitiveGeometriesTest {
    private fun assertMatches(
        geometry: BufferGeometry, count: Int, indexCount: Int, positionSum: Double, normalSum: Double, uvSum: Double,
    ) {
        fun sum(name: String) = (geometry.getAttribute(name) as Float32BufferAttribute).array.sumOf { abs(it.toDouble()) }
        assertEquals(count, geometry.getAttribute("position")!!.count)
        assertEquals(indexCount, geometry.index?.size ?: -1)
        assertEquals(positionSum, sum("position"), 0.01)
        assertEquals(normalSum, sum("normal"), 0.01)
        assertEquals(uvSum, sum("uv"), 0.01)
    }

    @Test
    fun ringTorusAndKnotMatchThreeJs() {
        assertMatches(RingGeometry(10.0, 50.0, 20, 5), 126, 600, 4725.901, 126.0, 127.8)
        assertMatches(TorusGeometry(50.0, 20.0, 20, 20), 441, 2400, 33396.408, 622.968, 441.0)
        assertMatches(TorusKnotGeometry(50.0, 10.0, 50, 20), 1071, 6000, 85342.819, 1608.582, 1071.0)
    }

    @Test
    fun latheCapsuleAndConeMatchThreeJs() {
        val points = Array(50) { i -> Vector2(sin(i * 0.2) * sin(i * 0.1) * 15 + 50, (i - 5) * 2.0) }
        assertMatches(LatheGeometry(points, 20), 1050, 5880, 105434.496, 1615.044, 1050.0)
        assertMatches(CapsuleGeometry(20.0, 50.0), 90, 432, 4619.571, 118.479, 90.125)
        assertMatches(ConeGeometry(5.0, 10.0, 8), 35, 48, 281.569, 44.113, 35.5)
    }

    @Test
    fun polyhedraMatchThreeJs() {
        assertMatches(OctahedronGeometry(75.0), 24, -1, 1800.0, 41.569, 24.0)
        assertMatches(TetrahedronGeometry(75.0), 12, -1, 1558.846, 20.785, 12.0)
        assertMatches(DodecahedronGeometry(75.0, 1), 432, -1, 48018.946, 640.253, 447.182)
    }

    @Test
    fun parametricKleinBottleMatchesThreeJs() {
        assertMatches(ParametricGeometry(ParametricFunctions::klein, 20, 20), 441, 2400, 3675.456, 628.406, 441.0)
    }
}
