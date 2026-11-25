package com.aitorgf.threekt.controls

import com.aitorgf.threekt.cameras.OrthographicCamera
import com.aitorgf.threekt.cameras.PerspectiveCamera
import com.aitorgf.threekt.math.Vector3
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OrbitControlsTest {
    private fun camera() = PerspectiveCamera(40.0, 2.0, 1.0, 1000.0).apply { position.set(0.0, 0.0, 10.0) }

    @Test
    fun updateKeepsTheDistanceAndLooksAtTheTarget() {
        val camera = camera()
        val controls = OrbitControls(camera)
        controls.rotateLeft(PI / 2)
        assertEquals(10.0, controls.getDistance(), 1e-9)
        assertEquals(-10.0, camera.position.x, 1e-9)
        assertEquals(0.0, camera.position.z, 1e-9)
        assertEquals(1.0, camera.getWorldDirection(Vector3()).x, 1e-9)
    }

    @Test
    fun distanceAndPolarLimitsAreApplied() {
        val camera = camera()
        val controls = OrbitControls(camera)
        controls.minDistance = 5.0
        controls.maxDistance = 20.0
        controls.maxPolarAngle = PI / 2
        controls.dollyIn(10.0)
        assertEquals(20.0, controls.getDistance(), 1e-9)
        controls.dollyOut(100.0)
        assertEquals(5.0, controls.getDistance(), 1e-9)
        controls.rotateUp(-PI)
        assertEquals(PI / 2, controls.getPolarAngle(), 1e-6)
        assertTrue(camera.position.y >= -1e-9)
    }

    @Test
    fun orthographicCamerasZoomInsteadOfMoving() {
        val camera = OrthographicCamera().apply { position.set(0.0, 0.0, 10.0) }
        val controls = OrbitControls(camera)
        controls.dollyOut(2.0)
        assertEquals(2.0, camera.zoom, 1e-12)
        assertEquals(10.0, controls.getDistance(), 1e-9)
    }

    @Test
    fun changeIsDispatchedOnlyWhenTheCameraMoves() {
        val controls = OrbitControls(camera())
        var changes = 0
        controls.addEventListener("change") { changes++ }
        assertFalse(controls.update())
        controls.rotateLeft(0.5)
        assertEquals(1, changes)
        controls.saveState()
        controls.rotateLeft(0.5)
        // As in Three.js, reset() dispatches change and then update() dispatches it again for the move.
        controls.reset()
        assertEquals(4, changes)
        assertEquals(0.5, -controls.getAzimuthalAngle(), 1e-9)
    }

    @Test
    fun mouseDragRotatesAndModifiedDragPans() {
        val surface = FakeInputSurface()
        val controls = OrbitControls(camera(), surface)
        val events = mutableListOf<String>()
        controls.addEventListener("start") { events.add("start") }
        controls.addEventListener("end") { events.add("end") }

        surface.down(1, 100.0, 50.0)
        surface.move(1, 125.0, 50.0)
        surface.up(1, 125.0, 50.0)
        // A 25 px drag on a 100 px tall surface rotates by a quarter turn.
        assertEquals(-PI / 2, controls.getAzimuthalAngle(), 1e-9)
        assertEquals(listOf("start", "end"), events)

        surface.down(1, 100.0, 50.0, shift = true)
        surface.move(1, 100.0, 60.0)
        surface.up(1, 100.0, 60.0)
        assertTrue(controls.target.y > 0.0, "Dragging down with Shift pans the target up.")
        assertEquals(10.0, controls.getDistance(), 1e-9)
    }

    @Test
    fun wheelDolliesAndPinchesZoomTenTimesFaster() {
        val surface = FakeInputSurface()
        val controls = OrbitControls(camera(), surface)
        assertTrue(surface.wheel(-100.0))
        assertEquals(10.0 * 0.95, controls.getDistance(), 1e-9)
        surface.wheel(-10.0, pinch = true)
        assertEquals(10.0 * 0.95 * 0.95, controls.getDistance(), 1e-9)
        controls.enableZoom = false
        assertFalse(surface.wheel(-100.0))
    }

    @Test
    fun twoFingerPinchDolliesAndReleasingOneFingerRestartsRotation() {
        val surface = FakeInputSurface()
        val controls = OrbitControls(camera(), surface)
        surface.down(1, 90.0, 50.0, PointerType.Touch)
        surface.down(2, 110.0, 50.0, PointerType.Touch)
        surface.move(2, 130.0, 50.0, PointerType.Touch)
        // Doubling the finger distance halves the camera distance.
        assertEquals(5.0, controls.getDistance(), 1e-9)

        surface.up(2, 130.0, 50.0, PointerType.Touch)
        surface.move(1, 115.0, 50.0, PointerType.Touch)
        assertEquals(-PI / 2, controls.getAzimuthalAngle(), 1e-9)
    }

    @Test
    fun keysPanOrRotateOnlyAfterListeningAndDisconnectStopsInput() {
        val surface = FakeInputSurface()
        val keys = FakeInputSurface()
        val controls = OrbitControls(camera(), surface)
        assertFalse(keys.key("ArrowLeft"))
        controls.listenToKeyEvents(keys)
        assertTrue(keys.key("ArrowLeft"))
        assertTrue(controls.target.x < 0.0)
        assertTrue(keys.key("ArrowLeft", shift = true))
        assertTrue(controls.getAzimuthalAngle() < 0.0)
        assertFalse(keys.key("KeyA"))
        // Keys never arrive through the pointer surface.
        assertFalse(surface.key("ArrowLeft"))

        controls.dispose()
        assertTrue(surface.listeners.isEmpty())
        assertTrue(keys.listeners.isEmpty())
    }

    @Test
    fun grabCursorStyleShowsGrabbingWhileDragging() {
        val surface = FakeInputSurface()
        val controls = OrbitControls(camera(), surface)
        controls.cursorStyle = Cursor.Grab
        assertEquals(Cursor.Grab, surface.cursor)
        surface.down(1, 0.0, 0.0)
        assertEquals(Cursor.Grabbing, surface.cursor)
        surface.up(1, 0.0, 0.0)
        assertEquals(Cursor.Grab, surface.cursor)
        controls.dispose()
        assertEquals(Cursor.Auto, surface.cursor)
    }
}
