package com.aitorgf.threekt.core

import com.aitorgf.threekt.cameras.Camera
import com.aitorgf.threekt.cameras.OrthographicCamera
import com.aitorgf.threekt.math.Matrix4
import com.aitorgf.threekt.math.Ray
import com.aitorgf.threekt.math.Vector2
import com.aitorgf.threekt.math.Vector3
import com.aitorgf.threekt.renderers.CoordinateSystem
import kotlin.js.JsExport
import kotlin.js.JsName

/**
 * Casts a [ray] into the scene to find the objects it hits. Each object tests itself in
 * [Object3D.raycast]; only objects that share a layer with [layers] are tested. World matrices must be
 * current, which they are after a render.
 *
 * Deviations from Three.js:
 * - [intersectObject] and [intersectObjects] return a new array instead of filling an optional target
 *   array.
 * - [setFromCamera] treats every camera that is not orthographic as a perspective camera, and places the
 *   origin of orthographic rays on the near plane for every clip-space convention.
 */
@JsExport
class Raycaster(
    origin: Vector3 = Vector3(),
    direction: Vector3 = Vector3(0.0, 0.0, -1.0),
    /** Hits closer than this distance are ignored. */
    var near: Double = 0.0,
    /** Hits farther than this distance are ignored. */
    var far: Double = Double.POSITIVE_INFINITY,
) {
    /** World-space ray used for the tests. [direction][Ray.direction] must be normalized. */
    val ray = Ray(origin.copy(), direction.copy())

    /**
     * Camera of the last [setFromCamera] call. Sprites need it to know how they face the view; set it
     * directly when building the ray by hand.
     */
    var camera: Camera? = null

    /** Layers to test. Objects that share no layer with them are skipped, but their children are not. */
    val layers = Layers()

    /** Settings of the tests of each object type. */
    val params = Params()

    /** Settings of the tests of each object type, as the Three.js `params` object. */
    class Params {
        /** Settings of mesh tests. Meshes have none yet. */
        @JsName("Mesh")
        val mesh = MeshParams()

        /** Settings of line tests. */
        @JsName("Line")
        val line = ThresholdParams()

        /** Settings of level-of-detail tests. They have none yet. */
        @JsName("LOD")
        val lod = MeshParams()

        /** Settings of point tests. */
        @JsName("Points")
        val points = ThresholdParams()

        /** Settings of sprite tests. Sprites have none yet. */
        @JsName("Sprite")
        val sprite = MeshParams()
    }

    /** Settings of an object type without parameters. */
    class MeshParams

    /** Settings of lines and points: the world-space distance from the ray within which they are hit. */
    class ThresholdParams(var threshold: Double = 1.0)

    /** Sets the ray from an [origin] and a normalized [direction], both in world space. */
    fun set(origin: Vector3, direction: Vector3) {
        ray.set(origin, direction)
    }

    /**
     * Sets the ray from [coords], normalized device coordinates in `[-1, 1]` such as a pointer position,
     * and [camera], and remembers the camera in [camera]. The camera's world matrix must be current.
     */
    fun setFromCamera(coords: Vector2, camera: Camera) {
        if (camera is OrthographicCamera) {
            val low = if (camera.coordinateSystem == CoordinateSystem.WebGPU || camera.reversedDepth) 0.0 else -1.0
            val high = 1.0
            val front = if (camera.reversedDepth) high else low
            val back = if (camera.reversedDepth) low else high
            val z = front + (back - front) * -camera.near / (camera.far - camera.near)
            ray.origin.set(coords.x, coords.y, z).unproject(camera)
            ray.direction.set(0.0, 0.0, -1.0).transformDirection(camera.matrixWorld)
        } else {
            ray.origin.setFromMatrixPosition(camera.matrixWorld)
            ray.direction.set(coords.x, coords.y, 0.5).unproject(camera).sub(ray.origin).normalize()
        }
        this.camera = camera
    }

    /**
     * Sets the ray from the position of [controller] along its local negative Z axis, as XR controllers
     * point. Returns this raycaster.
     */
    fun setFromXRController(controller: Object3D): Raycaster {
        matrix.identity().extractRotation(controller.matrixWorld)
        ray.origin.setFromMatrixPosition(controller.matrixWorld)
        ray.direction.set(0.0, 0.0, -1.0).applyMatrix4(matrix)
        return this
    }

    /**
     * Returns the hits of [obj], and of its descendants when [recursive] is set, sorted by distance.
     * Hidden objects are tested too.
     */
    fun intersectObject(obj: Object3D, recursive: Boolean = true): Array<Intersection> {
        val intersects = mutableListOf<Intersection>()
        intersect(obj, intersects, recursive)
        intersects.sortBy { it.distance }
        return intersects.toTypedArray()
    }

    /** Returns the hits of all [objects], and of their descendants when [recursive] is set, sorted by distance. */
    fun intersectObjects(objects: Array<Object3D>, recursive: Boolean = true): Array<Intersection> {
        val intersects = mutableListOf<Intersection>()
        for (obj in objects) intersect(obj, intersects, recursive)
        intersects.sortBy { it.distance }
        return intersects.toTypedArray()
    }

    private fun intersect(obj: Object3D, intersects: MutableList<Intersection>, recursive: Boolean) {
        var propagate = true
        if (obj.layers.test(layers)) propagate = obj.raycast(this, intersects)
        if (propagate && recursive) {
            for (child in obj.children) intersect(child, intersects, true)
        }
    }

    private val matrix = Matrix4()
}
