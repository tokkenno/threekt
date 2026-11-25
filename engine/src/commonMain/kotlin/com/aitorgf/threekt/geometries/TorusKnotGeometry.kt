package com.aitorgf.threekt.geometries

import com.aitorgf.threekt.core.BufferGeometry
import com.aitorgf.threekt.core.Float32BufferAttribute
import com.aitorgf.threekt.math.Vector3
import kotlin.js.JsExport
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * A tube that winds [p] times around the axis of rotational symmetry and [q] times around a circle in
 * the torus interior, as Three.js `TorusKnotGeometry`.
 */
@JsExport
class TorusKnotGeometry(
    radius: Double = 1.0,
    tube: Double = 0.4,
    tubularSegments: Int = 64,
    radialSegments: Int = 8,
    p: Int = 2,
    q: Int = 3,
) : BufferGeometry() {
    init {
        val indices = mutableListOf<Int>()
        val vertices = mutableListOf<Float>()
        val normals = mutableListOf<Float>()
        val uvs = mutableListOf<Float>()
        val vertex = Vector3()
        val normal = Vector3()
        val p1 = Vector3()
        val p2 = Vector3()
        val b = Vector3()
        val t = Vector3()
        val n = Vector3()

        fun calculatePositionOnCurve(u: Double, position: Vector3) {
            val quOverP = q.toDouble() / p * u
            val cs = cos(quOverP)
            position.set(radius * (2 + cs) * 0.5 * cos(u), radius * (2 + cs) * sin(u) * 0.5, radius * sin(quOverP) * 0.5)
        }

        for (i in 0..tubularSegments) {
            val u = i.toDouble() / tubularSegments * p * PI * 2
            calculatePositionOnCurve(u, p1)
            calculatePositionOnCurve(u + 0.01, p2)
            t.subVectors(p2, p1)
            n.addVectors(p2, p1)
            b.crossVectors(t, n)
            n.crossVectors(b, t)
            b.normalize()
            n.normalize()
            for (j in 0..radialSegments) {
                val v = j.toDouble() / radialSegments * PI * 2
                val cx = -tube * cos(v)
                val cy = tube * sin(v)
                vertex.set(p1.x + (cx * n.x + cy * b.x), p1.y + (cx * n.y + cy * b.y), p1.z + (cx * n.z + cy * b.z))
                vertices.add(vertex.x.toFloat()); vertices.add(vertex.y.toFloat()); vertices.add(vertex.z.toFloat())
                normal.subVectors(vertex, p1).normalize()
                normals.add(normal.x.toFloat()); normals.add(normal.y.toFloat()); normals.add(normal.z.toFloat())
                uvs.add((i.toDouble() / tubularSegments).toFloat())
                uvs.add((j.toDouble() / radialSegments).toFloat())
            }
        }
        for (j in 1..tubularSegments) {
            for (i in 1..radialSegments) {
                val a = (radialSegments + 1) * (j - 1) + (i - 1)
                val bIndex = (radialSegments + 1) * j + (i - 1)
                val c = (radialSegments + 1) * j + i
                val d = (radialSegments + 1) * (j - 1) + i
                indices.addAll(listOf(a, bIndex, d, bIndex, c, d))
            }
        }
        setIndex(indices.toIntArray())
        setAttribute("position", Float32BufferAttribute(vertices.toFloatArray(), 3))
        setAttribute("normal", Float32BufferAttribute(normals.toFloatArray(), 3))
        setAttribute("uv", Float32BufferAttribute(uvs.toFloatArray(), 2))
    }
}
