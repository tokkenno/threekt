package com.aitorgf.threekt.exporters

import com.aitorgf.threekt.geometries.BoxGeometry
import com.aitorgf.threekt.loaders.OBJLoader
import com.aitorgf.threekt.loaders.STLLoader
import com.aitorgf.threekt.objects.Group
import com.aitorgf.threekt.objects.Mesh
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GeometryExportTest {
    @Test
    fun stlAndObjPreserveWorldTriangles() {
        val root = Group()
        root.position.x = 2.0
        val mesh = Mesh(BoxGeometry())
        root.add(mesh)
        val stl = STLExporter()
        val loader = STLLoader()
        val ascii = loader.parse(stl.parse(root).encodeToByteArray())
        val binary = loader.parse(stl.parseBinary(root))
        val obj = OBJLoader().parse(OBJExporter().parse(root)).children.single() as Mesh
        for (geometry in listOf(ascii, binary, obj.geometry)) {
            val position = geometry.getAttribute("position")!!
            assertEquals(36, position.count)
            assertEquals(2.5, position.getX(0))
        }
    }

    @Test
    fun plyBinaryOptionsWriteTheDeclaredByteOrder() {
        val mesh = Mesh(BoxGeometry())
        val exporter = PLYExporter()
        for (littleEndian in listOf(false, true)) {
            val bytes = exporter.parseBinary(mesh, littleEndian)
            val marker = "end_header\n".encodeToByteArray()
            val start = bytes.indices.first { i -> marker.indices.all { j -> i + j < bytes.size && marker[j] == bytes[i + j] } }
            val offset = start + marker.size
            val header = bytes.decodeToString(0, offset)
            val format = if (littleEndian) "binary_little_endian" else "binary_big_endian"
            assertTrue(header.contains("format $format 1.0\n"))
            assertTrue(header.contains("element face 12\n"))
            val half = byteArrayOf(0x3f, 0, 0, 0)
            val expected = if (littleEndian) half.reversedArray() else half
            assertTrue(bytes.copyOfRange(offset, offset + 4).contentEquals(expected))
        }
    }
}
