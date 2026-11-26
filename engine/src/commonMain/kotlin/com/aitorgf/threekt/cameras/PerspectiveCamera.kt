package com.aitorgf.threekt.cameras

import com.aitorgf.threekt.math.MathUtils
import com.aitorgf.threekt.math.Vector2
import com.aitorgf.threekt.math.Vector3
import kotlin.math.atan
import kotlin.math.tan
import com.aitorgf.threekt.core.Object3D
import kotlin.js.JsExport

/** Perspective camera with a vertical field of view in degrees. */
@JsExport
open class PerspectiveCamera(
    var fov: Double = 50.0,
    var aspect: Double = 1.0,
    var near: Double = 0.1,
    var far: Double = 2000.0,
) : Camera() {
    /** Distance at which the two images converge in stereo rendering. */
    var focus = 10.0

    /** Film size along its larger axis, in the same units as focal length. */
    var filmGauge = 35.0

    /** Horizontal film offset, in the same units as [filmGauge]. */
    var filmOffset = 0.0

    init {
        updateProjectionMatrix()
    }

    /** Sets the field of view from [focalLength] in film units and rebuilds the projection. */
    fun setFocalLength(focalLength: Double) {
        fov = MathUtils.radToDeg(2.0 * atan(getFilmHeight() * 0.5 / focalLength))
        updateProjectionMatrix()
    }

    /** Returns the focal length in film units. */
    fun getFocalLength(): Double = getFilmHeight() * 0.5 / tanVerticalHalfFov()

    /** Returns the vertical field of view in degrees after applying [zoom]. */
    fun getEffectiveFOV(): Double = MathUtils.radToDeg(2.0 * atan(tanVerticalHalfFov() / zoom))

    /** Returns the width of the film covered by the view. */
    fun getFilmWidth(): Double = filmGauge * minOf(aspect, 1.0)

    /** Returns the height of the film covered by the view. */
    fun getFilmHeight(): Double = filmGauge / maxOf(aspect, 1.0)

    /** Writes the lower-left and upper-right view bounds at [distance] into the supplied vectors. */
    fun getViewBounds(distance: Double, minTarget: Vector2, maxTarget: Vector2) {
        val corner = Vector3(-1.0, -1.0, 0.5).applyMatrix4(projectionMatrixInverse)
        minTarget.set(corner.x, corner.y).multiplyScalar(-distance / corner.z)
        corner.set(1.0, 1.0, 0.5).applyMatrix4(projectionMatrixInverse)
        maxTarget.set(corner.x, corner.y).multiplyScalar(-distance / corner.z)
    }

    /** Writes the width and height of the view at [distance] into [target] and returns it. */
    fun getViewSize(distance: Double, target: Vector2): Vector2 {
        val min = Vector2()
        getViewBounds(distance, min, target)
        return target.sub(min)
    }

    override fun onViewOffsetSet(view: ViewOffset) {
        aspect = view.fullWidth.toDouble() / view.fullHeight
    }

    internal fun tanVerticalHalfFov(): Double = tan(MathUtils.degToRad(fov) * 0.5)

    /** Rebuilds the perspective projection and its inverse after camera parameters change. */
    final override fun updateProjectionMatrix() {
        var top = near * tanVerticalHalfFov() / zoom
        var height = 2.0 * top
        var width = aspect * height
        var left = -width * 0.5
        viewOffset?.let { view ->
            val scaleX = width / view.fullWidth
            val scaleY = height / view.fullHeight
            left += view.x * scaleX
            top -= view.y * scaleY
            width = view.width * scaleX
            height = view.height * scaleY
        }
        if (filmOffset != 0.0) left += near * filmOffset / getFilmWidth()
        projectionMatrix.makePerspective(left, left + width, top, top - height, near, far, coordinateSystem, reversedDepth)
        projectionMatrixInverse.copy(projectionMatrix).invert()
    }

    /** Copies the lens settings of [source] when it is a perspective camera. Returns this camera. */
    override fun copy(source: Object3D, recursive: Boolean): PerspectiveCamera {
        super.copy(source, recursive)
        if (source is PerspectiveCamera) {
            fov = source.fov
            near = source.near
            far = source.far
            focus = source.focus
            aspect = source.aspect
            filmGauge = source.filmGauge
            filmOffset = source.filmOffset
        }
        return this
    }

    override fun clone(recursive: Boolean): PerspectiveCamera = PerspectiveCamera().copy(this, recursive)
}
