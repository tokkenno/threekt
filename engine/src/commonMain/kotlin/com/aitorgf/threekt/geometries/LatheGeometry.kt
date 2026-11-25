package com.aitorgf.threekt.geometries

import com.aitorgf.threekt.core.BufferGeometry
import com.aitorgf.threekt.core.Float32BufferAttribute
import com.aitorgf.threekt.math.Vector2
import com.aitorgf.threekt.math.Vector3
import kotlin.js.JsExport
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Revolves a profile of [points] in the XY plane around the Y axis, as Three.js `LatheGeometry`, for
 * vases and similar shapes. Each point's X is its distance from the axis. Normals are averaged from
 * the neighboring profile segments. [phiLength] is clamped to `[0, 2π]`.
 */
@JsExport
class LatheGeometry(
    points: Array<Vector2> = arrayOf(Vector2(0.0, -0.5), Vector2(0.5, 0.0), Vector2(0.0, 0.5)),
    segments: Int = 12,
    phiStart: Double = 0.0,
    phiLength: Double = 2 * PI,
) : BufferGeometry() {
    init {
        val sweep = phiLength.coerceIn(0.0, 2 * PI)
        val indices = mutableListOf<Int>()
        val vertices = mutableListOf<Float>()
        val uvs = mutableListOf<Float>()
        val initNormals = mutableListOf<Double>()
        val normals = mutableListOf<Float>()
        val inverseSegments = 1.0 / segments
        val normal = Vector3()
        val curNormal = Vector3()
        val prevNormal = Vector3()
        val last = points.size - 1
        for (j in 0..last) {
            when (j) {
                0 -> {
                    val dx = points[j + 1].x - points[j].x
                    val dy = points[j + 1].y - points[j].y
                    normal.set(dy, -dx, 0.0)
                    prevNormal.copy(normal)
                    normal.normalize()
                    initNormals.addAll(listOf(normal.x, normal.y, normal.z))
                }
                last -> {
                    prevNormal.normalize()
                    initNormals.addAll(listOf(prevNormal.x, prevNormal.y, prevNormal.z))
                }
                else -> {
                    val dx = points[j + 1].x - points[j].x
                    val dy = points[j + 1].y - points[j].y
                    normal.set(dy, -dx, 0.0)
                    curNormal.copy(normal)
                    normal.add(prevNormal).normalize()
                    initNormals.addAll(listOf(normal.x, normal.y, normal.z))
                    prevNormal.copy(curNormal)
                }
            }
        }
        for (i in 0..segments) {
            val phi = phiStart + i * inverseSegments * sweep
            val sin = sin(phi)
            val cos = cos(phi)
            for (j in 0..last) {
                vertices.add((points[j].x * sin).toFloat()); vertices.add(points[j].y.toFloat()); vertices.add((points[j].x * cos).toFloat())
                uvs.add((i.toDouble() / segments).toFloat()); uvs.add((j.toDouble() / last).toFloat())
                normals.add((initNormals[3 * j] * sin).toFloat())
                normals.add(initNormals[3 * j + 1].toFloat())
                normals.add((initNormals[3 * j] * cos).toFloat())
            }
        }
        for (i in 0 until segments) {
            for (j in 0 until last) {
                val base = j + i * points.size
                val a = base
                val b = base + points.size
                val c = base + points.size + 1
                val d = base + 1
                indices.addAll(listOf(a, b, d, c, d, b))
            }
        }
        setIndex(indices.toIntArray())
        setAttribute("position", Float32BufferAttribute(vertices.toFloatArray(), 3))
        setAttribute("uv", Float32BufferAttribute(uvs.toFloatArray(), 2))
        setAttribute("normal", Float32BufferAttribute(normals.toFloatArray(), 3))
    }
}
