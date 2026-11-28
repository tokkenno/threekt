package com.aitorgf.threekt.render.gl.webgl

import com.aitorgf.threekt.cameras.Camera
import com.aitorgf.threekt.core.BufferGeometry
import com.aitorgf.threekt.core.Object3D
import com.aitorgf.threekt.lights.Light
import com.aitorgf.threekt.materials.Material
import com.aitorgf.threekt.math.Frustum
import com.aitorgf.threekt.math.Matrix4
import com.aitorgf.threekt.math.Vector3
import com.aitorgf.threekt.objects.Line
import com.aitorgf.threekt.objects.Mesh
import com.aitorgf.threekt.objects.Points
import com.aitorgf.threekt.objects.Sprite

/**
 * Meshes, lines, points and sprites of a scene in drawing order for one view, as in Three.js `WebGLRenderList`. Opaque objects are
 * sorted by [Object3D.renderOrder], then by material to reduce state changes, then nearest first.
 * Transparent objects are sorted by [Object3D.renderOrder], then farthest first so that they blend over
 * what is behind them. Remaining ties keep creation order through [Object3D.id].
 *
 * It holds no platform state, so it lives in common code even though Three.js keeps it in the WebGL
 * renderer. Transmissive objects, groups, and material variants are not supported yet.
 */
internal class WebGLRenderList {
    /** An object with its geometry, material, and the clip-space depth of its origin. */
    class RenderItem(
        val obj: Object3D,
        val geometry: BufferGeometry,
        val material: Material,
        val z: Double,
        val group: BufferGeometry.Group? = null,
    )

    /** Opaque objects in drawing order. */
    val opaque = mutableListOf<RenderItem>()
    /** Transparent objects in drawing order. */
    val transparent = mutableListOf<RenderItem>()

    /**
     * Visible lights of the scene. Three.js keeps them in a separate `WebGLRenderState`, which is filled
     * during the same traversal.
     */
    val lights = mutableListOf<Light>()

    private val vector = Vector3()

    /** Clears both lists. */
    fun init() {
        opaque.clear()
        transparent.clear()
        lights.clear()
    }

    /** Adds [object] with clip-space depth [z] to the list that matches [material]. */
    fun push(obj: Object3D, geometry: BufferGeometry, material: Material, z: Double, group: BufferGeometry.Group? = null) {
        val item = RenderItem(obj, geometry, material, z, group)
        if (material.transparent) transparent.add(item) else opaque.add(item)
    }

    /** Sorts both lists into drawing order. */
    fun sort() {
        opaque.sortWith(PAINTER_SORT_STABLE)
        transparent.sortWith(REVERSE_PAINTER_SORT_STABLE)
    }

    /**
     * Fills the lists with the meshes, lines, points and sprites under [root] that [camera] can see, using
     * their depth under [projScreenMatrix], and sorts them, as `projectObject` does in Three.js. Hidden
     * objects and their descendants are skipped. Objects that share no layer with [camera] are skipped
     * without their descendants, and so are objects with [Object3D.frustumCulled] set that
     * [Object3D.intersectsFrustum] reports outside [frustum]. World matrices must be current.
     */
    fun build(root: Object3D, camera: Camera, projScreenMatrix: Matrix4, frustum: Frustum) {
        init()
        root.traverseVisible { node ->
            if (!node.layers.test(camera.layers)) return@traverseVisible
            val z = { vector.setFromMatrixPosition(node.matrixWorld).applyMatrix4(projScreenMatrix).z }
            val culled = node.frustumCulled && !node.intersectsFrustum(frustum)
            when (node) {
                is Light -> lights.add(node)
                is Mesh -> if (!culled) {
                    val materials = node.materials
                    if (materials == null) {
                        push(node, node.geometry, node.material, z())
                    } else {
                        // One item per draw group with its own material, as Three.js does for material arrays.
                        val depth = z()
                        for (group in node.geometry.groups) {
                            val groupMaterial = materials.getOrNull(group.materialIndex) ?: continue
                            push(node, node.geometry, groupMaterial, depth, group)
                        }
                    }
                }
                is Line -> if (!culled) push(node, node.geometry, node.material, z())
                is Points -> if (!culled) push(node, node.geometry, node.material, z())
                is Sprite -> if (!culled) push(node, node.geometry, node.material, z())
            }
        }
        sort()
    }

    private companion object {
        val PAINTER_SORT_STABLE = compareBy<RenderItem>({ it.obj.renderOrder }, { it.material.id }, { it.z }, { it.obj.id })
        val REVERSE_PAINTER_SORT_STABLE = compareBy<RenderItem>({ it.obj.renderOrder }, { -it.z }, { it.obj.id })
    }
}
