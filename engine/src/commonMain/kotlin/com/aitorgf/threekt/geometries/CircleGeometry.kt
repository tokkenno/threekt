package com.aitorgf.threekt.geometries

import com.aitorgf.threekt.core.BufferGeometry
import com.aitorgf.threekt.core.Float32BufferAttribute
import kotlin.js.JsExport
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** A subdivided disk, or angular sector, in the XY plane with a +Z normal. */
@JsExport
class CircleGeometry(
    radius: Double = 1.0,
    segments: Int = 32,
    thetaStart: Double = 0.0,
    thetaLength: Double = 2 * PI,
) : BufferGeometry() {
    init {
        val divisions = segments.coerceAtLeast(3)
        val positions = FloatArray((divisions + 2) * 3)
        val normals = FloatArray(positions.size)
        val uvs = FloatArray((divisions + 2) * 2)
        val indices = IntArray(divisions * 3)
        normals[2] = 1f
        uvs[0] = 0.5f
        uvs[1] = 0.5f
        for (i in 0..divisions) {
            val theta = thetaStart + i.toDouble() / divisions * thetaLength
            val x = cos(theta)
            val y = sin(theta)
            val vertex = i + 1
            positions[vertex * 3] = (radius * x).toFloat()
            positions[vertex * 3 + 1] = (radius * y).toFloat()
            normals[vertex * 3 + 2] = 1f
            uvs[vertex * 2] = (0.5 + x * 0.5).toFloat()
            uvs[vertex * 2 + 1] = (0.5 + y * 0.5).toFloat()
            if (i < divisions) {
                indices[i * 3] = vertex
                indices[i * 3 + 1] = vertex + 1
                indices[i * 3 + 2] = 0
            }
        }
        setIndex(indices)
        setAttribute("position", Float32BufferAttribute(positions, 3))
        setAttribute("normal", Float32BufferAttribute(normals, 3))
        setAttribute("uv", Float32BufferAttribute(uvs, 2))
    }
}
