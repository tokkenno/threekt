package com.aitorgf.threekt.math

import com.aitorgf.threekt.cameras.Camera
import com.aitorgf.threekt.cameras.ArrayCamera
import com.aitorgf.threekt.core.Object3D

/**
 * FrustumArray is used to determine if an object is visible in at least one camera from an array of
 * cameras. This is particularly useful for multi-view renderers.
 */
class FrustumArray {
    private val frustums = mutableListOf<Frustum>()
    private var count = 0

    /** Refreshes the frustum for each subcamera in [camera]. */
    fun setFromArrayCamera(camera: ArrayCamera): FrustumArray = setFromArrayCamera(camera.views.map { it.camera })

    /**
     * Refreshes the cached view frustum for each camera in [cameras]. Uses the cameras' current world and
     * projection matrices.
     */
    fun setFromArrayCamera(cameras: Iterable<Camera>): FrustumArray {
        count = 0
        val matrix = Matrix4()
        for (camera in cameras) {
            matrix.multiplyMatrices(camera.projectionMatrix, camera.matrixWorldInverse)
            if (count == frustums.size) frustums.add(Frustum())
            frustums[count++].setFromProjectionMatrix(matrix, camera.coordinateSystem, camera.reversedDepth)
        }
        return this
    }

    /** Returns the cached frustum for the sub-camera at the given index. */
    fun getFrustum(index: Int): Frustum? = frustums.getOrNull(index)

    /**
     * Returns `true` if the 3D object's bounding sphere is intersecting any cached frustum.
     * [FrustumArray.setFromArrayCamera] must be called once per render before this method.
     */
    fun intersectsObject(object3D: Object3D): Boolean = anyFrustum { it.intersectsObject(object3D) }

    /**
     * Returns `true` if the given sprite is intersecting any cached frustum.
     * [FrustumArray.setFromArrayCamera] must be called once per render before this method.
     */
    fun intersectsSprite(sprite: Object3D): Boolean = anyFrustum { it.intersectsSprite(sprite) }

    /**
     * Returns `true` if the given bounding sphere is intersecting any cached frustum.
     * [FrustumArray.setFromArrayCamera] must be called once per render before this method.
     */
    fun intersectsSphere(sphere: Sphere): Boolean = anyFrustum { it.intersectsSphere(sphere) }

    /**
     * Returns `true` if the given bounding box is intersecting any cached frustum.
     * [FrustumArray.setFromArrayCamera] must be called once per render before this method.
     */
    fun intersectsBox(box: Box3): Boolean = anyFrustum { it.intersectsBox(box) }

    /**
     * Returns `true` if the given point lies within any cached frustum. [FrustumArray.setFromArrayCamera]
     * must be called once per render before this method.
     */
    fun containsPoint(point: Vector3): Boolean = anyFrustum { it.containsPoint(point) }

    private inline fun anyFrustum(test: (Frustum) -> Boolean): Boolean {
        for (index in 0 until count) if (test(frustums[index])) return true
        return false
    }

    /** Copies the supplied values into this instance and returns it. */
    fun copy(source: FrustumArray): FrustumArray {
        if (source === this) return this
        for (index in 0 until source.count) {
            if (index == frustums.size) frustums.add(Frustum())
            frustums[index].copy(source.frustums[index])
        }
        count = source.count
        return this
    }

    /** Returns a new instance with the same values. */
    fun copy(): FrustumArray = FrustumArray().copy(this)
}
