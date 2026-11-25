package com.aitorgf.threekt.loaders

import com.aitorgf.threekt.objects.LineSegments
import com.aitorgf.threekt.objects.Mesh
import com.aitorgf.threekt.objects.Points
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class OBJLoaderTest {
    @Test
    fun triangulatesRelativeIndicesAndPreservesPrimitiveTypes() {
        val group = OBJLoader().parse(
            """
            v 0 0 0 1 0 0
            v 1 0 0
            v 1 1 0
            v 0 1 0
            vt 0 0
            vt 1 1
            o Quad
            f -4/1 -3 -2/2 -1 # fan
            g Path
            l 1 2 3
            p 4
            """.trimIndent(),
        )
        val mesh = assertIs<Mesh>(group.children[0])
        assertEquals("Quad", mesh.name)
        assertEquals(6, mesh.geometry.getAttribute("position")!!.count)
        assertEquals(1.0, mesh.geometry.getAttribute("normal")!!.getZ(5))
        assertEquals(1.0, mesh.geometry.getAttribute("uv")!!.getY(2))
        assertEquals(1.0, mesh.geometry.getAttribute("color")!!.getX(0))
        assertEquals(0.0, mesh.geometry.getAttribute("color")!!.getY(0))
        assertEquals(4, assertIs<LineSegments>(group.children[1]).geometry.getAttribute("position")!!.count)
        assertEquals(1, assertIs<Points>(group.children[2]).geometry.getAttribute("position")!!.count)
    }

    @Test
    fun rejectsOutOfRangeIndices() {
        assertFailsWith<IllegalArgumentException> { OBJLoader().parse("v 0 0 0\nf 1 2 3") }
    }
}
