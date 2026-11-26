package com.aitorgf.threekt.core

import com.aitorgf.threekt.geometries.PlaneGeometry
import com.aitorgf.threekt.math.Vector2
import com.aitorgf.threekt.math.Vector3
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
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

    @Test
    fun toNonIndexedExpandsEveryAttributeAndKeepsItsArrayType() {
        val geometry = BufferGeometry()
            .setAttribute("position", positions(0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f, 0f, 1f, 1f, 0f))
            .setAttribute("color", Uint8BufferAttribute(byteArrayOf(0, 85, -86, -1), 1, normalized = true))
            .setIndex(intArrayOf(0, 1, 2, 2, 1, 3))
        geometry.addGroup(0, 6, 1)
        val flat = geometry.toNonIndexed()
        assertNull(flat.index)
        assertEquals(6, flat.getAttribute("position")!!.count)
        assertEquals(1.0, flat.getAttribute("position")!!.getX(4))
        val color = assertIs<Uint8BufferAttribute>(flat.getAttribute("color"))
        assertTrue(color.normalized)
        assertEquals(listOf(0, 85, 170, 170, 85, 255), List(6) { color.readRaw(it).toInt() })
        assertEquals(1, flat.groups.single().materialIndex)
        assertSame(flat, flat.toNonIndexed())
    }

    @Test
    fun computeTangentsFollowsTheUDirection() {
        val plane = PlaneGeometry(2.0, 2.0)
        plane.computeTangents()
        val tangent = plane.getAttribute("tangent")!!
        assertEquals(4, tangent.itemSize)
        for (i in 0 until tangent.count) {
            assertEquals(1.0, tangent.getX(i), 1e-6)
            assertEquals(0.0, tangent.getY(i), 1e-6)
            assertEquals(1.0, tangent.getW(i), 1e-6)
        }
        assertFailsWith<IllegalStateException> { BufferGeometry().computeTangents() }
    }

    @Test
    fun copyClonesDataAndSetFromPointsUpdatesInPlace() {
        val geometry = BufferGeometry().setFromPoints(listOf(Vector2(1.0, 2.0), Vector3(3.0, 4.0, 5.0)))
        geometry.name = "source"
        geometry.userData["tag"] = 1
        geometry.computeBoundingSphere()
        val copy = geometry.clone()
        assertEquals("source", copy.name)
        assertEquals(1, copy.userData["tag"])
        assertNotSame(geometry.getAttribute("position"), copy.getAttribute("position"))
        assertEquals(geometry.boundingSphere!!.radius, copy.boundingSphere!!.radius)

        val position = geometry.getAttribute("position")!!
        val version = position.version
        geometry.setFromPoints(listOf(Vector3(7.0, 8.0, 9.0)))
        assertSame(position, geometry.getAttribute("position"))
        assertEquals(7.0, position.getX(0))
        assertEquals(3.0, position.getX(1))
        assertEquals(version + 1, position.version)
        assertEquals(1.0, copy.getAttribute("position")!!.getX(0))
    }

    @Test
    fun lookAtAimsThePositiveZAxis() {
        val geometry = BufferGeometry().setFromPoints(listOf(Vector3(0.0, 0.0, 1.0)))
        geometry.lookAt(Vector3(1.0, 0.0, 0.0))
        val position = geometry.getAttribute("position")!!
        assertEquals(1.0, position.getX(0), 1e-6)
        assertEquals(0.0, position.getZ(0), 1e-6)
    }

    @Test
    fun disposeDispatchesAnEvent() {
        val geometry = BufferGeometry()
        var disposed = false
        geometry.addEventListener(BufferGeometry.DISPOSE) { disposed = true }
        geometry.dispose()
        assertTrue(disposed)
    }
}
