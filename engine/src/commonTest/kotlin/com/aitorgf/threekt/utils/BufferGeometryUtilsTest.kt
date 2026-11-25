package com.aitorgf.threekt.utils

import com.aitorgf.threekt.core.BufferGeometry
import com.aitorgf.threekt.core.Float32BufferAttribute
import com.aitorgf.threekt.geometries.Box
import com.aitorgf.threekt.math.Matrix4
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BufferGeometryUtilsTest {
    @Test
    fun mergeOffsetsIndicesAndKeepsTransformedPositions() {
        val a = Box()
        val b = Box().clone().applyMatrix4(Matrix4().makeTranslation(10.0, 0.0, 0.0))
        val merged = BufferGeometryUtils.mergeGeometries(arrayOf(a, b))!!
        val count = a.getAttribute("position")!!.count
        assertEquals(count * 2, merged.getAttribute("position")!!.count)
        assertEquals(a.index!![0] + count, merged.index!![a.index!!.size])
        merged.computeBoundingBox()
        assertEquals(10.5, merged.boundingBox!!.max.x)
    }

    @Test
    fun incompatibleGeometriesReturnNull() {
        val positions = BufferGeometry().setAttribute("position", Float32BufferAttribute(FloatArray(9), 3))
        assertNull(BufferGeometryUtils.mergeGeometries(arrayOf(Box(), positions)))
    }

    @Test
    fun computedNormalsOfAFlatTriangleFaceTheViewer() {
        val geometry = BufferGeometry().setAttribute(
            "position", Float32BufferAttribute(floatArrayOf(0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f, 0f), 3),
        )
        geometry.computeVertexNormals()
        assertEquals(1.0, geometry.getAttribute("normal")!!.getZ(2))
    }
}
