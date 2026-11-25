package com.aitorgf.threekt.renderers.webgl

/** Expands each triangle into three line segments in the order used by Three.js. */
internal class WebGLGeometries {
    private var wireframeIndices = IntArray(0)

    /** Reuses the output array and rebuilds it since callers can mutate index arrays directly. */
    fun getWireframeIndices(index: IntArray?, vertexCount: Int): IntArray {
        val triangleCount = (index?.size ?: vertexCount) / 3
        if (wireframeIndices.size != triangleCount * 6) wireframeIndices = IntArray(triangleCount * 6)

        for (triangle in 0 until triangleCount) {
            val first = triangle * 3
            val a = index?.get(first) ?: first
            val b = index?.get(first + 1) ?: first + 1
            val c = index?.get(first + 2) ?: first + 2
            val edge = triangle * 6
            wireframeIndices[edge] = a
            wireframeIndices[edge + 1] = b
            wireframeIndices[edge + 2] = b
            wireframeIndices[edge + 3] = c
            wireframeIndices[edge + 4] = c
            wireframeIndices[edge + 5] = a
        }
        return wireframeIndices
    }
}
