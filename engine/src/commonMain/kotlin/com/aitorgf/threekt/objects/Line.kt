package com.aitorgf.threekt.objects

import com.aitorgf.threekt.core.BufferGeometry
import com.aitorgf.threekt.core.Float32BufferAttribute
import com.aitorgf.threekt.core.Object3D
import com.aitorgf.threekt.materials.LineBasicMaterial
import com.aitorgf.threekt.materials.Material
import com.aitorgf.threekt.math.Vector3
import com.aitorgf.threekt.math.Frustum
import kotlin.js.JsExport

/** A continuous line through the vertices of a [BufferGeometry], drawn with a [Material]. */
@JsExport
open class Line(
    var geometry: BufferGeometry = BufferGeometry(),
    var material: Material = LineBasicMaterial(),
) : Object3D() {
    private val morphTargets = MorphTargets()

    init {
        updateMorphTargets()
    }

    /**
     * Weight of each morph target of the geometry, or null when the geometry had no morph targets at the
     * last [updateMorphTargets]. Influences are applied by the renderer on every draw.
     */
    val morphTargetInfluences: DoubleArray? get() = morphTargets.influences

    /** Index of each morph target by name, or by position when the target has no name. */
    @JsExport.Ignore
    val morphTargetDictionary: Map<String, Int>? get() = morphTargets.dictionary

    /** Resets [morphTargetInfluences] to zero for the geometry's current morph targets. */
    fun updateMorphTargets() = morphTargets.update(geometry)

    /**
     * Stores the distance along the line at each vertex in a `lineDistance` attribute, for dashed
     * materials. Only non-indexed geometry is supported; indexed geometry is left unchanged. Returns this
     * line.
     */
    open fun computeLineDistances(): Line {
        val position = geometry.getAttribute("position")
        if (geometry.index != null || position == null) return this
        val start = Vector3()
        val end = Vector3()
        val distances = FloatArray(position.count)
        var distance = 0.0
        for (index in 1 until position.count) {
            distance += start.fromBufferAttribute(position, index - 1).distanceTo(end.fromBufferAttribute(position, index))
            distances[index] = distance.toFloat()
        }
        geometry.setAttribute("lineDistance", Float32BufferAttribute(distances, 1))
        return this
    }

    /**
     * Shares the geometry and material of [source] and copies its morph target state when it is a
     * line. Returns this object.
     */
    override fun copy(source: Object3D, recursive: Boolean): Line {
        super.copy(source, recursive)
        if (source is Line) {
            morphTargets.copy(source.morphTargets)
            material = source.material
            geometry = source.geometry
        }
        return this
    }

    override fun clone(recursive: Boolean): Line = Line().copy(this, recursive)

    /** Tests the bounding sphere of this line against [frustum], as the renderer does for culling. */
    @JsExport.Ignore
    override fun intersectsFrustum(frustum: Frustum): Boolean = frustum.intersectsObject(this)
}
