package com.aitorgf.threekt.geometries

import com.aitorgf.threekt.core.BufferGeometry
import com.aitorgf.threekt.core.Float32BufferAttribute
import com.aitorgf.threekt.math.Vector3
import kotlin.js.JsExport
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Indexed sphere or sphere section with position, normal, and UV attributes. */
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
        val width = maxOf(3, widthSegments)
        val height = maxOf(2, heightSegments)
        val thetaEnd = minOf(thetaStart + thetaLength, PI)
        val count = (width + 1) * (height + 1)
        val positions = FloatArray(count * 3)
        val normals = FloatArray(count * 3)
        val uvs = FloatArray(count * 2)
        val normal = Vector3()
        for (iy in 0..height) {
            val v = iy.toDouble() / height
            val theta = thetaStart + v * thetaLength
            val y = radius * cos(theta)
            val ringRadius = sqrt(radius * radius - y * y)
            val uOffset = when {
                iy == 0 && thetaStart == 0.0 -> 0.5 / width
                iy == height && thetaEnd == PI -> -0.5 / width
                else -> 0.0
            }
            for (ix in 0..width) {
                val u = ix.toDouble() / width
                val phi = phiStart + u * phiLength
                val x = -ringRadius * cos(phi)
                val z = ringRadius * sin(phi)
                val index = iy * (width + 1) + ix
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
        val indices = buildList {
            for (iy in 0 until height) {
                for (ix in 0 until width) {
                    val b = iy * (width + 1) + ix
                    val a = b + 1
                    val c = b + width + 1
                    val d = c + 1
                    if (iy != 0 || thetaStart > 0) {
                        add(a)
                        add(b)
                        add(d)
                    }
                    if (iy != height - 1 || thetaEnd < PI) {
                        add(b)
                        add(c)
                        add(d)
                    }
                }
            }
        }
        setIndex(indices.toIntArray())
        setAttribute("position", Float32BufferAttribute(positions, 3))
        setAttribute("normal", Float32BufferAttribute(normals, 3))
        setAttribute("uv", Float32BufferAttribute(uvs, 2))
    }
}
