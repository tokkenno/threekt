package com.aitorgf.threekt.cameras

import com.aitorgf.threekt.math.Box2
import com.aitorgf.threekt.math.Vector2
import com.aitorgf.threekt.core.Object3D
import kotlin.js.JsExport

/**
 * Perspective camera calibrated by focal length, film dimensions and a principal point. Principal
 * point and crop coordinates are fractions of the unscaled image, with Y increasing upwards.
 *
 * Based on the sibling engine's camera. Threekt rebuilds the projection explicitly via
 * [updateProjectionMatrix], as with its other cameras. The near plane defaults to 0.1 instead of 0
 * so the projection has an inverse. Film aspect and film height describe the same film.
 */
@JsExport
class CalibratedPerspectiveCamera(
    var focal: Double = 50.0,
    aspect: Double = 1.0,
    var near: Double = 0.1,
    var far: Double = 10000.0,
) : Camera() {
    private val film = Vector2(35.0, 35.0 / aspect)
    private val crop = Box2(Vector2(), Vector2(1.0, 1.0))
    private val shift = Vector2()
    private val panOffset = Vector2()

    /** Width of the film, in the same units as [focal]. Setting it preserves [filmAR]. */
    var filmGauge: Double
        get() = film.x
        set(value) { film.set(value, value / filmAR) }

    /** Aspect ratio of the uncropped film. */
    var filmAR: Double
        get() = film.x / film.y
        set(value) { film.y = film.x / value }

    /** Film dimensions. The getter returns a copy and the setter copies the supplied values. */
    var filmSize: Vector2
        get() = film.copy()
        set(value) { film.copy(value) }

    /** Normalized displacement of the principal point from center. Returns a copy. */
    var principalPoint: Vector2
        get() = shift.copy()
        set(value) { shift.copy(value) }

    /** Pan of the zoomed window. Each component is clamped to [-1, 1]. Returns a copy. */
    var pan: Vector2
        get() = panOffset.copy()
        set(value) { panOffset.set(value.x.coerceIn(-1.0, 1.0), value.y.coerceIn(-1.0, 1.0)) }

    /** Image magnification, clamped to a minimum of 1. */
    override var zoom: Double = 1.0
        set(value) { field = value.coerceAtLeast(1.0) }

    /** Bounds of the selected film rectangle. The getter and setter copy the bounds. */
    @JsExport.Ignore
    var cropArea: Box2
        get() = crop.copy()
        set(value) { setCropArea(value.min.x, value.min.y, value.max.x, value.max.y) }

    /** Aspect ratio of the cropped film, before a viewport offset. */
    val aspectRatio: Double
        get() = (crop.max.x - crop.min.x) * film.x / ((crop.max.y - crop.min.y) * film.y)

    init {
        name = "CalibratedPerspectiveCamera"
        updateProjectionMatrix()
    }

    /** Sets the normalized crop rectangle. The default is (0, 0)–(1, 1). */
    fun setCropArea(minX: Double, minY: Double, maxX: Double, maxY: Double) {
        require(minX in 0.0..1.0 && minY in 0.0..1.0 && maxX in 0.0..1.0 && maxY in 0.0..1.0 &&
            minX < maxX && minY < maxY) { "The crop must have positive area within the film." }
        crop.min.set(minX, minY)
        crop.max.set(maxX, maxY)
    }

    /** Adds [displacement] to the principal point and returns this camera. */
    fun displacePrincipalPoint(displacement: Vector2): CalibratedPerspectiveCamera {
        shift.add(displacement)
        return this
    }

    /** Rebuilds the calibrated perspective projection and its inverse. */
    override fun updateProjectionMatrix() {
        require(focal.isFinite() && focal > 0 && film.x.isFinite() && film.x > 0 && film.y.isFinite() && film.y > 0)
        require(near.isFinite() && far.isFinite() && near > 0 && far > near)
        require(zoom.isFinite() && shift.x.isFinite() && shift.y.isFinite() && panOffset.x.isFinite() && panOffset.y.isFinite())

        val cropWidth = crop.max.x - crop.min.x
        val cropHeight = crop.max.y - crop.min.y
        val centerX = (crop.min.x + crop.max.x) * 0.5 - 0.5
        val centerY = (crop.min.y + crop.max.y) * 0.5 - 0.5
        val filmScaleX = near * film.x / focal
        val filmScaleY = near * film.y / focal
        val halfWindow = 0.5 / zoom
        val travel = 0.5 - halfWindow

        // The upstream NDC × intrinsic product is equivalent to these asymmetric near-plane bounds.
        var left = (centerX + (panOffset.x * travel - halfWindow) * cropWidth - shift.x) * filmScaleX
        var top = (centerY + (panOffset.y * travel + halfWindow) * cropHeight - shift.y) * filmScaleY
        var width = 2 * halfWindow * cropWidth * filmScaleX
        var height = 2 * halfWindow * cropHeight * filmScaleY
        viewOffset?.let { view ->
            val scaleX = width / view.fullWidth
            val scaleY = height / view.fullHeight
            left += view.x * scaleX
            top -= view.y * scaleY
            width = view.width * scaleX
            height = view.height * scaleY
        }
        projectionMatrix.makePerspective(left, left + width, top, top - height, near, far, coordinateSystem, reversedDepth)
        projectionMatrixInverse.copy(projectionMatrix).invert()
    }

    /** Copies the calibration of [source] when it is a calibrated camera. Returns this camera. */
    override fun copy(source: Object3D, recursive: Boolean): CalibratedPerspectiveCamera {
        super.copy(source, recursive)
        if (source is CalibratedPerspectiveCamera) {
            focal = source.focal
            near = source.near
            far = source.far
            film.copy(source.film)
            crop.copy(source.crop)
            shift.copy(source.shift)
            panOffset.copy(source.panOffset)
            updateProjectionMatrix()
        }
        return this
    }

    override fun clone(recursive: Boolean): CalibratedPerspectiveCamera = CalibratedPerspectiveCamera().copy(this, recursive)
}
