package com.aitorgf.threekt.objects

import com.aitorgf.threekt.core.BufferGeometry
import com.aitorgf.threekt.core.Float32BufferAttribute
import com.aitorgf.threekt.materials.LineBasicMaterial
import com.aitorgf.threekt.materials.Material
import com.aitorgf.threekt.math.Vector3
import kotlin.js.JsExport

/** Independent line segments between consecutive pairs of vertices. */
@JsExport
open class LineSegments(
    geometry: BufferGeometry = BufferGeometry(),
    material: Material = LineBasicMaterial(),
) : Line(geometry, material) {
    /**
     * Stores the accumulated length of the segments at each vertex in a `lineDistance` attribute, for dashed
     * materials. Gaps between segments do not count. Only non-indexed geometry is supported. Returns this
     * object.
     */
    override fun computeLineDistances(): Line {
        val position = geometry.getAttribute("position")
        if (geometry.index != null || position == null) return this
        val start = Vector3()
        val end = Vector3()
        val distances = FloatArray(position.count)
        var distance = 0.0
        for (index in 0 until position.count - 1 step 2) {
            distances[index] = distance.toFloat()
            distance += start.fromBufferAttribute(position, index).distanceTo(end.fromBufferAttribute(position, index + 1))
            distances[index + 1] = distance.toFloat()
        }
        geometry.setAttribute("lineDistance", Float32BufferAttribute(distances, 1))
        return this
    }

    override fun clone(recursive: Boolean): LineSegments = LineSegments().copy(this, recursive) as LineSegments
}
