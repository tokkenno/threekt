package com.aitorgf.threekt.controls

import com.aitorgf.threekt.cameras.PerspectiveCamera
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TrackballControlsTest {
    @Test
    fun draggingRotatesOverThePoleAndKeepsTheDistance() {
        val camera = PerspectiveCamera().apply { position.z = 10.0 }
        val surface = FakeInputSurface(100.0, 100.0)
        val controls = TrackballControls(camera, surface).apply { staticMoving = true }
        surface.down(1, 50.0, 50.0)
        surface.move(1, 50.0, 0.0)
        controls.update()
        surface.up(1, 50.0, 0.0)
        assertEquals(10.0, camera.position.length(), 1e-9)
        // A half-width vertical drag turns the camera by one radian toward the top, tilting its up vector.
        assertTrue(camera.position.y < 0.0)
        assertTrue(camera.up.z > 0.0)
    }

    @Test
    fun wheelZoomsTowardTheTargetWithDamping() {
        val camera = PerspectiveCamera().apply { position.z = 10.0 }
        val surface = FakeInputSurface(100.0, 100.0)
        val controls = TrackballControls(camera, surface)
        assertTrue(surface.wheel(-400.0))
        controls.update()
        assertTrue(camera.position.z < 10.0)
        controls.reset()
        assertEquals(10.0, camera.position.z)
    }
}
