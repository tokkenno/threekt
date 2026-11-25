package com.aitorgf.threekt.cameras

import com.aitorgf.threekt.math.Matrix4
import com.aitorgf.threekt.math.Vector2
import com.aitorgf.threekt.math.Vector3
import com.aitorgf.threekt.renderers.CoordinateSystem
import kotlin.math.PI
import kotlin.math.atan
import kotlin.test.Test
import kotlin.test.assertEquals

class CalibratedPerspectiveCameraTest {
    @Test
    fun centeredFilmMatchesAStandardPerspectiveCamera() {
        val camera = CalibratedPerspectiveCamera(50.0, 1.5, 0.5, 30.0)
        val fov = 2 * atan(camera.filmSize.y / (2 * camera.focal)) * 180 / PI
        val standard = PerspectiveCamera(fov, camera.filmAR, camera.near, camera.far)
        for (system in CoordinateSystem.entries) {
            for (reversed in listOf(false, true)) {
                camera.coordinateSystem = system
                camera.reversedDepth = reversed
                standard.coordinateSystem = system
                standard.reversedDepth = reversed
                camera.updateProjectionMatrix()
                standard.updateProjectionMatrix()
                for (i in 0..15) assertEquals(standard.projectionMatrix.elements[i], camera.projectionMatrix.elements[i], 1e-12)
            }
        }
    }

    @Test
    fun cropZoomPanAndPrincipalPointMatchTheSiblingEngineComposition() {
        val camera = CalibratedPerspectiveCamera(50.0, 2.0, 0.5, 20.0)
        camera.setCropArea(0.10, 0.25, 0.90, 0.75)
        camera.principalPoint = Vector2(0.1, -0.2)
        camera.zoom = 2.0
        camera.pan = Vector2(0.4, -0.6)
        camera.updateProjectionMatrix()

        // Bounds in normalized film units before applying the intrinsic matrix.
        val expected = Matrix4().makeOrthographic(-0.12, 0.28, 0.05, -0.20, 0.5, 20.0)
            .multiply(Matrix4().set(
                50.0 / 35, 0.0, -0.1, 0.0,
                0.0, 100.0 / 35, 0.2, 0.0,
                0.0, 0.0, 20.5, 10.0,
                0.0, 0.0, -1.0, 0.0,
            ))
        for (i in 0..15) assertEquals(expected.elements[i], camera.projectionMatrix.elements[i], 1e-12)
        assertEquals(3.2, camera.aspectRatio, 1e-12)
        val point = Vector3(0.5, -0.1, -4.0).applyMatrix4(camera.projectionMatrix)
            .applyMatrix4(camera.projectionMatrixInverse)
        assertEquals(0.5, point.x, 1e-12)
        assertEquals(-0.1, point.y, 1e-12)
        assertEquals(-4.0, point.z, 1e-12)
    }
}
