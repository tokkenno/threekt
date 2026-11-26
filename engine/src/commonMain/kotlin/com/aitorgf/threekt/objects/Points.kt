package com.aitorgf.threekt.objects

import com.aitorgf.threekt.core.BufferGeometry
import com.aitorgf.threekt.core.Object3D
import com.aitorgf.threekt.materials.Material
import com.aitorgf.threekt.materials.PointsMaterial
import com.aitorgf.threekt.math.Frustum
import kotlin.js.JsExport

/** Draws each vertex of a [BufferGeometry] as a point, typically with a [PointsMaterial]. */
@JsExport
class Points(
    var geometry: BufferGeometry = BufferGeometry(),
    var material: Material = PointsMaterial(),
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
     * Shares the geometry and material of [source] and copies its morph target state when it is a
     * points. Returns this object.
     */
    override fun copy(source: Object3D, recursive: Boolean): Points {
        super.copy(source, recursive)
        if (source is Points) {
            morphTargets.copy(source.morphTargets)
            material = source.material
            geometry = source.geometry
        }
        return this
    }

    override fun clone(recursive: Boolean): Points = Points().copy(this, recursive)

    /** Tests the bounding sphere of this point cloud against [frustum], as the renderer does for culling. */
    @JsExport.Ignore
    override fun intersectsFrustum(frustum: Frustum): Boolean = frustum.intersectsObject(this)
}
