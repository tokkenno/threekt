package com.aitorgf.threekt.loaders

import com.aitorgf.threekt.core.Float32BufferAttribute
import com.aitorgf.threekt.objects.LineSegments
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class GCodeLoaderTest {
    @Test
    fun extrusionOverridesAndNewLayerDetection() {
        val loader = GCodeLoader().apply { splitLayer = true }
        val model = loader.parse(
            """
            G90
            M83
            G1 X1 Z0.5 E1
            G1 X2 E1
            G1 X3
            G91
            M82
            G1 X1 Z0.5 E2
            G92 E0
            G1 X1 E1 ; starts depositing at the new Z
            """.trimIndent(),
        )
        assertEquals(4, model.children.size)
        fun vertices(index: Int): FloatArray {
            val line = model.children[index] as LineSegments
            return (line.geometry.getAttribute("position") as Float32BufferAttribute).array
        }
        assertContentEquals(floatArrayOf(0f, 0f, 0f, 1f, 0f, 0.5f, 1f, 0f, 0.5f, 2f, 0f, 0.5f), vertices(0))
        assertContentEquals(floatArrayOf(2f, 0f, 0.5f, 3f, 0f, 0.5f, 3f, 0f, 0.5f, 4f, 0f, 1f), vertices(1))
        assertContentEquals(floatArrayOf(4f, 0f, 1f, 5f, 0f, 1f), vertices(2))
        assertContentEquals(floatArrayOf(), vertices(3))
    }
}
