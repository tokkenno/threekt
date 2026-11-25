package com.aitorgf.threekt.geometries

import com.aitorgf.threekt.core.BufferGeometry
import com.aitorgf.threekt.core.Float32BufferAttribute
import com.aitorgf.threekt.math.Vector3
import kotlin.js.JsExport
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.hypot

/** Subdivides triangular faces and projects the resulting vertices onto a sphere. */
@JsExport
open class PolyhedronGeometry(
    vertices: DoubleArray,
    indices: IntArray,
    radius: Double = 1.0,
    detail: Int = 0,
) : BufferGeometry() {
    init {
        require(detail >= 0 && vertices.size % 3 == 0 && indices.size % 3 == 0)
        val columns = detail + 1
        val count = indices.size * columns * columns
        val positions = FloatArray(count * 3)
        val normals = FloatArray(count * 3)
        val uvs = FloatArray(count * 2)
        var vertex = 0
        val direction = Vector3()
        val normal = Vector3()
        val edge = Vector3()

        fun vertexAt(index: Int): Vector3 = Vector3(vertices[index * 3], vertices[index * 3 + 1], vertices[index * 3 + 2])

        fun appendTriangle(a: Vector3, b: Vector3, c: Vector3) {
            val triangle = arrayOf(a, b, c)
            val centroid = a.copy().normalize().add(b.copy().normalize()).add(c.copy().normalize()).multiplyScalar(radius)
            val azimuth = atan2(centroid.z, -centroid.x)
            if (detail == 0) normal.subVectors(b, a).cross(edge.subVectors(c, a)).normalize()
            val start = vertex
            for (point in triangle) {
                direction.copy(point).normalize().multiplyScalar(radius)
                positions[vertex * 3] = direction.x.toFloat()
                positions[vertex * 3 + 1] = direction.y.toFloat()
                positions[vertex * 3 + 2] = direction.z.toFloat()
                var u = atan2(direction.z, -direction.x) / (2 * PI) + 0.5
                if (azimuth < 0 && u == 1.0) u -= 1
                if (direction.x == 0.0 && direction.z == 0.0) u = azimuth / (2 * PI) + 0.5
                uvs[vertex * 2] = u.toFloat()
                uvs[vertex * 2 + 1] = (0.5 - atan2(-direction.y, hypot(direction.x, direction.z)) / PI).toFloat()
                if (detail != 0) normal.copy(direction).normalize()
                normals[vertex * 3] = normal.x.toFloat()
                normals[vertex * 3 + 1] = normal.y.toFloat()
                normals[vertex * 3 + 2] = normal.z.toFloat()
                vertex++
            }
            val u0 = uvs[start * 2]
            val u1 = uvs[(start + 1) * 2]
            val u2 = uvs[(start + 2) * 2]
            if (maxOf(u0, u1, u2) > 0.9f && minOf(u0, u1, u2) < 0.1f) {
                for (i in start until vertex) if (uvs[i * 2] < 0.2f) uvs[i * 2] += 1f
            }
        }

        for (face in indices.indices step 3) {
            val a = vertexAt(indices[face])
            val b = vertexAt(indices[face + 1])
            val c = vertexAt(indices[face + 2])
            val grid = Array(columns + 1) { i ->
                val aj = a.copy().lerp(c, i.toDouble() / columns)
                val bj = b.copy().lerp(c, i.toDouble() / columns)
                val rows = columns - i
                Array(rows + 1) { j -> if (rows == 0) aj else aj.copy().lerp(bj, j.toDouble() / rows) }
            }
            for (i in 0 until columns) {
                for (j in 0 until (2 * (columns - i) - 1)) {
                    val k = j / 2
                    if (j % 2 == 0) appendTriangle(grid[i][k + 1], grid[i + 1][k], grid[i][k])
                    else appendTriangle(grid[i][k + 1], grid[i + 1][k + 1], grid[i + 1][k])
                }
            }
        }
        setAttribute("position", Float32BufferAttribute(positions, 3))
        setAttribute("normal", Float32BufferAttribute(normals, 3))
        setAttribute("uv", Float32BufferAttribute(uvs, 2))
    }
}
