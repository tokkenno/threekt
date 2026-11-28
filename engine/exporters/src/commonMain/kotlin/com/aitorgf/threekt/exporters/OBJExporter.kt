package com.aitorgf.threekt.exporters

import com.aitorgf.threekt.core.Object3D
import com.aitorgf.threekt.math.Color
import com.aitorgf.threekt.math.Matrix3
import com.aitorgf.threekt.math.Vector3
import com.aitorgf.threekt.objects.Line
import com.aitorgf.threekt.objects.LineSegments
import com.aitorgf.threekt.objects.Mesh
import com.aitorgf.threekt.objects.Points
import kotlin.js.JsExport

/** Exports meshes, line strips, line segments and points with world-space geometry. */
@JsExport
class OBJExporter {
    fun parse(scene: Object3D): String = buildString {
        scene.updateWorldMatrix(true, true)
        var vertexOffset = 1
        var uvOffset = 1
        var normalOffset = 1
        val point = Vector3()
        val normalMatrix = Matrix3()
        val color = Color()
        scene.traverse { node ->
            val geometry = when (node) {
                is Mesh -> node.geometry
                is Line -> node.geometry
                is Points -> node.geometry
                else -> return@traverse
            }
            val position = geometry.getAttribute("position") ?: return@traverse
            val uv = if (node is Mesh) geometry.getAttribute("uv") else null
            val normal = if (node is Mesh) geometry.getAttribute("normal") else null
            val colors = if (node is Points) geometry.getAttribute("color") else null
            append("o ").append(node.name.replace('\n', ' ').replace('\r', ' ')).append('\n')
            for (i in 0 until position.count) {
                point.fromBufferAttribute(position, i).applyMatrix4(node.matrixWorld)
                append("v ${point.x} ${point.y} ${point.z}")
                if (colors != null) {
                    color.set(colors.getX(i), colors.getY(i), colors.getZ(i)).convertLinearToSRGB()
                    append(" ${color.r} ${color.g} ${color.b}")
                }
                append('\n')
            }
            if (uv != null) for (i in 0 until uv.count) append("vt ${uv.getX(i)} ${uv.getY(i)}\n")
            if (normal != null) {
                normalMatrix.getNormalMatrix(node.matrixWorld)
                for (i in 0 until normal.count) {
                    point.fromBufferAttribute(normal, i).applyNormalMatrix(normalMatrix)
                    append("vn ${point.x} ${point.y} ${point.z}\n")
                }
            }
            val indices = geometry.index
            val count = indices?.count ?: position.count
            fun index(i: Int) = indices?.getX(i)?.toInt() ?: i
            when (node) {
                is Mesh -> for (i in 0 until count - 2 step 3) {
                    append('f')
                    for (j in i..(i + 2)) {
                        val vertex = index(j)
                        append(' ').append(vertexOffset + vertex)
                        if (uv != null || normal != null) {
                            append('/')
                            if (uv != null) append(uvOffset + vertex)
                            if (normal != null) append('/').append(normalOffset + vertex)
                        }
                    }
                    append('\n')
                }
                is LineSegments -> for (i in 0 until count - 1 step 2) {
                    append("l ${vertexOffset + index(i)} ${vertexOffset + index(i + 1)}\n")
                }
                else -> if (count > 0) {
                    append(if (node is Points) 'p' else 'l')
                    for (i in 0 until count) append(' ').append(vertexOffset + index(i))
                    append('\n')
                }
            }
            vertexOffset += position.count
            uvOffset += uv?.count ?: 0
            normalOffset += normal?.count ?: 0
        }
    }
}
