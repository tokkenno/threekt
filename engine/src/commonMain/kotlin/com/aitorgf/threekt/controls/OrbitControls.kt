package com.aitorgf.threekt.controls

import com.aitorgf.threekt.cameras.Camera
import com.aitorgf.threekt.cameras.OrthographicCamera
import com.aitorgf.threekt.cameras.PerspectiveCamera
import com.aitorgf.threekt.math.MathUtils
import com.aitorgf.threekt.math.Plane
import com.aitorgf.threekt.math.Quaternion
import com.aitorgf.threekt.math.Ray
import com.aitorgf.threekt.math.Spherical
import com.aitorgf.threekt.math.Vector2
import com.aitorgf.threekt.math.Vector3
import kotlin.js.JsExport
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Orbits a camera around [target] while keeping its [Camera.up] direction. Dragging with the primary
 * button or one finger rotates, the wheel, middle button, or pinch dollies, and the secondary button or
 * two fingers pan. A perspective camera dollies by changing its distance; an orthographic camera changes
 * its [Camera.zoom].
 *
 * Input comes from an optional [InputSurface], such as `DomInputSurface` in the browser, and keys from
 * the source passed to [listenToKeyEvents]. The camera's [Camera.up] is captured on construction. Call [update] after changing the camera
 * transform or [target] directly, and on every frame when [enableDamping] or [autoRotate] is enabled.
 */
@JsExport
class OrbitControls(camera: Camera, surface: InputSurface? = null) : Controls<Camera>(camera) {
    @JsExport.Ignore
    private enum class State { None, Rotate, Dolly, Pan, TouchRotate, TouchPan, TouchDollyPan, TouchDollyRotate }

    /** Point the camera orbits around. */
    val target = Vector3()
    /** Center of the sphere that limits [target], used with [minTargetRadius] and [maxTargetRadius]. */
    val cursor = Vector3()

    /** Smallest distance a perspective camera can dolly in to. */
    var minDistance = 0.0
    /** Largest distance a perspective camera can dolly out to. */
    var maxDistance = Double.POSITIVE_INFINITY
    /** Smallest zoom of an orthographic camera. */
    var minZoom = 0.0
    /** Largest zoom of an orthographic camera. */
    var maxZoom = Double.POSITIVE_INFINITY
    /** Smallest distance between [target] and [cursor]. */
    var minTargetRadius = 0.0
    /** Largest distance between [target] and [cursor]. */
    var maxTargetRadius = Double.POSITIVE_INFINITY
    /** Lowest polar angle in radians, measured from the up direction. */
    var minPolarAngle = 0.0
    /** Highest polar angle in radians. `PI / 2` keeps the camera above the horizon. */
    var maxPolarAngle = PI
    /** Lowest azimuth angle in radians. Both azimuth limits must be finite to take effect. */
    var minAzimuthAngle = Double.NEGATIVE_INFINITY
    /** Highest azimuth angle in radians. Both azimuth limits must be finite to take effect. */
    var maxAzimuthAngle = Double.POSITIVE_INFINITY

    /** Whether motion continues and slows down after input. Requires [update] on every frame. */
    var enableDamping = false
    /** Fraction of the remaining motion applied on each [update] when [enableDamping] is set. */
    var dampingFactor = 0.05
    /** Whether dollying or zooming is enabled. */
    var enableZoom = true
    /** Multiplier for dolly and zoom speed. */
    var zoomSpeed = 1.0
    /** Whether rotation is enabled. */
    var enableRotate = true
    /** Multiplier for rotation speed. */
    var rotateSpeed = 1.0
    /** Multiplier for rotation by keyboard. */
    var keyRotateSpeed = 1.0
    /** Whether panning is enabled. */
    var enablePan = true
    /** Multiplier for pan speed. */
    var panSpeed = 1.0
    /** Pans in the screen plane when true, or in the plane orthogonal to [Camera.up] when false. */
    var screenSpacePanning = true
    /** Pixels moved per arrow-key press. */
    var keyPanSpeed = 7.0
    /** Whether dollying moves toward the point under the pointer instead of the target. */
    var zoomToCursor = false
    /** Whether the camera rotates around [target] automatically. Requires [update] on every frame. */
    var autoRotate = false
    /** Automatic rotation speed. `2.0` completes one orbit every 30 seconds at 60 frames per second. */
    var autoRotateSpeed = 2.0

