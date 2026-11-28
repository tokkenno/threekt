package com.aitorgf.threekt.exporters

import com.aitorgf.threekt.core.Object3D
import com.aitorgf.threekt.math.Vector3
import com.aitorgf.threekt.objects.Mesh
import kotlin.js.JsExport

/** Exports triangle meshes in world coordinates. Threekt exposes separate methods for text and binary. */
@JsExport
class STLExporter {
    fun parse(scene: Object3D): String = buildString {
        append("solid exported\n")
        forEachTriangle(scene) { a, b, c, normal ->
            append("\tfacet normal ${normal.x} ${normal.y} ${normal.z}\n\t\touter loop\n")
            for (vertex in listOf(a, b, c)) append("\t\t\tvertex ${vertex.x} ${vertex.y} ${vertex.z}\n")
            append("\t\tendloop\n\tendfacet\n")
        }
        append("endsolid exported\n")
    }

    fun parseBinary(scene: Object3D): ByteArray {
        var count = 0
        scene.traverse { object3d ->
            if (object3d is Mesh) {
                val geometry = object3d.geometry
                count += (geometry.index?.count ?: geometry.getAttribute("position")?.count ?: 0) / 3
            }
        }
        val output = BinaryWriter(ByteArray(84 + count * 50), 80)
        output.int(count)
        forEachTriangle(scene) { a, b, c, normal ->
            for (point in listOf(normal, a, b, c)) {
                output.float(point.x)
                output.float(point.y)
                output.float(point.z)
            }
            output.byte(0)
            output.byte(0)
        }
        return output.bytes
    }

    private fun forEachTriangle(scene: Object3D, action: (Vector3, Vector3, Vector3, Vector3) -> Unit) {
        scene.updateWorldMatrix(true, true)
        val a = Vector3()
        val b = Vector3()
        val c = Vector3()
        val normal = Vector3()
        val edge = Vector3()
        scene.traverse { node ->
            if (node !is Mesh) return@traverse
            val geometry = node.geometry
            val position = geometry.getAttribute("position") ?: return@traverse
            val indices = geometry.index
            val matrix = node.matrixWorld
            val size = indices?.count ?: position.count
            for (i in 0 until size - 2 step 3) {
                a.fromBufferAttribute(position, indices?.getX(i)?.toInt() ?: i).applyMatrix4(matrix)
                b.fromBufferAttribute(position, indices?.getX(i + 1)?.toInt() ?: i + 1).applyMatrix4(matrix)
                c.fromBufferAttribute(position, indices?.getX(i + 2)?.toInt() ?: i + 2).applyMatrix4(matrix)
                normal.subVectors(b, a).cross(edge.subVectors(c, a)).normalize()
                action(a, b, c, normal)
            }
        }
    }
}
