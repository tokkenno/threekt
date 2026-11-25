package com.aitorgf.threekt.geometries

import com.aitorgf.threekt.core.BufferGeometry
import com.aitorgf.threekt.core.Float32BufferAttribute
import kotlin.js.JsExport

/** A centered box with one quad per face. Three.js calls this class `BoxGeometry`. */
@JsExport
class Box(
    width: Double = 1.0,
    height: Double = 1.0,
    depth: Double = 1.0,
) : BufferGeometry() {
    init {
        val halfSize = doubleArrayOf(width / 2.0, height / 2.0, depth / 2.0)
        val positions = FloatArray(24 * 3)
        val normals = FloatArray(24 * 3)
        val uvs = FloatArray(24 * 2)
        val indices = IntArray(36)
        var face = 0

        fun addFace(u: Int, v: Int, normalAxis: Int, uDirection: Int, vDirection: Int, side: Int) {
            val firstVertex = face * 4
            val faceDepth = halfSize[normalAxis] * side
            for (row in 0..1) {
                for (column in 0..1) {
                    val vertex = firstVertex + row * 2 + column
                    positions[vertex * 3 + u] = (halfSize[u] * (2 * column - 1) * uDirection).toFloat()
                    positions[vertex * 3 + v] = (halfSize[v] * (2 * row - 1) * vDirection).toFloat()
                    positions[vertex * 3 + normalAxis] = faceDepth.toFloat()
                    normals[vertex * 3 + normalAxis] = if (faceDepth > 0.0) 1f else -1f
                    uvs[vertex * 2] = column.toFloat()
                    uvs[vertex * 2 + 1] = (1 - row).toFloat()
                }
            }

            val start = face * 6
            indices[start] = firstVertex
            indices[start + 1] = firstVertex + 2
            indices[start + 2] = firstVertex + 1
            indices[start + 3] = firstVertex + 2
            indices[start + 4] = firstVertex + 3
            indices[start + 5] = firstVertex + 1
            face++
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