    /** Keys handled after [listenToKeyEvents]. */
    var keys = OrbitKeys()
    /** Actions for mouse buttons. */
    var mouseButtons = MouseButtons()
    /** Actions for touch gestures. */
    var touches = Touches()

    /** Target restored by [reset]. */
    val target0 = target.copy()
    /** Camera position restored by [reset]. */
    val position0 = camera.position.copy()
    /** Camera zoom restored by [reset]. */
    var zoom0 = camera.zoom

    /** Cursor mode: [Cursor.Grab] shows grab and grabbing cursors, any other value uses [Cursor.Auto]. */
    var cursorStyle = Cursor.Auto
        set(value) {
            field = value
            surface?.cursor = idleCursor
        }

    private val idleCursor: Cursor get() = if (cursorStyle == Cursor.Grab) Cursor.Grab else Cursor.Auto

    private var state = State.None
    private var keySource: InputSource? = null

    private val lastPosition = Vector3()
    private val lastQuaternion = Quaternion()
    private val lastTargetPosition = Vector3()
    private val quat = Quaternion().setFromUnitVectors(camera.up, Vector3(0.0, 1.0, 0.0))
    private val quatInverse = Quaternion().copy(quat).invert()
    private val spherical = Spherical()
    private val sphericalDelta = Spherical()
    private var scale = 1.0
    private val panOffset = Vector3()
    private val offset = Vector3()

    private val rotateStart = Vector2()
    private val rotateEnd = Vector2()
    private val rotateDelta = Vector2()
    private val panStart = Vector2()
    private val panEnd = Vector2()
    private val panDelta = Vector2()
    private val dollyStart = Vector2()
    private val dollyEnd = Vector2()
    private val dollyDirection = Vector3()
    private val mouse = Vector2()
    private var performCursorZoom = false

    private val pointers = mutableListOf<Int>()
    private val pointerPositions = mutableMapOf<Int, Vector2>()

    private val ray = Ray()
    private val plane = Plane()


    init {
        if (surface != null) connect(surface)
        update()
    }

    override fun onConnect(surface: InputSurface) {
        surface.cursor = idleCursor
    }

    override fun onDisconnect(surface: InputSurface) {
        state = State.None
        stopListenToKeyEvents()
        pointers.clear()
        pointerPositions.clear()
        surface.cursor = Cursor.Auto
    }

    /** Returns the current polar angle in radians, measured from the up direction. */
    fun getPolarAngle(): Double = spherical.phi

    /** Returns the current azimuth angle in radians. */
    fun getAzimuthalAngle(): Double = spherical.theta

    /** Returns the distance from the camera to [target]. */
    fun getDistance(): Double = `object`.position.distanceTo(target)

    /**
     * Handles arrow keys from [source], which in the browser is usually `new DomInputSource(window)`.
     * Replaces any previous key source.
     */
    fun listenToKeyEvents(source: InputSource) {
        stopListenToKeyEvents()
        source.addInputListener(keyListener)
        keySource = source
    }

    /** Stops handling the keys registered with [listenToKeyEvents]. */
    fun stopListenToKeyEvents() {
        keySource?.removeInputListener(keyListener)
        keySource = null
    }

