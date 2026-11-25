package com.aitorgf.threekt.geometries

import com.aitorgf.threekt.core.BufferGeometry
import com.aitorgf.threekt.core.Float32BufferAttribute
import kotlin.js.JsExport
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/** A Y-aligned cylinder or truncated cone with optional caps. Angles are in radians. */
@JsExport
class CylinderGeometry(
    radiusTop: Double = 1.0,
    radiusBottom: Double = 1.0,
    height: Double = 1.0,
    radialSegments: Int = 32,
    heightSegments: Int = 1,
    openEnded: Boolean = false,
    thetaStart: Double = 0.0,
    thetaLength: Double = 2 * PI,
) : BufferGeometry() {
    init {
        require(radialSegments >= 3 && heightSegments > 0 && height > 0 && radiusTop >= 0 && radiusBottom >= 0)
        val positions = mutableListOf<Float>()
        val normals = mutableListOf<Float>()
        val uvs = mutableListOf<Float>()
        val indices = mutableListOf<Int>()
        val half = height / 2
        val slope = (radiusBottom - radiusTop) / height
        val normalScale = 1 / hypot(1.0, slope)
        val columns = radialSegments + 1

        fun vertex(x: Double, y: Double, z: Double, nx: Double, ny: Double, nz: Double, u: Double, v: Double) {
            positions.add(x.toFloat())
            positions.add(y.toFloat())
            positions.add(z.toFloat())
            normals.add(nx.toFloat())
            normals.add(ny.toFloat())
            normals.add(nz.toFloat())
            uvs.add(u.toFloat())
            uvs.add(v.toFloat())
        }

        for (row in 0..heightSegments) {
            val v = row.toDouble() / heightSegments
            val radius = radiusTop + (radiusBottom - radiusTop) * v
            for (column in 0..radialSegments) {
                val u = column.toDouble() / radialSegments
                val angle = thetaStart + u * thetaLength
                val x = sin(angle)
                val z = cos(angle)
                vertex(radius * x, half - v * height, radius * z, x * normalScale, slope * normalScale, z * normalScale, u, 1 - v)
            }
        }
        for (row in 0 until heightSegments) {
            for (column in 0 until radialSegments) {
                val b = row * columns + column
                val a = b + 1
                val c = b + columns
                val d = c + 1
                if (radiusTop > 0 || row != 0) indices.addAll(listOf(a, b, d))
                if (radiusBottom > 0 || row != heightSegments - 1) indices.addAll(listOf(b, c, d))
            }
        }

        fun cap(radius: Double, sign: Double) {
            val center = positions.size / 3
            repeat(radialSegments) { vertex(0.0, sign * half, 0.0, 0.0, sign, 0.0, 0.5, 0.5) }
            val ring = positions.size / 3
            for (column in 0..radialSegments) {
                val angle = thetaStart + column.toDouble() / radialSegments * thetaLength
                val x = sin(angle)
                val z = cos(angle)
                vertex(x * radius, sign * half, z * radius, 0.0, sign, 0.0, z * 0.5 + 0.5, x * 0.5 * sign + 0.5)
            }
            for (column in 0 until radialSegments) {
                val i = ring + column
                if (sign > 0) indices.addAll(listOf(i, i + 1, center + column))
                else indices.addAll(listOf(i + 1, i, center + column))
            }
        }
        if (!openEnded && radiusTop > 0) cap(radiusTop, 1.0)
        if (!openEnded && radiusBottom > 0) cap(radiusBottom, -1.0)

        setIndex(indices.toIntArray())
        setAttribute("position", Float32BufferAttribute(positions.toFloatArray(), 3))
        setAttribute("normal", Float32BufferAttribute(normals.toFloatArray(), 3))
        setAttribute("uv", Float32BufferAttribute(uvs.toFloatArray(), 2))
    }
}
