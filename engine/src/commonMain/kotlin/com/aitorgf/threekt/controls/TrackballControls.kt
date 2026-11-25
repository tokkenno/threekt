package com.aitorgf.threekt.controls

import com.aitorgf.threekt.cameras.Camera
import com.aitorgf.threekt.cameras.OrthographicCamera
import com.aitorgf.threekt.cameras.PerspectiveCamera
import com.aitorgf.threekt.math.Quaternion
import com.aitorgf.threekt.math.Vector2
import com.aitorgf.threekt.math.Vector3
import kotlin.js.JsExport
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Rotates a camera freely around [target], as the Three.js addon `TrackballControls`. Unlike
 * [OrbitControls], it does not keep the camera's up direction, so the camera can turn over the poles.
 * The primary button rotates, the middle button or wheel zooms and the secondary button pans; one finger
 * rotates and two fingers zoom and pan. Call [update] on every frame.
 *
 * Input comes from an [InputSurface], whose bounds replace the Three.js `screen` rectangle and
 * `handleResize()`. The A, S and D keys that upstream holds to switch modes are not supported, because
 * input sources do not report key releases yet.
 */
@JsExport
class TrackballControls(camera: Camera, surface: InputSurface? = null) : Controls<Camera>(camera) {
    @JsExport.Ignore
    private enum class State { None, Rotate, Zoom, Pan, TouchRotate, TouchZoomPan }

    /** Multiplier for rotation speed. */
    var rotateSpeed = 1.0
    /** Multiplier for zoom speed. */
    var zoomSpeed = 1.2
    /** Multiplier for pan speed. */
    var panSpeed = 0.3
    /** Multiplier for the two-finger roll speed. */
    var rollSpeed = 1.0
    /** Disables rotation. */
    var noRotate = false
    /** Disables zooming. */
    var noZoom = false
    /** Disables panning. */
    var noPan = false
    /** Whether twisting two fingers rolls the camera. */
    var multiTouchRoll = false
    /** Whether motion stops with the input instead of slowing down with [dynamicDampingFactor]. */
    var staticMoving = false
    /** How quickly motion slows down when [staticMoving] is false. */
    var dynamicDampingFactor = 0.2
    /** Smallest distance from [target] for a perspective camera. */
    var minDistance = 0.0
    /** Largest distance from [target] for a perspective camera. */
    var maxDistance = Double.POSITIVE_INFINITY
    /** Smallest zoom of an orthographic camera. */
    var minZoom = 0.0
    /** Largest zoom of an orthographic camera. */
    var maxZoom = Double.POSITIVE_INFINITY
    /** Actions for mouse buttons. */
    var mouseButtons = MouseButtons()

    /** Point the camera looks at and rotates around. */
    val target = Vector3()

    private var state = State.None
    private val lastPosition = Vector3()
    private val lastUp = Vector3()
    private var lastZoom = 1.0
    private var touchZoomDistanceStart = 0.0
    private var touchZoomDistanceEnd = 0.0
    private var touchRollAngle = 0.0
    private var touchRollPointerIds = ""
    private var lastAngle = 0.0
    private var lastRollAngle = 0.0
    private val eye = Vector3()
    private val movePrev = Vector2()
    private val moveCurr = Vector2()
    private val lastAxis = Vector3()
    private val zoomStart = Vector2()
    private val zoomEnd = Vector2()
    private val panStart = Vector2()
    private val panEnd = Vector2()
    private val pointers = mutableListOf<PointerInput>()
    private val pointerPositions = mutableMapOf<Int, Vector2>()

    private val target0 = target.copy()
    private val position0 = camera.position.copy()
    private val up0 = camera.up.copy()
    private val zoom0 = camera.zoom

    private val v2 = Vector2()
    private val mouseChange = Vector2()
    private val objectUp = Vector3()
    private val pan = Vector3()
    private val axis = Vector3()
    private val quaternion = Quaternion()
    private val eyeDirection = Vector3()
    private val objectUpDirection = Vector3()
    private val objectSidewaysDirection = Vector3()
    private val moveDirection = Vector3()

    init {
        if (surface != null) connect(surface)
        update()
    }

    override fun onDisconnect(surface: InputSurface) {
        state = State.None
        pointers.clear()
        pointerPositions.clear()
    }