    /** Forwards only key presses, so that a key source does not also deliver pointer input. */
    private val keyListener = object : InputListener {
        override fun onPointerDown(input: PointerInput) = Unit
        override fun onPointerMove(input: PointerInput) = Unit
        override fun onPointerUp(input: PointerInput) = Unit
        override fun onWheel(input: WheelInput): Boolean = false
        override fun onContextMenu(): Boolean = false
        override fun onKeyDown(input: KeyInput): Boolean = enabled && handleKeyDown(input)
    }

    /** Stores the current target, camera position, and zoom for [reset]. */
    fun saveState() {
        target0.copy(target)
        position0.copy(`object`.position)
        zoom0 = `object`.zoom
    }

    /** Restores the state stored by [saveState] or at construction and dispatches `change`. */
    fun reset() {
        target.copy(target0)
        `object`.position.copy(position0)
        `object`.zoom = zoom0
        `object`.updateProjectionMatrix()
        dispatchEvent(CHANGE)
        update()
        state = State.None
    }

    /** Pans by [deltaX] and [deltaY] pixels, where right and down are positive, then updates. */
    fun pan(deltaX: Double, deltaY: Double) {
        panPixels(deltaX, deltaY)
        update()
    }

    /** Dollies toward the target by [dollyScale], which is greater than `1`, then updates. */
    fun dollyIn(dollyScale: Double) {
        dollyInBy(dollyScale)
        update()
    }

    /** Dollies away from the target by [dollyScale], which is greater than `1`, then updates. */
    fun dollyOut(dollyScale: Double) {
        dollyOutBy(dollyScale)
        update()
    }

    /** Rotates around the up axis by [angle] radians, then updates. */
    fun rotateLeft(angle: Double) {
        sphericalDelta.theta -= angle
        update()
    }

    /** Rotates toward the up direction by [angle] radians, then updates. */
    fun rotateUp(angle: Double) {
        sphericalDelta.phi -= angle
        update()
    }

    /**
     * Applies pending rotation, dolly, and pan to the camera and points it at [target]. With [autoRotate],
     * [deltaTime] in seconds sets the rotation; without it, one 60 Hz frame is assumed. Dispatches
     * `change` and returns true when the camera or target moved.
     */
    override fun update(deltaTime: Double?): Boolean {
        val camera = `object`
        val position = camera.position
        offset.copy(position).sub(target).applyQuaternion(quat)
        spherical.setFromVector3(offset)

        if (autoRotate && state == State.None) sphericalDelta.theta -= autoRotationAngle(deltaTime)

        val damping = if (enableDamping) dampingFactor else 1.0
        spherical.theta += sphericalDelta.theta * damping
        spherical.phi += sphericalDelta.phi * damping

        var min = minAzimuthAngle
        var max = maxAzimuthAngle
        if (min.isFinite() && max.isFinite()) {
            if (min < -PI) min += TWO_PI else if (min > PI) min -= TWO_PI
            if (max < -PI) max += TWO_PI else if (max > PI) max -= TWO_PI
            spherical.theta = if (min <= max) {
                spherical.theta.coerceIn(min, max)
            } else if (spherical.theta > (min + max) / 2) {
                maxOf(min, spherical.theta)
            } else {
                minOf(max, spherical.theta)
            }
        }
        spherical.phi = maxOf(minPolarAngle, minOf(maxPolarAngle, spherical.phi))
        spherical.makeSafe()

        target.addScaledVector(panOffset, damping)
        target.sub(cursor).clampLength(minTargetRadius, maxTargetRadius).add(cursor)

        var zoomChanged = false
        if ((zoomToCursor && performCursorZoom) || camera is OrthographicCamera) {
            spherical.radius = clampDistance(spherical.radius)
        } else {
            val previousRadius = spherical.radius
            spherical.radius = clampDistance(spherical.radius * scale)
            zoomChanged = previousRadius != spherical.radius
        }

        offset.setFromSpherical(spherical).applyQuaternion(quatInverse)
        position.copy(target).add(offset)
        camera.lookAt(target)

        if (enableDamping) {
            sphericalDelta.theta *= 1.0 - dampingFactor
            sphericalDelta.phi *= 1.0 - dampingFactor
            panOffset.multiplyScalar(1.0 - dampingFactor)
        } else {
            sphericalDelta.set(0.0, 0.0, 0.0)
            panOffset.set(0.0, 0.0, 0.0)
        }

        if (zoomToCursor && performCursorZoom) {
            zoomChanged = zoomTowardCursor(camera) || zoomChanged
        } else if (camera is OrthographicCamera) {
            zoomChanged = applyOrthographicZoom(camera) || zoomChanged
        }

        scale = 1.0
        performCursorZoom = false

        // Small-angle approximation: cos(x / 2) = 1 - x² / 8.
        if (zoomChanged ||
            lastPosition.distanceToSquared(position) > EPS ||
            8.0 * (1.0 - lastQuaternion.dot(camera.quaternion)) > EPS ||
            lastTargetPosition.distanceToSquared(target) > EPS
        ) {
            dispatchEvent(CHANGE)
            lastPosition.copy(position)
            lastQuaternion.copy(camera.quaternion)
            lastTargetPosition.copy(target)
            return true
        }
        return false
    }

