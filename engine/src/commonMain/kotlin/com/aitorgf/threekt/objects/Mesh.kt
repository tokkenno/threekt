package com.aitorgf.threekt.objects

import com.aitorgf.threekt.core.BufferGeometry
import com.aitorgf.threekt.core.Object3D
import com.aitorgf.threekt.materials.Material
import com.aitorgf.threekt.materials.MeshBasicMaterial
import com.aitorgf.threekt.math.Frustum
import kotlin.js.JsExport
import kotlin.js.JsName

/**
 * A scene node that draws the triangles of a [BufferGeometry] with a [Material]. With [materials], each
 * draw group of the geometry uses the material at its index instead, as when Three.js receives an array
 * of materials.
 */
@JsExport
open class Mesh(
    var geometry: BufferGeometry = BufferGeometry(),
    var material: Material = MeshBasicMaterial(),
) : Object3D() {
    /**
     * Creates a multi-material mesh. Three.js accepts the array in the `material` argument; from JavaScript
     * call `Mesh.withMaterials(geometry, materials)`.
     */
    @JsName("withMaterials")
    constructor(geometry: BufferGeometry, materials: Array<Material>) : this(geometry, materials[0]) {
        this.materials = materials
    }

    /**
     * Materials selected by the [BufferGeometry.groups] of the geometry, or null to draw the whole
     * geometry with [material]. Groups whose index has no material are skipped.
     */
    var materials: Array<Material>? = null

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
     * Shares the geometry and materials of [source] and copies its morph target state when it is a
     * mesh. Returns this object.
     */
    override fun copy(source: Object3D, recursive: Boolean): Mesh {
        super.copy(source, recursive)
        if (source is Mesh) {
            morphTargets.copy(source.morphTargets)
            material = source.material
            materials = source.materials?.copyOf()
            geometry = source.geometry
        }
        return this
    }

    override fun clone(recursive: Boolean): Mesh = Mesh().copy(this, recursive)

    /** Tests the bounding sphere of this mesh against [frustum], as the renderer does for culling. */
    @JsExport.Ignore
    override fun intersectsFrustum(frustum: Frustum): Boolean = frustum.intersectsObject(this)
}