    /**
     * Applies pending rotation, zoom and pan, points the camera at [target] and dispatches `change` when
     * the camera moved. Returns whether it moved.
     */
    override fun update(deltaTime: Double?): Boolean {
        val camera = `object`
        eye.subVectors(camera.position, target)
        if (!noRotate) {
            rotateCamera()
            if (multiTouchRoll) rollCamera()
        }
        if (!noZoom) zoomCamera()
        if (!noPan) panCamera()
        camera.position.addVectors(target, eye)
        if (camera is PerspectiveCamera) checkDistances()
        camera.lookAt(target)
        val positionChanged = lastPosition.distanceToSquared(camera.position) > EPS
        val rollChanged = lastUp.distanceToSquared(camera.up) > EPS
        val zoomChanged = camera is OrthographicCamera && lastZoom != camera.zoom
        if (positionChanged || rollChanged || zoomChanged) {
            dispatchEvent(CHANGE)
            lastPosition.copy(camera.position)
            lastUp.copy(camera.up)
            lastZoom = camera.zoom
            return true
        }
        return false
    }

    /** Restores the target, camera position, up direction and zoom from construction. */
    fun reset() {
        val camera = `object`
        state = State.None
        target.copy(target0)
        camera.position.copy(position0)
        camera.up.copy(up0)
        camera.zoom = zoom0
        camera.updateProjectionMatrix()
        eye.subVectors(camera.position, target)
        camera.lookAt(target)
        dispatchEvent(CHANGE)
        lastPosition.copy(camera.position)
        lastUp.copy(camera.up)
        lastZoom = camera.zoom
    }

    private fun panCamera() {
        val camera = `object`
        mouseChange.copy(panEnd).sub(panStart)
        if (mouseChange.lengthSq() == 0.0) return
        if (camera is OrthographicCamera) {
            val width = surface?.width ?: 1.0
            mouseChange.x *= (camera.right - camera.left) / camera.zoom / width
            mouseChange.y *= (camera.top - camera.bottom) / camera.zoom / width
        }
        mouseChange.multiplyScalar(eye.length() * panSpeed)
        pan.copy(eye).cross(camera.up).setLength(mouseChange.x)
        pan.add(objectUp.copy(camera.up).setLength(mouseChange.y))
        camera.position.add(pan)
        target.add(pan)
        if (staticMoving) {
            panStart.copy(panEnd)
        } else {
            panStart.add(mouseChange.subVectors(panEnd, panStart).multiplyScalar(dynamicDampingFactor))
        }
    }

    private fun rotateCamera() {
        val camera = `object`
        moveDirection.set(moveCurr.x - movePrev.x, moveCurr.y - movePrev.y, 0.0)
        var angle = moveDirection.length()
        if (angle != 0.0) {
            eye.copy(camera.position).sub(target)
            eyeDirection.copy(eye).normalize()
            objectUpDirection.copy(camera.up).normalize()
            objectSidewaysDirection.crossVectors(objectUpDirection, eyeDirection).normalize()
            objectUpDirection.setLength(moveCurr.y - movePrev.y)
            objectSidewaysDirection.setLength(moveCurr.x - movePrev.x)
            moveDirection.copy(objectUpDirection.add(objectSidewaysDirection))
            axis.crossVectors(moveDirection, eye).normalize()
            angle *= rotateSpeed
            quaternion.setFromAxisAngle(axis, angle)
            eye.applyQuaternion(quaternion)
            camera.up.applyQuaternion(quaternion)
            lastAxis.copy(axis)
            lastAngle = angle
        } else if (!staticMoving && lastAngle != 0.0) {
            lastAngle *= sqrt(1.0 - dynamicDampingFactor)
            eye.copy(camera.position).sub(target)
            quaternion.setFromAxisAngle(lastAxis, lastAngle)
            eye.applyQuaternion(quaternion)
            camera.up.applyQuaternion(quaternion)
        }
        movePrev.copy(moveCurr)
    }

    private fun rollCamera() {
        val angle = touchRollAngle()
        if (angle != 0.0) {
            lastRollAngle = angle * rollSpeed
        } else if (!staticMoving && lastRollAngle != 0.0) {
            lastRollAngle *= sqrt(1.0 - dynamicDampingFactor)
        } else {
            return
        }
        eyeDirection.copy(eye).normalize()
        quaternion.setFromAxisAngle(eyeDirection, lastRollAngle)
        `object`.up.applyQuaternion(quaternion)
    }

