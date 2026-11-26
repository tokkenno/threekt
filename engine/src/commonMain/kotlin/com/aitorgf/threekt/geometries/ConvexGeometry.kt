package com.aitorgf.threekt.geometries

import com.aitorgf.threekt.core.BufferGeometry
import com.aitorgf.threekt.core.Float32BufferAttribute
import com.aitorgf.threekt.math.Vector3
import kotlin.js.JsExport
import kotlin.math.abs

/**
 * Grows a tetrahedron around noncoplanar points using the Quickhull horizon operation. Scanning the
 * live faces for each point can change the triangulation and order compared with Three.js.
 */
@JsExport
class ConvexGeometry(points: Array<Vector3>) : BufferGeometry() {
    private class Face(val a: Int, val b: Int, val c: Int, val normal: Vector3, val offset: Double)

    init {
        require(points.size >= 4)
        val indices = points.indices
        val a = indices.minBy { points[it].x }
        val b = indices.maxBy { points[it].distanceToSquared(points[a]) }
        val axis = points[b].copy().sub(points[a])
        val scratch = Vector3()
        val c = indices.maxBy { scratch.subVectors(points[it], points[a]).cross(axis).lengthSq() }
        val normal = points[c].copy().sub(points[a]).cross(axis).normalize()
        val d = indices.maxBy { abs(scratch.subVectors(points[it], points[a]).dot(normal)) }
        val epsilon = axis.length() * 1e-10
        require(abs(scratch.subVectors(points[d], points[a]).dot(normal)) > epsilon) {
            "ConvexGeometry requires four noncoplanar points."
        }
        val center = points[a].copy().add(points[b]).add(points[c]).add(points[d]).multiplyScalar(0.25)
        fun face(i: Int, j: Int, k: Int): Face {
            val n = points[j].copy().sub(points[i]).cross(points[k].copy().sub(points[i])).normalize()
            val offset = n.dot(points[i])
            return if (n.dot(center) <= offset) Face(i, j, k, n, offset)
            else Face(i, k, j, n.negate(), -offset)
        }

        val faces = mutableListOf(face(a, b, c), face(a, d, b), face(b, d, c), face(c, d, a))
        for (index in indices) {
            val visible = faces.filter { it.normal.dot(points[index]) - it.offset > epsilon }
            if (visible.isEmpty()) continue
            val horizon = linkedSetOf<Pair<Int, Int>>()
            fun addEdge(start: Int, end: Int) {
                if (!horizon.remove(end to start)) horizon.add(start to end)
            }
            for (removed in visible) {
                addEdge(removed.a, removed.b)
                addEdge(removed.b, removed.c)
                addEdge(removed.c, removed.a)
            }
            faces.removeAll(visible.toSet())
            for ((start, end) in horizon) faces.add(face(start, end, index))
        }

        val positions = FloatArray(faces.size * 9)
        val normals = FloatArray(positions.size)
        fun store(array: FloatArray, offset: Int, vector: Vector3) {
            array[offset] = vector.x.toFloat()
            array[offset + 1] = vector.y.toFloat()
            array[offset + 2] = vector.z.toFloat()
        }
        for ((i, triangle) in faces.withIndex()) {
            val offset = i * 9
            store(positions, offset, points[triangle.a])
            store(positions, offset + 3, points[triangle.b])
            store(positions, offset + 6, points[triangle.c])
            for (vertex in 0..2) store(normals, offset + vertex * 3, triangle.normal)
        }
        setAttribute("position", Float32BufferAttribute(positions, 3))
        setAttribute("normal", Float32BufferAttribute(normals, 3))
    }
}
