package com.aitorgf.threekt.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BufferGeometryTest {
    @Test
    fun drawRangeDefaultsToEverythingAndCanBeNarrowed() {
        val geometry = BufferGeometry()
        assertEquals(0, geometry.drawRange.start)
        assertEquals(Int.MAX_VALUE, geometry.drawRange.count)
        geometry.setDrawRange(3, 30)
        assertEquals(3, geometry.drawRange.start)
        assertEquals(30, geometry.drawRange.count)
    }

    private fun positions(vararg values: Float) = Float32BufferAttribute(values, 3)

    @Test
    fun boundsIncludeAbsoluteMorphTargets() {
        val geometry = BufferGeometry().setAttribute("position", positions(-1f, 0f, 0f, 1f, 0f, 0f))
        geometry.setMorphAttribute("position", arrayOf(positions(-1f, 0f, 0f, 5f, 0f, 0f)))
        geometry.computeBoundingBox()
        assertEquals(5.0, geometry.boundingBox!!.max.x)
        geometry.computeBoundingSphere()
        val sphere = geometry.boundingSphere!!
        assertEquals(2.0, sphere.center.x)
        assertEquals(3.0, sphere.radius)
    }

    @Test
    fun boundsAddRelativeMorphTargetsToTheBase() {
        val geometry = BufferGeometry().setAttribute("position", positions(-1f, 0f, 0f, 1f, 0f, 0f))
        geometry.morphTargetsRelative = true
        geometry.setMorphAttribute("position", arrayOf(positions(0f, 0f, 0f, 4f, 0f, 0f)))
        geometry.computeBoundingBox()
        assertEquals(5.0, geometry.boundingBox!!.max.x)
        geometry.computeBoundingSphere()
        assertEquals(2.0, geometry.boundingSphere!!.center.x)
        assertEquals(3.0, geometry.boundingSphere!!.radius)
    }

    @Test
    fun boundsWithoutPositionsAreEmpty() {
        val geometry = BufferGeometry()
        geometry.computeBoundingBox()
        assertTrue(geometry.boundingBox!!.isEmpty())
        geometry.computeBoundingSphere()
        assertTrue(geometry.boundingSphere!!.isEmpty())
    }
}