    private fun applyOrthographicZoom(camera: OrthographicCamera): Boolean {
        val previousZoom = camera.zoom
        camera.zoom = maxOf(minZoom, minOf(maxZoom, camera.zoom / scale))
        if (previousZoom == camera.zoom) return false
        camera.updateProjectionMatrix()
        return true
    }

    /** Moves the camera, or changes the orthographic zoom, toward the point under the pointer. */
    private fun zoomTowardCursor(camera: Camera): Boolean {
        val newRadius: Double
        var zoomChanged: Boolean
        when (camera) {
            is PerspectiveCamera -> {
                val previousRadius = offset.length()
                newRadius = clampDistance(previousRadius * scale)
                val radiusDelta = previousRadius - newRadius
                camera.position.addScaledVector(dollyDirection, radiusDelta)
                camera.updateMatrixWorld()
                zoomChanged = radiusDelta != 0.0
            }
            is OrthographicCamera -> {
                val mouseBefore = Vector3(mouse.x, mouse.y, 0.0).unproject(camera)
                zoomChanged = applyOrthographicZoom(camera)
                val mouseAfter = Vector3(mouse.x, mouse.y, 0.0).unproject(camera)
                camera.position.sub(mouseAfter).add(mouseBefore)
                camera.updateMatrixWorld()
                newRadius = offset.length()
            }
            else -> {
                zoomToCursor = false
                return false
            }
        }

        if (screenSpacePanning) {
            target.set(0.0, 0.0, -1.0).transformDirection(camera.matrix).multiplyScalar(newRadius).add(camera.position)
        } else {
            ray.origin.copy(camera.position)
            ray.direction.set(0.0, 0.0, -1.0).transformDirection(camera.matrix)
            // Keep the target when the camera looks almost horizontally, which would move it very far away.
            if (abs(camera.up.dot(ray.direction)) < TILT_LIMIT) {
                camera.lookAt(target)
            } else {
                plane.setFromNormalAndCoplanarPoint(camera.up, target)
                ray.intersectPlane(plane, target)
            }
        }
        return zoomChanged
    }

    private fun autoRotationAngle(deltaTime: Double?): Double =
        if (deltaTime != null) TWO_PI / 60.0 * autoRotateSpeed * deltaTime else TWO_PI / 60.0 / 60.0 * autoRotateSpeed

    private fun zoomScale(delta: Double): Double = 0.95.pow(zoomSpeed * abs(delta * 0.01))

    private fun clampDistance(distance: Double): Double = maxOf(minDistance, minOf(maxDistance, distance))

    private fun canDolly(): Boolean {
        val camera = `object`
        if (camera is PerspectiveCamera || camera is OrthographicCamera) return true
        enableZoom = false
        return false
    }

