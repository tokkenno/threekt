package com.aitorgf.threekt.geometries

import com.aitorgf.threekt.core.BufferGeometry
import com.aitorgf.threekt.core.Float32BufferAttribute
import kotlin.js.JsExport

/** A centered box subdivided along each axis. */
@JsExport
class BoxGeometry(
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
        val vertexCount = 2 * ((widthSegments + 1) * (heightSegments + 1) +
            (widthSegments + 1) * (depthSegments + 1) + (heightSegments + 1) * (depthSegments + 1))
        val positions = FloatArray(vertexCount * 3)
        val normals = FloatArray(positions.size)
        val uvs = FloatArray(vertexCount * 2)
        val indices = IntArray(12 * (widthSegments * heightSegments + widthSegments * depthSegments + heightSegments * depthSegments))
        var firstVertex = 0
        var index = 0

        fun addFace(u: Int, v: Int, normalAxis: Int, uDirection: Int, vDirection: Int, side: Int) {
            val columns = segments[u]
            val rows = segments[v]
            val faceDepth = halfSize[normalAxis] * side
            for (row in 0..rows) {
                for (column in 0..columns) {
                    val vertex = firstVertex + row * (columns + 1) + column
                    positions[vertex * 3 + u] = (halfSize[u] * (2.0 * column / columns - 1) * uDirection).toFloat()
                    positions[vertex * 3 + v] = (halfSize[v] * (2.0 * row / rows - 1) * vDirection).toFloat()
                    positions[vertex * 3 + normalAxis] = faceDepth.toFloat()
                    normals[vertex * 3 + normalAxis] = if (faceDepth > 0.0) 1f else -1f
                    uvs[vertex * 2] = column.toFloat() / columns
                    uvs[vertex * 2 + 1] = 1f - row.toFloat() / rows
                }
            }
            for (row in 0 until rows) {
                for (column in 0 until columns) {
                    val a = firstVertex + row * (columns + 1) + column
                    val b = a + columns + 1
                    indices[index++] = a
                    indices[index++] = b
                    indices[index++] = a + 1
                    indices[index++] = b
                    indices[index++] = b + 1
                    indices[index++] = a + 1
                }
            }
            firstVertex += (columns + 1) * (rows + 1)
        }

        addFace(2, 1, 0, -1, -1, 1)
        addFace(2, 1, 0, 1, -1, -1)
        addFace(0, 2, 1, 1, 1, 1)
        addFace(0, 2, 1, 1, -1, -1)
        addFace(0, 1, 2, 1, -1, 1)
        addFace(0, 1, 2, -1, -1, -1)

        setIndex(indices)
        setAttribute("position", Float32BufferAttribute(positions, 3))
        setAttribute("normal", Float32BufferAttribute(normals, 3))
        setAttribute("uv", Float32BufferAttribute(uvs, 2))
    }
}
