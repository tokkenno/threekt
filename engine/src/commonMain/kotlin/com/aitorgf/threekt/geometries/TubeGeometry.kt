package com.aitorgf.threekt.geometries

import com.aitorgf.threekt.core.BufferGeometry
import com.aitorgf.threekt.core.Float32BufferAttribute
import com.aitorgf.threekt.extras.core.Curve
import com.aitorgf.threekt.extras.curves.QuadraticBezierCurve3
import com.aitorgf.threekt.math.Vector3
import kotlin.js.JsExport
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * A tube of [radius] around a 3D [path], as Three.js `TubeGeometry`. Rings of [radialSegments] vertices are
 * placed at [tubularSegments] + 1 equal arc-length steps and oriented by the path's Frenet frames, which
 * are kept in [tangents], [normals] and [binormals]. A [closed] tube reuses the first ring at the end.
 */
@JsExport
class TubeGeometry(
    /** Curve the tube follows. Retained. */
    val path: Curve<Vector3> = QuadraticBezierCurve3(Vector3(-1.0, -1.0, 0.0), Vector3(-1.0, 1.0, 0.0), Vector3(1.0, 1.0, 0.0)),
    val tubularSegments: Int = 64,
    val radius: Double = 1.0,
    val radialSegments: Int = 8,
    val closed: Boolean = false,
) : BufferGeometry() {
    private val frames = path.computeFrenetFrames(tubularSegments, closed)

    /** Unit tangent of the path at each ring. */
    val tangents: Array<Vector3> get() = frames.tangents

    /** Normal of the Frenet frame at each ring. */
    val normals: Array<Vector3> get() = frames.normals

    /** Binormal of the Frenet frame at each ring. */
    val binormals: Array<Vector3> get() = frames.binormals

    init {
        val vertex = Vector3()
        val normal = Vector3()
        var p = Vector3()
        val vertices = mutableListOf<Float>()
        val normalValues = mutableListOf<Float>()
        val uvs = mutableListOf<Float>()
        val indices = mutableListOf<Int>()

        fun generateSegment(i: Int) {
            // The point on the path.
            p = path.getPointAt(i.toDouble() / tubularSegments, p)
            val n = frames.normals[i]
            val b = frames.binormals[i]
            for (j in 0..radialSegments) {
                val v = j.toDouble() / radialSegments * PI * 2
                val sin = sin(v)
                val cos = -cos(v)
                normal.set(cos * n.x + sin * b.x, cos * n.y + sin * b.y, cos * n.z + sin * b.z).normalize()
                normalValues.add(normal.x.toFloat()); normalValues.add(normal.y.toFloat()); normalValues.add(normal.z.toFloat())
                vertex.set(p.x + radius * normal.x, p.y + radius * normal.y, p.z + radius * normal.z)
                vertices.add(vertex.x.toFloat()); vertices.add(vertex.y.toFloat()); vertices.add(vertex.z.toFloat())
            }
        }

        for (i in 0 until tubularSegments) generateSegment(i)
        // An open tube adds a last ring at the end of the path; a closed one repeats the first ring.
        generateSegment(if (!closed) tubularSegments else 0)
        for (i in 0..tubularSegments) {
            for (j in 0..radialSegments) {
                uvs.add((i.toDouble() / tubularSegments).toFloat())
                uvs.add((j.toDouble() / radialSegments).toFloat())
            }
        }
        for (j in 1..tubularSegments) {
            for (i in 1..radialSegments) {
                val a = (radialSegments + 1) * (j - 1) + (i - 1)
                val b = (radialSegments + 1) * j + (i - 1)
                val c = (radialSegments + 1) * j + i
                val d = (radialSegments + 1) * (j - 1) + i
                indices.addAll(listOf(a, b, d, b, c, d))
            }
        }
        setIndex(indices.toIntArray())
        setAttribute("position", Float32BufferAttribute(vertices.toFloatArray(), 3))
        setAttribute("normal", Float32BufferAttribute(normalValues.toFloatArray(), 3))
        setAttribute("uv", Float32BufferAttribute(uvs.toFloatArray(), 2))
    }
}