    private fun dollyInBy(dollyScale: Double) {
        if (canDolly()) scale *= dollyScale
    }

    private fun dollyOutBy(dollyScale: Double) {
        if (canDolly()) scale /= dollyScale
    }

    private fun panLeft(distance: Double) {
        offset.setFromMatrixColumn(`object`.matrix, 0).multiplyScalar(-distance)
        panOffset.add(offset)
    }

    private fun panUp(distance: Double) {
        if (screenSpacePanning) {
            offset.setFromMatrixColumn(`object`.matrix, 1)
        } else {
            offset.setFromMatrixColumn(`object`.matrix, 0)
            offset.crossVectors(`object`.up, offset)
        }
        panOffset.add(offset.multiplyScalar(distance))
    }

    /** Converts a pixel movement into a pan offset; right and down are positive. */
    private fun panPixels(deltaX: Double, deltaY: Double) {
        val height = surface?.height ?: 1.0
        when (val camera = `object`) {
            is PerspectiveCamera -> {
                // Half the vertical field of view spans from the center to the top of the view.
                val targetDistance = offset.copy(camera.position).sub(target).length() *
                    tan(camera.fov / 2.0 * MathUtils.DEG2RAD)
                panLeft(2.0 * deltaX * targetDistance / height)
                panUp(2.0 * deltaY * targetDistance / height)
            }
            is OrthographicCamera -> {
                val width = surface?.width ?: 1.0
                panLeft(deltaX * (camera.right - camera.left) / camera.zoom / width)
                panUp(deltaY * (camera.top - camera.bottom) / camera.zoom / height)
            }
            else -> enablePan = false
        }
    }

    private fun updateZoomParameters(x: Double, y: Double) {
        if (!zoomToCursor) return
        performCursorZoom = true
        val surface = surface ?: return
        mouse.x = (x - surface.left) / surface.width * 2.0 - 1.0
        mouse.y = -(y - surface.top) / surface.height * 2.0 + 1.0
        updateDollyDirection()
    }

    private fun updateDollyDirection() {
        dollyDirection.set(mouse.x, mouse.y, 1.0).unproject(`object`).sub(`object`.position).normalize()
    }

    private val surfaceHeight: Double get() = surface?.height ?: 1.0

    private fun rotateBy(delta: Vector2) {
        // Both directions use the height so that a drag of the same length rotates by the same angle.
        sphericalDelta.theta -= TWO_PI * delta.x / surfaceHeight
        sphericalDelta.phi -= TWO_PI * delta.y / surfaceHeight
    }

    // Mouse input.

    private fun mouseDown(input: PointerInput) {
        val action = when (input.button) {
            0 -> mouseButtons.left
            1 -> mouseButtons.middle
            2 -> mouseButtons.right
            else -> null
        }
        state = when (action) {
            MouseAction.Dolly -> {
                if (!enableZoom) return
                updateZoomParameters(input.x, input.y)
                dollyStart.set(input.x, input.y)
                State.Dolly
            }
            MouseAction.Rotate, MouseAction.Pan -> {
                // A modifier key swaps rotation and panning.
                if ((action == MouseAction.Rotate) == input.hasModifier) {
                    if (!enablePan) return
                    panStart.set(input.x, input.y)
                    State.Pan
                } else {
                    if (!enableRotate) return
                    rotateStart.set(input.x, input.y)
                    State.Rotate
                }
            }
            null -> State.None
        }
        if (state != State.None) dispatchEvent(START)
    }

