package com.aitorgf.threekt.objects

import com.aitorgf.threekt.core.BufferGeometry
import com.aitorgf.threekt.core.Float32BufferAttribute
import com.aitorgf.threekt.materials.LineBasicMaterial
import com.aitorgf.threekt.materials.PointsMaterial
import com.aitorgf.threekt.math.Color
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class LineTest {
    private fun geometry() = BufferGeometry().setAttribute(
        "position", Float32BufferAttribute(floatArrayOf(0f, 0f, 0f, 3f, 0f, 0f, 3f, 4f, 0f, 3f, 4f, 2f), 3),
    )

    @Test
    fun linesDefaultToAWhiteBasicMaterial() {
        val material = assertIs<LineBasicMaterial>(Line().material)
        assertEquals(Color(1.0, 1.0, 1.0), material.color)
        assertEquals(1.0, material.linewidth)
        assertEquals(Color(0xff0000), LineBasicMaterial(0xff0000).color)
    }

    @Test
    fun lineDistancesAccumulateAlongTheLineAndAcrossSegments() {
        val line = Line(geometry()).computeLineDistances()
        assertContentEquals(floatArrayOf(0f, 3f, 7f, 9f), (line.geometry.getAttribute("lineDistance") as Float32BufferAttribute).array)
        val segments = LineSegments(geometry()).computeLineDistances()
        // Segments are (0 → 1) and (2 → 3); the gap between them does not count.
        assertContentEquals(floatArrayOf(0f, 3f, 3f, 5f), (segments.geometry.getAttribute("lineDistance") as Float32BufferAttribute).array)
    }

    @Test
    fun morphTargetsCreateZeroInfluencesKeyedByName() {
        val geometry = geometry()
        assertNull(Line(geometry).morphTargetInfluences)
        val named = Float32BufferAttribute(FloatArray(12), 3).apply { name = "target1" }
        geometry.setMorphAttribute("position", arrayOf(named, Float32BufferAttribute(FloatArray(12), 3)))
        val line = LineLoop(geometry)
        assertContentEquals(doubleArrayOf(0.0, 0.0), line.morphTargetInfluences)
        assertEquals(mapOf("target1" to 0, "1" to 1), line.morphTargetDictionary)
        assertContentEquals(doubleArrayOf(0.0, 0.0), Mesh(geometry).morphTargetInfluences)
    }

    @Test
    fun pointsDefaultToAttenuatedOnePixelWhitePoints() {
        val material = assertIs<PointsMaterial>(Points().material)
        assertEquals(Color(1.0, 1.0, 1.0), material.color)
        assertEquals(1.0, material.size)
        assertEquals(true, material.sizeAttenuation)
        assertEquals(true, material.fog)
        val geometry = geometry().setMorphAttribute("position", arrayOf(Float32BufferAttribute(FloatArray(12), 3)))
        assertContentEquals(doubleArrayOf(0.0), Points(geometry).morphTargetInfluences)
    }
}
