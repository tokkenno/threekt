package com.aitorgf.threekt.geometries

import com.aitorgf.threekt.core.BufferGeometry
import com.aitorgf.threekt.core.Float32BufferAttribute
import kotlin.js.JsExport
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * A flat ring, or ring sector, in the XY plane with a +Z normal, as Three.js `RingGeometry`. Rows of
 * vertices go from [innerRadius] to [outerRadius]. Angles are in radians.
 */
@JsExport
class RingGeometry(
    innerRadius: Double = 0.5,
    outerRadius: Double = 1.0,
    thetaSegments: Int = 32,
    phiSegments: Int = 1,
    thetaStart: Double = 0.0,
    thetaLength: Double = 2 * PI,
) : BufferGeometry() {
    init {
        val thetaCount = maxOf(3, thetaSegments)
        val phiCount = maxOf(1, phiSegments)
        val indices = mutableListOf<Int>()
        val vertices = mutableListOf<Float>()
        val normals = mutableListOf<Float>()
        val uvs = mutableListOf<Float>()
        var radius = innerRadius
        val radiusStep = (outerRadius - innerRadius) / phiCount
        for (j in 0..phiCount) {
            for (i in 0..thetaCount) {
                val segment = thetaStart + i.toDouble() / thetaCount * thetaLength
                val x = radius * cos(segment)
                val y = radius * sin(segment)
                vertices.add(x.toFloat()); vertices.add(y.toFloat()); vertices.add(0f)
                normals.add(0f); normals.add(0f); normals.add(1f)
                uvs.add(((x / outerRadius + 1) / 2).toFloat())
                uvs.add(((y / outerRadius + 1) / 2).toFloat())
            }
            radius += radiusStep
        }
        for (j in 0 until phiCount) {
            val thetaSegmentLevel = j * (thetaCount + 1)
            for (i in 0 until thetaCount) {
                val a = i + thetaSegmentLevel
                val b = a + thetaCount + 1
                val c = a + thetaCount + 2
                val d = a + 1
                indices.addAll(listOf(a, b, d, b, c, d))
            }
        }
        setIndex(indices.toIntArray())
        setAttribute("position", Float32BufferAttribute(vertices.toFloatArray(), 3))
        setAttribute("normal", Float32BufferAttribute(normals.toFloatArray(), 3))
        setAttribute("uv", Float32BufferAttribute(uvs.toFloatArray(), 2))
    }
}