    private fun touchRollAngle(): Double {
        val first = pointers.getOrNull(0)
        val second = pointers.getOrNull(1)
        if (first == null || second == null || first.pointerType != PointerType.Touch || second.pointerType != PointerType.Touch) {
            touchRollPointerIds = ""
            return 0.0
        }
        val pointerIds = "${first.pointerId},${second.pointerId}"
        val positionFirst = pointerPositions.getValue(first.pointerId)
        val positionSecond = pointerPositions.getValue(second.pointerId)
        val rollAngle = atan2(positionSecond.y - positionFirst.y, positionSecond.x - positionFirst.x)
        if (pointerIds != touchRollPointerIds) {
            touchRollPointerIds = pointerIds
            touchRollAngle = rollAngle
            return 0.0
        }
        var angle = rollAngle - touchRollAngle
        touchRollAngle = rollAngle
        if (angle > PI) angle -= 2 * PI else if (angle < -PI) angle += 2 * PI
        return angle
    }

    private fun zoomCamera() {
        val camera = `object`
        if (state == State.TouchZoomPan) {
            val factor = touchZoomDistanceStart / touchZoomDistanceEnd
            touchZoomDistanceStart = touchZoomDistanceEnd
            applyZoom(camera, factor)
        } else {
            val factor = 1.0 + (zoomEnd.y - zoomStart.y) * zoomSpeed
            if (factor != 1.0 && factor > 0.0) applyZoom(camera, factor)
            if (staticMoving) zoomStart.copy(zoomEnd) else zoomStart.y += (zoomEnd.y - zoomStart.y) * dynamicDampingFactor
        }
    }

    private fun applyZoom(camera: Camera, factor: Double) {
        if (camera is OrthographicCamera) {
            camera.zoom = (camera.zoom / factor).coerceIn(minZoom, maxZoom)
            if (lastZoom != camera.zoom) camera.updateProjectionMatrix()
        } else {
            eye.multiplyScalar(factor)
        }
    }

    private fun mouseOnScreen(x: Double, y: Double): Vector2 {
        val surface = surface ?: return v2.set(0.0, 0.0)
        return v2.set((x - surface.left) / surface.width, (y - surface.top) / surface.height)
    }

    private fun mouseOnCircle(x: Double, y: Double): Vector2 {
        val surface = surface ?: return v2.set(0.0, 0.0)
        // Both coordinates are divided by the width, as in Three.js.
        return v2.set(
            (x - surface.width * 0.5 - surface.left) / (surface.width * 0.5),
            (surface.height + 2 * (surface.top - y)) / surface.width,
        )
    }

    private fun checkDistances() {
        if (noZoom && noPan) return
        val camera = `object`
        if (eye.lengthSq() > maxDistance * maxDistance) {
            camera.position.addVectors(target, eye.setLength(maxDistance))
            zoomStart.copy(zoomEnd)
        }
        if (eye.lengthSq() < minDistance * minDistance) {
            camera.position.addVectors(target, eye.setLength(minDistance))
            zoomStart.copy(zoomEnd)
        }
    }

    // Pointer dispatch.

    override fun onPointerDown(input: PointerInput) {
        if (!enabled || pointers.any { it.pointerId == input.pointerId }) return
        pointers.add(input)
        if (input.pointerType == PointerType.Touch) touchStart(input) else mouseDown(input)
    }

    override fun onPointerMove(input: PointerInput) {
        if (!enabled || pointers.none { it.pointerId == input.pointerId }) return
        if (input.pointerType == PointerType.Touch) touchMove(input) else mouseMove(input)
    }

    override fun onPointerUp(input: PointerInput) {
        if (!enabled || pointers.none { it.pointerId == input.pointerId }) return
        if (input.pointerType == PointerType.Touch) touchEnd(input) else mouseUp()
        pointerPositions.remove(input.pointerId)
        pointers.removeAll { it.pointerId == input.pointerId }
    }

    override fun onWheel(input: WheelInput): Boolean {
        if (!enabled || noZoom) return false
        // Pixel deltas, the DOM_DELTA_PIXEL case of Three.js.
        zoomStart.y -= input.deltaY * 0.00025
        dispatchEvent(START)
        dispatchEvent(END)
        return true
    }

