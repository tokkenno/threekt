package com.aitorgf.threekt.geometries

import com.aitorgf.threekt.core.BufferGeometry
import com.aitorgf.threekt.core.Float32BufferAttribute
import kotlin.js.JsExport

/** A centered box with configurable subdivisions on each axis. Three.js calls this class `BoxGeometry`. */
@JsExport
class Box(
    width: Double = 1.0,
    height: Double = 1.0,
    depth: Double = 1.0,
    widthSegments: Int = 1,
    heightSegments: Int = 1,
    depthSegments: Int = 1,
) : BufferGeometry() {
    init {
        val halfSize = doubleArrayOf(width / 2.0, height / 2.0, depth / 2.0)
        val segments = intArrayOf(widthSegments, heightSegments, depthSegments)
        require(segments.all { it > 0 })
        val positions = mutableListOf<Float>()
        val normals = mutableListOf<Float>()
        val uvs = mutableListOf<Float>()
        val indices = mutableListOf<Int>()
        val vertex = FloatArray(3)
        val normal = FloatArray(3)

        fun addFace(u: Int, v: Int, normalAxis: Int, uDirection: Int, vDirection: Int, side: Int) {
            val firstVertex = positions.size / 3
            val gridX = segments[u]
            val gridY = segments[v]
            val rowSize = gridX + 1
            val faceDepth = halfSize[normalAxis] * side
            vertex[normalAxis] = faceDepth.toFloat()
            normal.fill(0f)
            normal[normalAxis] = if (faceDepth > 0) 1f else -1f
            for (row in 0..gridY) {
                for (column in 0..gridX) {
                    val s = column.toDouble() / gridX
                    val t = row.toDouble() / gridY
                    vertex[u] = (halfSize[u] * (2 * s - 1) * uDirection).toFloat()
                    vertex[v] = (halfSize[v] * (2 * t - 1) * vDirection).toFloat()
                    for (component in 0..2) {
                        positions.add(vertex[component])
                        normals.add(normal[component])
                    }
                    uvs.add(s.toFloat())
                    uvs.add((1 - t).toFloat())
                }
            }
            for (row in 0 until gridY) {
                for (column in 0 until gridX) {
                    val a = firstVertex + row * rowSize + column
                    val b = a + rowSize
                    val c = b + 1
                    val d = a + 1
                    indices.add(a)
                    indices.add(b)
                    indices.add(d)
                    indices.add(b)
                    indices.add(c)
                    indices.add(d)
                }
            }
        }

        addFace(2, 1, 0, -1, -1, 1)
        addFace(2, 1, 0, 1, -1, -1)
        addFace(0, 2, 1, 1, 1, 1)
        addFace(0, 2, 1, 1, -1, -1)
        addFace(0, 1, 2, 1, -1, 1)
        addFace(0, 1, 2, -1, -1, -1)

        setIndex(indices.toIntArray())
        setAttribute("position", Float32BufferAttribute(positions.toFloatArray(), 3))
        setAttribute("normal", Float32BufferAttribute(normals.toFloatArray(), 3))
        setAttribute("uv", Float32BufferAttribute(uvs.toFloatArray(), 2))
    }
}
