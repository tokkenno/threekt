package com.aitorgf.threekt.geometries

import com.aitorgf.threekt.core.BufferGeometry
import com.aitorgf.threekt.core.Float32BufferAttribute
import com.aitorgf.threekt.math.Vector3
import kotlin.js.JsExport
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** An indexed sphere or spherical patch. Angular parameters are in radians. */
@JsExport
class SphereGeometry(
    radius: Double = 1.0,
    widthSegments: Int = 32,
    heightSegments: Int = 16,
    phiStart: Double = 0.0,
    phiLength: Double = 2 * PI,
    thetaStart: Double = 0.0,
    thetaLength: Double = PI,
) : BufferGeometry() {
    init {
        val width = widthSegments.coerceAtLeast(3)
        val height = heightSegments.coerceAtLeast(2)
        val thetaEnd = minOf(thetaStart + thetaLength, PI)
        val columns = width + 1
        val vertexCount = columns * (height + 1)
        val positions = FloatArray(vertexCount * 3)
        val normals = FloatArray(vertexCount * 3)
        val uvs = FloatArray(vertexCount * 2)
        val normal = Vector3()

        for (row in 0..height) {
            val v = row.toDouble() / height
            val theta = thetaStart + v * thetaLength
            val y = radius * cos(theta)
            val ringRadius = sqrt(radius * radius - y * y)
            val uOffset = when {
                row == 0 && thetaStart == 0.0 -> 0.5 / width
                row == height && thetaEnd == PI -> -0.5 / width
                else -> 0.0
            }
            for (column in 0..width) {
                val u = column.toDouble() / width
                val phi = phiStart + u * phiLength
                val x = -ringRadius * cos(phi)
                val z = ringRadius * sin(phi)
                val index = row * columns + column
                positions[index * 3] = x.toFloat()
                positions[index * 3 + 1] = y.toFloat()
                positions[index * 3 + 2] = z.toFloat()
                normal.set(x, y, z).normalize()
                normals[index * 3] = normal.x.toFloat()
                normals[index * 3 + 1] = normal.y.toFloat()
                normals[index * 3 + 2] = normal.z.toFloat()
                uvs[index * 2] = (u + uOffset).toFloat()
                uvs[index * 2 + 1] = (1 - v).toFloat()
            }
        }

        val indices = IntArray(width * height * 6)
        var offset = 0
        for (row in 0 until height) {
            for (column in 0 until width) {
                val b = row * columns + column
                val a = b + 1
                val c = b + columns
                val d = c + 1
                if (row != 0 || thetaStart > 0.0) {
                    indices[offset++] = a
                    indices[offset++] = b
                    indices[offset++] = d
                }
                if (row != height - 1 || thetaEnd < PI) {
                    indices[offset++] = b
                    indices[offset++] = c
                    indices[offset++] = d
                }
            }
        }
        setIndex(indices.copyOf(offset))
        setAttribute("position", Float32BufferAttribute(positions, 3))
        setAttribute("normal", Float32BufferAttribute(normals, 3))
        setAttribute("uv", Float32BufferAttribute(uvs, 2))
    }
}