    private fun mouseMove(input: PointerInput) {
        when (state) {
            State.Rotate -> {
                if (!enableRotate) return
                rotateEnd.set(input.x, input.y)
                rotateDelta.subVectors(rotateEnd, rotateStart).multiplyScalar(rotateSpeed)
                rotateBy(rotateDelta)
                rotateStart.copy(rotateEnd)
                update()
            }
            State.Dolly -> {
                if (!enableZoom) return
                dollyEnd.set(input.x, input.y)
                val deltaY = dollyEnd.y - dollyStart.y
                if (deltaY > 0) dollyOutBy(zoomScale(deltaY)) else if (deltaY < 0) dollyInBy(zoomScale(deltaY))
                dollyStart.copy(dollyEnd)
                if (zoomToCursor) {
                    // update() clears the cursor zoom, so keep zooming toward where the drag started.
                    performCursorZoom = true
                    updateDollyDirection()
                }
                update()
            }
            State.Pan -> {
                if (!enablePan) return
                panEnd.set(input.x, input.y)
                panDelta.subVectors(panEnd, panStart).multiplyScalar(panSpeed)
                panPixels(panDelta.x, panDelta.y)
                panStart.copy(panEnd)
                update()
            }
            else -> Unit
        }
    }

    override fun onWheel(input: WheelInput): Boolean {
        if (!enabled || !enableZoom || state != State.None) return false
        dispatchEvent(START)
        val deltaY = if (input.pinch) input.deltaY * 10.0 else input.deltaY
        updateZoomParameters(input.x, input.y)
        if (deltaY < 0) dollyInBy(zoomScale(deltaY)) else if (deltaY > 0) dollyOutBy(zoomScale(deltaY))
        update()
        dispatchEvent(END)
        return true
    }

    override fun onContextMenu(): Boolean = enabled

    override fun onKeyDown(input: KeyInput): Boolean = false

    /** Pans, or with a modifier rotates, for the configured [keys]. Returns whether the key was handled. */
    private fun handleKeyDown(input: KeyInput): Boolean {
        val modified = input.hasModifier
        val step = TWO_PI * keyRotateSpeed / surfaceHeight
        when (input.code) {
            keys.up -> if (modified) { if (enableRotate) sphericalDelta.phi -= step } else if (enablePan) panPixels(0.0, keyPanSpeed)
            keys.bottom -> if (modified) { if (enableRotate) sphericalDelta.phi += step } else if (enablePan) panPixels(0.0, -keyPanSpeed)
            keys.left -> if (modified) { if (enableRotate) sphericalDelta.theta -= step } else if (enablePan) panPixels(keyPanSpeed, 0.0)
            keys.right -> if (modified) { if (enableRotate) sphericalDelta.theta += step } else if (enablePan) panPixels(-keyPanSpeed, 0.0)
            else -> return false
        }
        update()
        return true
    }

    // Touch input.

    private fun touchCenter(pointerId: Int, position: Vector2, target: Vector2) {
        if (pointers.size == 1) {
            target.copy(position)
        } else {
            val second = secondPointerPosition(pointerId)
            target.set(0.5 * (position.x + second.x), 0.5 * (position.y + second.y))
        }
    }

    private fun touchDistance(pointerId: Int, position: Vector2): Double {
        val second = secondPointerPosition(pointerId)
        val dx = position.x - second.x
        val dy = position.y - second.y
        return sqrt(dx * dx + dy * dy)
    }

    private fun touchStart(pointerId: Int, position: Vector2) {
        trackPointer(pointerId, position)
        state = when (pointers.size) {
            1 -> when (touches.one) {
                TouchAction.Rotate -> if (enableRotate) {
                    touchCenter(pointerId, position, rotateStart)
                    State.TouchRotate
                } else State.None
                TouchAction.Pan -> if (enablePan) {
                    touchCenter(pointerId, position, panStart)
                    State.TouchPan
                } else State.None
                else -> State.None
            }
            2 -> when (touches.two) {
                TouchAction.DollyPan -> {
                    if (!enableZoom && !enablePan) return
                    if (enableZoom) dollyStart.set(0.0, touchDistance(pointerId, position))
                    if (enablePan) touchCenter(pointerId, position, panStart)
                    State.TouchDollyPan
                }
                TouchAction.DollyRotate -> {
                    if (!enableZoom && !enableRotate) return
                    if (enableZoom) dollyStart.set(0.0, touchDistance(pointerId, position))
                    if (enableRotate) touchCenter(pointerId, position, rotateStart)
                    State.TouchDollyRotate
                }
                else -> State.None
            }
            else -> State.None
        }
        if (state != State.None) dispatchEvent(START)
    }