    override fun onContextMenu(): Boolean = enabled

    private fun mouseDown(input: PointerInput) {
        val action = when (input.button) {
            0 -> mouseButtons.left
            1 -> mouseButtons.middle
            2 -> mouseButtons.right
            else -> null
        }
        state = when (action) {
            MouseAction.Dolly -> State.Zoom
            MouseAction.Rotate -> State.Rotate
            MouseAction.Pan -> State.Pan
            null -> State.None
        }
        when {
            state == State.Rotate && !noRotate -> {
                moveCurr.copy(mouseOnCircle(input.x, input.y))
                movePrev.copy(moveCurr)
            }
            state == State.Zoom && !noZoom -> {
                zoomStart.copy(mouseOnScreen(input.x, input.y))
                zoomEnd.copy(zoomStart)
            }
            state == State.Pan && !noPan -> {
                panStart.copy(mouseOnScreen(input.x, input.y))
                panEnd.copy(panStart)
            }
        }
        dispatchEvent(START)
    }

    private fun mouseMove(input: PointerInput) {
        when {
            state == State.Rotate && !noRotate -> moveCurr.copy(mouseOnCircle(input.x, input.y))
            state == State.Zoom && !noZoom -> zoomEnd.copy(mouseOnScreen(input.x, input.y))
            state == State.Pan && !noPan -> panEnd.copy(mouseOnScreen(input.x, input.y))
        }
    }

    private fun mouseUp() {
        state = State.None
        dispatchEvent(END)
    }

    private fun trackPointer(input: PointerInput) {
        pointerPositions.getOrPut(input.pointerId) { Vector2() }.set(input.x, input.y)
    }

    private fun secondPointerPosition(input: PointerInput): Vector2 {
        val pointer = if (input.pointerId == pointers[0].pointerId) pointers[1] else pointers[0]
        return pointerPositions.getValue(pointer.pointerId)
    }

    private fun touchStart(input: PointerInput) {
        trackPointer(input)
        if (pointers.size == 1) {
            state = State.TouchRotate
            moveCurr.copy(mouseOnCircle(pointers[0].x, pointers[0].y))
            movePrev.copy(moveCurr)
        } else {
            state = State.TouchZoomPan
            val first = pointerPositions[pointers[0].pointerId] ?: Vector2(pointers[0].x, pointers[0].y)
            val second = pointerPositions[pointers[1].pointerId] ?: Vector2(pointers[1].x, pointers[1].y)
            val dx = first.x - second.x
            val dy = first.y - second.y
            touchZoomDistanceStart = sqrt(dx * dx + dy * dy)
            touchZoomDistanceEnd = touchZoomDistanceStart
            panStart.copy(mouseOnScreen((first.x + second.x) / 2, (first.y + second.y) / 2))
            panEnd.copy(panStart)
        }
        dispatchEvent(START)
    }

    private fun touchMove(input: PointerInput) {
        trackPointer(input)
        if (pointers.size == 1) {
            moveCurr.copy(mouseOnCircle(input.x, input.y))
        } else {
            val position = secondPointerPosition(input)
            val dx = input.x - position.x
            val dy = input.y - position.y
            touchZoomDistanceEnd = sqrt(dx * dx + dy * dy)
            panEnd.copy(mouseOnScreen((input.x + position.x) / 2, (input.y + position.y) / 2))
        }
    }

    /** Runs before the lifted pointer is removed, so the counts match Three.js `onTouchEnd`. */
    private fun touchEnd(input: PointerInput) {
        when (pointers.size) {
            0 -> state = State.None
            1 -> {
                state = State.TouchRotate
                moveCurr.copy(mouseOnCircle(input.x, input.y))
                movePrev.copy(moveCurr)
            }
            2 -> {
                state = State.TouchZoomPan
                val other = pointers.firstOrNull { it.pointerId != input.pointerId }
                val position = other?.let { pointerPositions[it.pointerId] }
                if (position != null) {
                    moveCurr.copy(mouseOnCircle(position.x, position.y))
                    movePrev.copy(moveCurr)
                }
            }
        }
        dispatchEvent(END)
    }

    private companion object {
        const val CHANGE = "change"
        const val START = "start"
        const val END = "end"
        const val EPS = 0.000001
    }
}
