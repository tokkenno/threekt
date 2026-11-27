package com.aitorgf.threekt.cameras

import com.aitorgf.threekt.core.Object3D
import com.aitorgf.threekt.math.Matrix4
import com.aitorgf.threekt.math.Quaternion
import com.aitorgf.threekt.math.Vector3
import com.aitorgf.threekt.renderers.CoordinateSystem
import kotlin.js.JsExport

/**
 * Base class for cameras with world, view, and projection transforms. The view matrix maps world
 * coordinates into camera space.
 */
@JsExport
abstract class Camera : Object3D() {
    /** Clip-space convention used to create the projection. */
    var coordinateSystem = CoordinateSystem.WebGL
    /** Whether the projection maps the near plane to the greater depth value. */
    var reversedDepth = false
    /** View transform from world space to camera space. */
    val matrixWorldInverse = Matrix4()
    /** Transform from camera space to clip space. */
    val projectionMatrix = Matrix4()
    /** Inverse of the current projection matrix. */
    val projectionMatrixInverse = Matrix4()

    /** Magnification factor applied to the projection. */
    open var zoom = 1.0

    /** Active tile of a larger view, or null when the full view is used. */
    var viewOffset: ViewOffset? = null
        private set

    private val viewPosition = Vector3()
    private val viewRotation = Quaternion()
    private val viewScale = Vector3()
    private val unitScale = Vector3(1.0, 1.0, 1.0)

    /** A view tile in pixels, measured from the top left of the full view. */
    data class ViewOffset(
        val fullWidth: Int, val fullHeight: Int, val x: Int, val y: Int, val width: Int, val height: Int,
    ) {
        init {
            require(fullWidth > 0 && fullHeight > 0 && width > 0 && height > 0)
        }
    }

    /** Sets a tile of a larger view and rebuilds the projection. */
    fun setViewOffset(fullWidth: Int, fullHeight: Int, x: Int, y: Int, width: Int, height: Int) {
        val view = ViewOffset(fullWidth, fullHeight, x, y, width, height)
        viewOffset = view
        onViewOffsetSet(view)
        updateProjectionMatrix()
    }

    /** Restores the full view and rebuilds the projection. */
    fun clearViewOffset() {
        viewOffset = null
        updateProjectionMatrix()
    }

    /** Lets a projection update its full-view parameters before applying a tile. */
    protected open fun onViewOffsetSet(view: ViewOffset) = Unit

    /** Rebuilds [projectionMatrix] and [projectionMatrixInverse] after projection parameters change. */
    abstract fun updateProjectionMatrix()

    override val looksAlongNegativeZ: Boolean get() = true

    /**
     * Writes the viewing direction of the camera, its local negative Z axis in world space, to [target] and
     * returns it.
     */
    override fun getWorldDirection(target: Vector3): Vector3 = super.getWorldDirection(target).negate()

    /** Updates the world matrices, then rebuilds [matrixWorldInverse] from the camera's world pose. */
    override fun updateMatrixWorld(force: Boolean) {
        super.updateMatrixWorld(force)
        updateMatrixWorldInverse()
    }

    /** Updates the world matrices, then rebuilds [matrixWorldInverse] from the camera's world pose. */
    override fun updateWorldMatrix(updateParents: Boolean, updateChildren: Boolean, force: Boolean) {
        super.updateWorldMatrix(updateParents, updateChildren, force)
        updateMatrixWorldInverse()
    }

    /**
     * Copies the matrices, [coordinateSystem], [reversedDepth], [zoom] and [viewOffset] of [source] when it
     * is a camera, in addition to what [Object3D.copy] copies. Returns this camera. Three.js copies the zoom
     * and the view offset in each camera subclass.
     */
    override fun copy(source: Object3D, recursive: Boolean): Camera {
        super.copy(source, recursive)
        if (source is Camera) {
            matrixWorldInverse.copy(source.matrixWorldInverse)
            projectionMatrix.copy(source.projectionMatrix)
            projectionMatrixInverse.copy(source.projectionMatrixInverse)
            coordinateSystem = source.coordinateSystem
            reversedDepth = source.reversedDepth
            zoom = source.zoom
            viewOffset = source.viewOffset
        }
        return this
    }

    /**
     * Rebuilds the view matrix from the camera's world pose, excluding scale so that scaled cameras behave
     * as in glTF.
     */
    private fun updateMatrixWorldInverse() {
        viewScale.setFromMatrixScale(matrixWorld)
        val determinant = matrixWorld.determinantAffine()
        val unitWorldScale = viewScale.x == 1.0 && viewScale.y == 1.0 && viewScale.z == 1.0
        if (determinant == 0.0 || (determinant > 0.0 && unitWorldScale)) {
            matrixWorldInverse.copy(matrixWorld)
        } else {
            matrixWorld.decompose(viewPosition, viewRotation, viewScale)
            matrixWorldInverse.compose(viewPosition, viewRotation, unitScale)
        }
        matrixWorldInverse.invertAffine()
    }
}
