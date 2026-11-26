package com.aitorgf.threekt.helpers

import com.aitorgf.threekt.core.BufferGeometry
import com.aitorgf.threekt.core.Float32BufferAttribute
import com.aitorgf.threekt.materials.LineBasicMaterial
import com.aitorgf.threekt.objects.LineSegments
import kotlin.js.JsExport

/** Red X, green Y, and blue Z axis lines from the origin. */
@JsExport
class AxesHelper(size: Double = 1.0) : LineSegments(
    axesGeometry(size.toFloat()),
    LineBasicMaterial().apply {
        vertexColors = true
        fog = false
    },
)

private fun axesGeometry(size: Float): BufferGeometry = BufferGeometry()
    .setAttribute("position", Float32BufferAttribute(floatArrayOf(
        0f, 0f, 0f, size, 0f, 0f, 0f, 0f, 0f, 0f, size, 0f, 0f, 0f, 0f, 0f, 0f, size,
    ), 3))
    .setAttribute("color", Float32BufferAttribute(floatArrayOf(
        1f, 0f, 0f, 1f, 0.6f, 0f, 0f, 1f, 0f, 0.6f, 1f, 0f, 0f, 0f, 1f, 0f, 0.6f, 1f,
    ), 3))
