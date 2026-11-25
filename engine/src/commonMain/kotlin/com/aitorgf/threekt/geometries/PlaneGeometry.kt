package com.aitorgf.threekt.geometries

import com.aitorgf.threekt.core.BufferGeometry
import com.aitorgf.threekt.core.Float32BufferAttribute
import kotlin.js.JsExport

/** Indexed plane in XY with normals facing +Z. Segment counts are clamped to at least one. */
@JsExport
class PlaneGeometry(
    width: Double = 1.0,
    height: Double = 1.0,
    widthSegments: Int = 1,
    heightSegments: Int = 1,
) : BufferGeometry() {
    init {
        val gridX = maxOf(widthSegments, 1)
        val gridY = maxOf(heightSegments, 1)
        val rowSize = gridX + 1
        val count = rowSize * (gridY + 1)
        val vertices = FloatArray(count * 3)
        val normals = FloatArray(count * 3)
        val uvs = FloatArray(count * 2)
        for (iy in 0..gridY) {
            val y = iy * (height / gridY) - height / 2
            for (ix in 0..gridX) {
                val x = ix * (width / gridX) - width / 2
                val vertex = iy * rowSize + ix
                vertices[vertex * 3] = x.toFloat()
                vertices[vertex * 3 + 1] = -y.toFloat()
                normals[vertex * 3 + 2] = 1f
                uvs[vertex * 2] = ix.toFloat() / gridX
                uvs[vertex * 2 + 1] = (1.0 - iy.toDouble() / gridY).toFloat()
            }
        }
        val indices = IntArray(gridX * gridY * 6)
        var index = 0
        for (iy in 0 until gridY) {
            for (ix in 0 until gridX) {
                val a = iy * rowSize + ix
                val b = a + rowSize
                val c = b + 1
                val d = a + 1
                indices[index++] = a
                indices[index++] = b
                indices[index++] = d
                indices[index++] = b
                indices[index++] = c
                indices[index++] = d
            }
        }
        setIndex(indices)
        setAttribute("position", Float32BufferAttribute(vertices, 3))
        setAttribute("normal", Float32BufferAttribute(normals, 3))
        setAttribute("uv", Float32BufferAttribute(uvs, 2))
    }
}
