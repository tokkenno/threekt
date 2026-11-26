package com.aitorgf.threekt.geometries

import com.aitorgf.threekt.core.BufferGeometry
import com.aitorgf.threekt.core.Float32BufferAttribute
import com.aitorgf.threekt.math.Vector3
import kotlin.js.JsExport
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.round

/** Emits boundaries and edges whose adjacent normals differ by at least [thresholdAngle] degrees. */
@JsExport
class EdgesGeometry(geometry: BufferGeometry, thresholdAngle: Double = 1.0) : BufferGeometry() {
    init {
        setAttribute("position", Float32BufferAttribute(geometryEdges(geometry, cos(thresholdAngle * PI / 180)), 3))
    }
}

/** Like the upstream edge helpers, matches positions across duplicate vertices. */
internal fun geometryEdges(geometry: BufferGeometry, threshold: Double?): FloatArray {
    data class Edge(val a: Vector3, val b: Vector3, val normal: Vector3, var visible: Boolean)
    val position = geometry.getAttribute("position") ?: return FloatArray(0)
    val index = geometry.getIndex()
    val edges = linkedMapOf<Pair<String, String>, Edge>()
    val vertices = Array(3) { Vector3() }
    val normal = Vector3()
    val direction = Vector3()
    fun hash(point: Vector3) = "${round(point.x * 10000).toLong()},${round(point.y * 10000).toLong()},${round(point.z * 10000).toLong()}"
    for (slot in 0..(index?.count ?: position.count) - 3 step 3) {
        for (i in 0..2) vertices[i].fromBufferAttribute(position, index?.getX(slot + i)?.toInt() ?: slot + i)
        val hashes = vertices.map(::hash)
        if (hashes.toSet().size < 3) continue
        normal.subVectors(vertices[1], vertices[0]).cross(direction.subVectors(vertices[2], vertices[0])).normalize()
        for (i in 0..2) {
            val j = (i + 1) % 3
            val key = if (hashes[i] < hashes[j]) hashes[i] to hashes[j] else hashes[j] to hashes[i]
            val edge = edges[key]
            if (edge == null) edges[key] = Edge(vertices[i].copy(), vertices[j].copy(), normal.copy(), true)
            else if (threshold != null) edge.visible = edge.normal.dot(normal) <= threshold
        }
    }
    return edges.values.filter { it.visible }.flatMap { listOf(
        it.a.x.toFloat(), it.a.y.toFloat(), it.a.z.toFloat(), it.b.x.toFloat(), it.b.y.toFloat(), it.b.z.toFloat(),
    ) }.toFloatArray()
}
