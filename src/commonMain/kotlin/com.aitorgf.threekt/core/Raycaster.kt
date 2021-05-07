package com.aitorgf.threekt.core

import com.aitorgf.threekt.cameras.OrthographicCamera
import com.aitorgf.threekt.math.Ray
import com.aitorgf.threekt.math.Vector2
import com.aitorgf.threekt.math.Vector3

@ExperimentalJsExport
@JsExport
class Raycaster(
    origin: Vector3,
    direction: Vector3,
    var near: Double = 0.0,
    var far: Double = Double.POSITIVE_INFINITY
) {
    val ray: Ray = Ray(origin, direction)
    var camera: Camera? = null
    val layers: Layers = Layers()

    fun set(origin: Vector3, direction: Vector3) {
        // direction is assumed to be normalized (for accurate distance calculations)
        this.ray.set(origin, direction);
    }

    fun setFromCamera(coords: Vector2, camera: Camera) {
        /*if ( camera is PerspectiveCamera ) {
            this.ray.origin.setFromMatrixPosition( camera.matrixWorld );
            this.ray.direction.set( coords.x, coords.y, 0.5 ).unproject( camera ).sub( this.ray.origin ).normalize();
            this.camera = camera;
        } else */
        if (camera is OrthographicCamera) {
            this.ray.origin.set(coords.x, coords.y, (camera.near + camera.far) / (camera.near - camera.far))
            camera.unproject(this.ray.origin) // set origin in plane of camera
            this.ray.direction.set(0.0, 0.0, -1.0).transformDirection(camera.matrixWorld);
            this.camera = camera;
        } else {
            throw EngineException("Unsupported camera type: ${camera::class.simpleName}")
        }
    }

    @JsName("intersectObject3")
    fun intersect(obj: Object3, recursive: Boolean = false ): Array<Intersection3> {
        var intersections = Raycaster.intersect(obj, this, recursive);
        intersections.sortBy { it.distance }
        return intersections
    }

    @JsName("intersectMultipleObject3")
    fun intersect(objs: Array<Object3>, recursive: Boolean = false): Array<Intersection3> {
        var intersections: Array<Intersection3> = emptyArray()

        objs.forEach {
            intersections += this.intersect(it, recursive)
        }

        intersections.sortBy { it.distance }
        return intersections
    }

    companion object {
        @JsName("intersectObject3")
        fun intersect(obj: Object3, raycaster: Raycaster, recursive: Boolean = false ): Array<Intersection3> {
            var intersections: Array<Intersection3> = emptyArray()

            if (obj.layers.test(raycaster.layers)) {
                intersections += obj.raycast(raycaster);
            }

            if (recursive) {
                obj.children.forEach {
                    intersections += intersect(it, raycaster, true)
                }
            }

            intersections.sortBy { it.distance }
            return intersections
        }
    }
}
