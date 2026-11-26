package com.aitorgf.threekt.helpers

import com.aitorgf.threekt.core.BufferGeometry
import com.aitorgf.threekt.core.Float32BufferAttribute
import com.aitorgf.threekt.materials.LineBasicMaterial
import com.aitorgf.threekt.math.Color
import com.aitorgf.threekt.objects.LineSegments
import kotlin.js.JsExport

/** An XZ grid with a different color on the center lines. */
@JsExport
class GridHelper(
    size: Double = 10.0,
    divisions: Int = 10,
    color1: Color = Color(0x444444),
    color2: Color = Color(0x888888),
) : LineSegments(gridGeometry(size, divisions, color1, color2), LineBasicMaterial().apply {
    vertexColors = true
    fog = false
}) {
    /** Returns a grid that shares the geometry and material of this one. */
    override fun clone(recursive: Boolean): GridHelper = GridHelper().copy(this, recursive) as GridHelper
}

private fun gridGeometry(size: Double, divisions: Int, color1: Color, color2: Color): BufferGeometry {
    require(divisions > 0)
    val positions = FloatArray((divisions + 1) * 12)
    val colors = FloatArray(positions.size)
    val halfSize = (size / 2).toFloat()
    for (i in 0..divisions) {
        val k = (i * size / divisions - size / 2).toFloat()
        val offset = i * 12
        positions[offset] = -halfSize
        positions[offset + 2] = k
        positions[offset + 3] = halfSize
        positions[offset + 5] = k
        positions[offset + 6] = k
        positions[offset + 8] = -halfSize
        positions[offset + 9] = k
        positions[offset + 11] = halfSize
        val color = if (i.toDouble() == divisions / 2.0) color1 else color2
        for (vertex in offset until offset + 12 step 3) {
            colors[vertex] = color.r.toFloat()
            colors[vertex + 1] = color.g.toFloat()
            colors[vertex + 2] = color.b.toFloat()
        }
    }
    return BufferGeometry()
        .setAttribute("position", Float32BufferAttribute(positions, 3))
        .setAttribute("color", Float32BufferAttribute(colors, 3))
}
