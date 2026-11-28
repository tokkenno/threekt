package com.aitorgf.threekt.geometries

import com.aitorgf.threekt.core.BufferGeometry
import com.aitorgf.threekt.core.Float32BufferAttribute
import kotlin.js.JsExport

/** An indexed, centered plane in XY with normals facing +Z and UV coordinates spanning the surface. */
@JsExport
class PlaneGeometry(
    width: Double = 1.0,
    height: Double = 1.0,
    widthSegments: Int = 1,
    heightSegments: Int = 1,
) : BufferGeometry() {
    init {
        require(widthSegments > 0 && heightSegments > 0)
        val columns = widthSegments + 1
        val vertices = columns * (heightSegments + 1)
        val positions = FloatArray(vertices * 3)
        val normals = FloatArray(vertices * 3)
        val uvs = FloatArray(vertices * 2)
        val indices = IntArray(widthSegments * heightSegments * 6)
        for (row in 0..heightSegments) {
            val y = row * (height / heightSegments) - height / 2
            for (column in 0..widthSegments) {
                val vertex = row * columns + column
                positions[vertex * 3] = (column * (width / widthSegments) - width / 2).toFloat()
                positions[vertex * 3 + 1] = -y.toFloat()
                normals[vertex * 3 + 2] = 1f
                uvs[vertex * 2] = column.toFloat() / widthSegments
                uvs[vertex * 2 + 1] = 1f - row.toFloat() / heightSegments
            }
        }
        var offset = 0
        for (row in 0 until heightSegments) {
            for (column in 0 until widthSegments) {
                val a = row * columns + column
                val b = a + columns
                val c = b + 1
                val d = a + 1
                indices[offset++] = a
                indices[offset++] = b
                indices[offset++] = d
                indices[offset++] = b
                indices[offset++] = c
                indices[offset++] = d
            }
        }
        setIndex(indices)
        setAttribute("position", Float32BufferAttribute(positions, 3))
        setAttribute("normal", Float32BufferAttribute(normals, 3))
        setAttribute("uv", Float32BufferAttribute(uvs, 2))
    }
}
