package com.aitorgf.threekt.geometries

import com.aitorgf.threekt.core.BufferGeometry
import com.aitorgf.threekt.core.Float32BufferAttribute
import com.aitorgf.threekt.math.Vector3
import kotlin.js.JsExport
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * A torus in the XY plane around the Z axis, as Three.js `TorusGeometry`. [radius] is measured to the
 * center of the [tube]. [arc] limits the sweep around the Z axis and [thetaStart] and [thetaLength] the
 * sweep around the tube. Angles are in radians.
 */
@JsExport
class TorusGeometry(
    radius: Double = 1.0,
    tube: Double = 0.4,
    radialSegments: Int = 12,
    tubularSegments: Int = 48,
    arc: Double = 2 * PI,
    thetaStart: Double = 0.0,
    thetaLength: Double = 2 * PI,
) : BufferGeometry() {
    init {
        val indices = mutableListOf<Int>()
        val vertices = mutableListOf<Float>()
        val normals = mutableListOf<Float>()
        val uvs = mutableListOf<Float>()
        val center = Vector3()
        val vertex = Vector3()
        val normal = Vector3()
        for (j in 0..radialSegments) {
            val v = thetaStart + j.toDouble() / radialSegments * thetaLength
            for (i in 0..tubularSegments) {
                val u = i.toDouble() / tubularSegments * arc
                vertex.set((radius + tube * cos(v)) * cos(u), (radius + tube * cos(v)) * sin(u), tube * sin(v))
                vertices.add(vertex.x.toFloat()); vertices.add(vertex.y.toFloat()); vertices.add(vertex.z.toFloat())
                center.set(radius * cos(u), radius * sin(u), 0.0)
                normal.subVectors(vertex, center).normalize()
                normals.add(normal.x.toFloat()); normals.add(normal.y.toFloat()); normals.add(normal.z.toFloat())
                uvs.add((i.toDouble() / tubularSegments).toFloat())
                uvs.add((j.toDouble() / radialSegments).toFloat())
            }
        }
        for (j in 1..radialSegments) {
            for (i in 1..tubularSegments) {
                val a = (tubularSegments + 1) * j + i - 1
                val b = (tubularSegments + 1) * (j - 1) + i - 1
                val c = (tubularSegments + 1) * (j - 1) + i
                val d = (tubularSegments + 1) * j + i
                indices.addAll(listOf(a, b, d, b, c, d))
            }
        }
        setIndex(indices.toIntArray())
        setAttribute("position", Float32BufferAttribute(vertices.toFloatArray(), 3))
        setAttribute("normal", Float32BufferAttribute(normals.toFloatArray(), 3))
        setAttribute("uv", Float32BufferAttribute(uvs.toFloatArray(), 2))
    }
}
