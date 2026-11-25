package com.aitorgf.threekt.exporters

import com.aitorgf.threekt.core.BufferGeometry
import com.aitorgf.threekt.core.Object3D
import com.aitorgf.threekt.math.Color
import com.aitorgf.threekt.math.Matrix3
import com.aitorgf.threekt.math.Vector3
import com.aitorgf.threekt.objects.Mesh
import com.aitorgf.threekt.objects.Points
import kotlin.js.JsExport

/** Exports meshes or points in world space, including normals, UVs and RGB when available. */
@JsExport
class PLYExporter {
    fun parse(scene: Object3D): String {
        val data = collect(scene)
        return buildString {
            append(data.header("ascii"))
            for (vertex in data.vertices) append(vertex.joinToString(" ")).append('\n')
            for (face in data.faces) append("3 ${face[0]} ${face[1]} ${face[2]}\n")
        }
    }

    /** Writes binary big endian by default. Set [littleEndian] to switch byte order. */
    fun parseBinary(scene: Object3D, littleEndian: Boolean = false): ByteArray {
        val data = collect(scene)
        val header = data.header(if (littleEndian) "binary_little_endian" else "binary_big_endian").encodeToByteArray()
        val floatCount = 3 + (if (data.normals) 3 else 0) + (if (data.uvs) 2 else 0)
        val output = BinaryWriter(ByteArray(header.size + data.vertices.size * (floatCount * 4 + if (data.colors) 3 else 0) +
            data.faces.size * 13), header.size, littleEndian)
        header.copyInto(output.bytes)
        for (vertex in data.vertices) {
            for (i in 0 until floatCount) output.float(vertex[i].toDouble())
            for (i in floatCount until vertex.size) output.byte(vertex[i].toInt())
        }
        for (face in data.faces) {
            output.byte(3)
            face.forEach(output::int)
        }
        return output.bytes
    }

    private fun collect(scene: Object3D): Data {
        scene.updateWorldMatrix(true, true)
        val objects = mutableListOf<Pair<Object3D, BufferGeometry>>()
        scene.traverse { node ->
            when (node) {
                is Mesh -> objects.add(node to node.geometry)
                is Points -> objects.add(node to node.geometry)
            }
        }
        val data = Data(objects.any { it.second.hasAttribute("normal") },
            objects.any { it.second.hasAttribute("uv") }, objects.any { it.second.hasAttribute("color") })
        val point = Vector3()
        val normalMatrix = Matrix3()
        val color = Color()
        for ((node, geometry) in objects) {
            val position = geometry.getAttribute("position") ?: continue
            val normals = geometry.getAttribute("normal")
            val uv = geometry.getAttribute("uv")
            val colors = geometry.getAttribute("color")
            val offset = data.vertices.size
            normalMatrix.getNormalMatrix(node.matrixWorld)
            for (i in 0 until position.count) {
                point.fromBufferAttribute(position, i).applyMatrix4(node.matrixWorld)
                val values = mutableListOf<Number>(point.x, point.y, point.z)
                if (data.normals) {
                    if (normals == null) point.set(0.0, 0.0, 0.0)
                    else point.fromBufferAttribute(normals, i).applyNormalMatrix(normalMatrix)
                    values.addAll(listOf(point.x, point.y, point.z))
                }
                if (data.uvs) values.addAll(listOf(uv?.getX(i) ?: 0.0, uv?.getY(i) ?: 0.0))
                if (data.colors) {
                    val hex = if (colors == null) 0xffffff else color.set(colors.getX(i), colors.getY(i), colors.getZ(i)).getHex()
                    values.addAll(listOf(hex shr 16 and 255, hex shr 8 and 255, hex and 255))
                }
                data.vertices.add(values)
            }
            if (node is Mesh) {
                val indices = geometry.index
                val size = indices?.size ?: position.count
                for (i in 0 until size - 2 step 3) {
                    data.faces.add(IntArray(3) { j -> offset + (indices?.get(i + j) ?: i + j) })
                }
            }
        }
        return data
    }

    private class Data(val normals: Boolean, val uvs: Boolean, val colors: Boolean) {
        val vertices = mutableListOf<List<Number>>()
        val faces = mutableListOf<IntArray>()

        fun header(format: String) = buildString {
            append("ply\nformat $format 1.0\nelement vertex ${vertices.size}\n")
            append("property float x\nproperty float y\nproperty float z\n")
            if (normals) append("property float nx\nproperty float ny\nproperty float nz\n")
            if (uvs) append("property float s\nproperty float t\n")
            if (colors) append("property uchar red\nproperty uchar green\nproperty uchar blue\n")
            if (faces.isNotEmpty()) append("element face ${faces.size}\nproperty list uchar int vertex_index\n")
            append("end_header\n")
        }
    }
}
