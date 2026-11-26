package com.aitorgf.threekt.core

import com.aitorgf.threekt.math.Vector2
import com.aitorgf.threekt.math.Vector3
import kotlin.js.JsExport
import kotlin.js.JsName

/**
 * A hit reported by [Raycaster], with the same fields as the plain objects that Three.js returns. Which
 * optional fields are set depends on the object that was hit: meshes report a [face], [faceIndex] and
 * [barycoord], and [uv], [uv1] and [normal] when the geometry has those attributes; points and lines
 * report the vertex or segment [index]; points also report [distanceToRay]; sprites report [uv];
 * instanced meshes add [instanceId].
 */
@JsExport
class Intersection(
    /** Distance from the ray origin to [point]. */
    val distance: Double,
    /** Hit point in world space. */
    val point: Vector3,
    obj: Object3D,
) {
    /** The object that was hit. Named `object` in JavaScript, as in Three.js. */
    @JsName("object")
    var obj: Object3D = obj
        internal set

    /** Distance between the ray and the hit point of a point cloud. */
    var distanceToRay: Double? = null
        internal set

    /** Index of the hit vertex of a point cloud, or of the first vertex of the hit line segment. */
    var index: Int? = null
        internal set

    /** The hit triangle of a mesh. */
    var face: Face? = null
        internal set

    /** Index of the hit triangle of a mesh, counted in the index buffer, or in vertices without one. */
    var faceIndex: Int? = null
        internal set

    /** Texture coordinates at the hit point, from the `uv` attribute of a mesh or the quad of a sprite. */
    var uv: Vector2? = null
        internal set

    /** Second set of texture coordinates at the hit point, from the `uv1` attribute. */
    var uv1: Vector2? = null
        internal set

    /** Vertex normal interpolated at the hit point, in local space, facing the ray. */
    var normal: Vector3? = null
        internal set

    /** Barycentric coordinates of the hit point within [face]. */
    var barycoord: Vector3? = null
        internal set

    /** Index of the hit instance of an `InstancedMesh`. */
    var instanceId: Int? = null
        internal set

    /**
     * A triangle of a mesh: the vertex indices [a], [b] and [c], its geometric [normal] in local space,
     * and the [materialIndex] of its draw group when the mesh has several materials.
     */
    class Face(val a: Int, val b: Int, val c: Int, val normal: Vector3, var materialIndex: Int = 0)
}
