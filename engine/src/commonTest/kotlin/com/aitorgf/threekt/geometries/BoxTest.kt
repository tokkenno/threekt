package com.aitorgf.threekt.geometries

import com.aitorgf.threekt.core.Float32BufferAttribute
import com.aitorgf.threekt.materials.MeshBasicMaterial
import com.aitorgf.threekt.math.Vector3
import com.aitorgf.threekt.objects.Mesh
import com.aitorgf.threekt.scenes.Scene
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class BoxTest {
    @Test
    fun boxHasCenteredVerticesAndOutwardFacingTriangles() {
        val geometry = Box(2.0, 4.0, 6.0)
        val positions = assertIs<Float32BufferAttribute>(geometry.getAttribute("position"))
        val normals = assertIs<Float32BufferAttribute>(geometry.getAttribute("normal"))
        val uvs = assertIs<Float32BufferAttribute>(geometry.getAttribute("uv"))
        val indices = assertNotNull(geometry.index)

        assertEquals(24, positions.count)
        assertEquals(24, normals.count)
        assertEquals(24, uvs.count)
        assertEquals(36, indices.size)
        assertTrue(indices.all { it in 0 until positions.count })
        for (vertex in 0 until positions.count) {
            assertEquals(1f, abs(positions.array[vertex * 3]))
            assertEquals(2f, abs(positions.array[vertex * 3 + 1]))
            assertEquals(3f, abs(positions.array[vertex * 3 + 2]))
        }

        for (triangle in indices.indices step 3) {
            val a = indices[triangle] * 3
            val b = indices[triangle + 1] * 3
            val c = indices[triangle + 2] * 3
            val ab = FloatArray(3) { positions.array[b + it] - positions.array[a + it] }
            val ac = FloatArray(3) { positions.array[c + it] - positions.array[a + it] }
            val normalX = ab[1] * ac[2] - ab[2] * ac[1]
            val normalY = ab[2] * ac[0] - ab[0] * ac[2]
            val normalZ = ab[0] * ac[1] - ab[1] * ac[0]
            val outward = normalX * positions.array[a] +
                normalY * positions.array[a + 1] + normalZ * positions.array[a + 2]
            val alignment = normalX * normals.array[a] +
                normalY * normals.array[a + 1] + normalZ * normals.array[a + 2]
            assertTrue(outward > 0f)
            assertTrue(alignment > 0f)
        }
    }

    @Test
    fun meshCarriesGeometryAndMaterialThroughSceneHierarchy() {
        val box = Box()
        val material = MeshBasicMaterial(0x336699)
        val mesh = Mesh(box, material)
        val scene = Scene()
        scene.position.x = 2.0
        mesh.position.x = 3.0
        scene.add(mesh)
        scene.updateMatrixWorld()

        assertSame(box, mesh.geometry)
        assertSame(material, mesh.material)
        assertEquals(5.0, Vector3().applyMatrix4(mesh.matrixWorld).x)
    }
}