    private fun touchMove(pointerId: Int, position: Vector2) {
        trackPointer(pointerId, position)
        when (state) {
            State.TouchRotate -> if (!enableRotate) return
            State.TouchPan -> if (!enablePan) return
            State.TouchDollyPan -> if (!enableZoom && !enablePan) return
            State.TouchDollyRotate -> if (!enableZoom && !enableRotate) return
            else -> {
                state = State.None
                return
            }
        }
        val dolly = state == State.TouchDollyPan || state == State.TouchDollyRotate
        val rotate = state == State.TouchRotate || (state == State.TouchDollyRotate && enableRotate)
        val pan = state == State.TouchPan || (state == State.TouchDollyPan && enablePan)
        if (dolly && enableZoom) {
            dollyEnd.set(0.0, touchDistance(pointerId, position))
            dollyOutBy((dollyEnd.y / dollyStart.y).pow(zoomSpeed))
            dollyStart.copy(dollyEnd)
            val second = secondPointerPosition(pointerId)
            updateZoomParameters(0.5 * (position.x + second.x), 0.5 * (position.y + second.y))
        }
        if (rotate) {
            touchCenter(pointerId, position, rotateEnd)
            rotateDelta.subVectors(rotateEnd, rotateStart).multiplyScalar(rotateSpeed)
            rotateBy(rotateDelta)
            rotateStart.copy(rotateEnd)
        }
        if (pan) {
            touchCenter(pointerId, position, panEnd)
            panDelta.subVectors(panEnd, panStart).multiplyScalar(panSpeed)
            panPixels(panDelta.x, panDelta.y)
            panStart.copy(panEnd)
        }
        update()
    }

    private fun trackPointer(pointerId: Int, position: Vector2) {
        pointerPositions.getOrPut(pointerId) { Vector2() }.copy(position)
    }

    private fun secondPointerPosition(pointerId: Int): Vector2 {
        val other = if (pointerId == pointers[0]) pointers[1] else pointers[0]
        return pointerPositions.getValue(other)
    }

    // Pointer dispatch.

    override fun onPointerDown(input: PointerInput) {
        if (!enabled || input.pointerId in pointers) return
        pointers.add(input.pointerId)
        if (input.pointerType == PointerType.Touch) touchStart(input.pointerId, Vector2(input.x, input.y)) else mouseDown(input)
        if (cursorStyle == Cursor.Grab) surface?.cursor = Cursor.Grabbing
    }

    override fun onPointerMove(input: PointerInput) {
        if (!enabled) return
        if (input.pointerType == PointerType.Touch) touchMove(input.pointerId, Vector2(input.x, input.y)) else mouseMove(input)
    }

    override fun onPointerUp(input: PointerInput) {
        if (input.pointerId !in pointers) return
        pointerPositions.remove(input.pointerId)
        pointers.remove(input.pointerId)
        when (pointers.size) {
            0 -> {
                dispatchEvent(END)
                state = State.None
                surface?.cursor = idleCursor
            }
            // Restart the gesture with the remaining touch point.
            1 -> {
                val remaining = pointers[0]
                touchStart(remaining, pointerPositions[remaining] ?: return)
            }
        }
    }

    private companion object {
        const val CHANGE = "change"
        const val START = "start"
        const val END = "end"
        const val TWO_PI = 2.0 * PI
        const val EPS = 0.000001
        val TILT_LIMIT = cos(70.0 * MathUtils.DEG2RAD)
    }
}
