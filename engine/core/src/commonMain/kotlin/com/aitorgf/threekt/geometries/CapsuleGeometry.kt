package com.aitorgf.threekt.geometries

import com.aitorgf.threekt.core.BufferGeometry
import com.aitorgf.threekt.core.Float32BufferAttribute
import com.aitorgf.threekt.math.Vector3
import kotlin.js.JsExport
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * A Y-aligned cylinder of [height] capped by two hemispheres of [radius], as Three.js `CapsuleGeometry`.
 * The total height is `height + 2 * radius`. UVs run along the profile's arc length.
 */
@JsExport
class CapsuleGeometry(
    radius: Double = 1.0,
    height: Double = 1.0,
    capSegments: Int = 4,
    radialSegments: Int = 8,
    heightSegments: Int = 1,
) : BufferGeometry() {
    init {
        val length = maxOf(0.0, height)
        val caps = maxOf(1, capSegments)
        val radial = maxOf(3, radialSegments)
        val rows = maxOf(1, heightSegments)
        val indices = mutableListOf<Int>()
        val vertices = mutableListOf<Float>()
        val normals = mutableListOf<Float>()
        val uvs = mutableListOf<Float>()
        val halfHeight = length / 2
        val capArcLength = (PI / 2) * radius
        val totalArcLength = 2 * capArcLength + length
        val numVerticalSegments = caps * 2 + rows
        val verticesPerRow = radial + 1
        val normal = Vector3()
        for (iy in 0..numVerticalSegments) {
            val profileY: Double
            val profileRadius: Double
            val normalYComponent: Double
            val currentArcLength: Double
            if (iy <= caps) {
                val progress = iy.toDouble() / caps
                val angle = progress * PI / 2
                profileY = -halfHeight - radius * cos(angle)
                profileRadius = radius * sin(angle)
                normalYComponent = -radius * cos(angle)
                currentArcLength = progress * capArcLength
            } else if (iy <= caps + rows) {
                val progress = (iy - caps).toDouble() / rows
                profileY = -halfHeight + progress * length
                profileRadius = radius
                normalYComponent = 0.0
                currentArcLength = capArcLength + progress * length
            } else {
                val progress = (iy - caps - rows).toDouble() / caps
                val angle = progress * PI / 2
                profileY = halfHeight + radius * sin(angle)
                profileRadius = radius * cos(angle)
                normalYComponent = radius * sin(angle)
                currentArcLength = capArcLength + length + progress * capArcLength
            }
            val v = (currentArcLength / totalArcLength).coerceIn(0.0, 1.0)
            val uOffset = when (iy) {
                0 -> 0.5 / radial
                numVerticalSegments -> -0.5 / radial
                else -> 0.0
            }
            for (ix in 0..radial) {
                val u = ix.toDouble() / radial
                val theta = u * PI * 2
                val x = -profileRadius * cos(theta)
                val z = profileRadius * sin(theta)
                vertices.add(x.toFloat()); vertices.add(profileY.toFloat()); vertices.add(z.toFloat())
                normal.set(x, normalYComponent, z).normalize()
                normals.add(normal.x.toFloat()); normals.add(normal.y.toFloat()); normals.add(normal.z.toFloat())
                uvs.add((u + uOffset).toFloat()); uvs.add(v.toFloat())
            }
            if (iy > 0) {
                val prevIndexRow = (iy - 1) * verticesPerRow
                for (ix in 0 until radial) {
                    val i1 = prevIndexRow + ix
                    val i2 = prevIndexRow + ix + 1
                    val i3 = iy * verticesPerRow + ix
                    val i4 = iy * verticesPerRow + ix + 1
                    indices.addAll(listOf(i1, i2, i3, i2, i4, i3))
                }
            }
        }
        setIndex(indices.toIntArray())
        setAttribute("position", Float32BufferAttribute(vertices.toFloatArray(), 3))
        setAttribute("normal", Float32BufferAttribute(normals.toFloatArray(), 3))
        setAttribute("uv", Float32BufferAttribute(uvs.toFloatArray(), 2))
    }
}
