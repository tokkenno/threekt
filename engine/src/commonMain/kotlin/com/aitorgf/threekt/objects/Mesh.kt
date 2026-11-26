package com.aitorgf.threekt.objects

import com.aitorgf.threekt.core.BufferGeometry
import com.aitorgf.threekt.core.Object3D
import com.aitorgf.threekt.materials.Material
import com.aitorgf.threekt.materials.MeshBasicMaterial
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
}
