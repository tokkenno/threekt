package com.aitorgf.threekt.geometries

import com.aitorgf.threekt.core.BufferGeometry
import com.aitorgf.threekt.core.Float32BufferAttribute
import com.aitorgf.threekt.math.Vector3
import kotlin.js.JsExport
import kotlin.math.cos
import kotlin.math.sin

/**
 * A surface sampled from [func] over `u, v ∈ [0, 1]` with [slices] × [stacks] quads, as the Three.js
 * addon `ParametricGeometry`. [func] writes the point for `(u, v)` into its third argument. Normals come
 * from finite differences of [func]. [ParametricFunctions] holds the upstream sample surfaces.
 */
@JsExport
class ParametricGeometry(
    func: (u: Double, v: Double, target: Vector3) -> Unit = { u, v, target -> target.set(u, v, cos(u) * sin(v)) },
    slices: Int = 8,
    stacks: Int = 8,
) : BufferGeometry() {
    init {
        val indices = mutableListOf<Int>()
        val vertices = mutableListOf<Float>()
        val normals = mutableListOf<Float>()
        val uvs = mutableListOf<Float>()
        val eps = 0.00001
        val normal = Vector3()
        val p0 = Vector3()
        val p1 = Vector3()
        val pu = Vector3()
        val pv = Vector3()
        val sliceCount = slices + 1
        for (i in 0..stacks) {
            val v = i.toDouble() / stacks
            for (j in 0..slices) {
                val u = j.toDouble() / slices
                func(u, v, p0)
                vertices.add(p0.x.toFloat()); vertices.add(p0.y.toFloat()); vertices.add(p0.z.toFloat())
                if (u - eps >= 0) {
                    func(u - eps, v, p1)
                    pu.subVectors(p0, p1)
                } else {
                    func(u + eps, v, p1)
                    pu.subVectors(p1, p0)
                }
                if (v - eps >= 0) {
                    func(u, v - eps, p1)
                    pv.subVectors(p0, p1)
                } else {
                    func(u, v + eps, p1)
                    pv.subVectors(p1, p0)
                }
                normal.crossVectors(pu, pv).normalize()
                normals.add(normal.x.toFloat()); normals.add(normal.y.toFloat()); normals.add(normal.z.toFloat())
                uvs.add(u.toFloat()); uvs.add(v.toFloat())
            }
        }
        for (i in 0 until stacks) {
            for (j in 0 until slices) {
                val a = i * sliceCount + j
                val b = i * sliceCount + j + 1
                val c = (i + 1) * sliceCount + j + 1
                val d = (i + 1) * sliceCount + j
                indices.addAll(listOf(a, b, d, b, c, d))
            }
        }
        setIndex(indices.toIntArray())
        setAttribute("position", Float32BufferAttribute(vertices.toFloatArray(), 3))
        setAttribute("normal", Float32BufferAttribute(normals.toFloatArray(), 3))
        setAttribute("uv", Float32BufferAttribute(uvs.toFloatArray(), 2))
    }
}
