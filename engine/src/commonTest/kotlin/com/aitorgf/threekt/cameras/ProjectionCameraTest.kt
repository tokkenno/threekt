package com.aitorgf.threekt.cameras

import com.aitorgf.threekt.core.Object3D
import com.aitorgf.threekt.math.Matrix4
import com.aitorgf.threekt.math.Vector2
import com.aitorgf.threekt.math.Vector3
import com.aitorgf.threekt.renderers.CoordinateSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame

class ProjectionCameraTest {
    @Test
    fun perspectiveFilmDimensionsAndEffectiveFovAccountForAspectAndZoom() {
        val camera = PerspectiveCamera(fov = 90.0, aspect = 0.5)
        camera.filmGauge = 40.0
        camera.zoom = 2.0
        assertEquals(20.0, camera.getFilmWidth())
        assertEquals(40.0, camera.getFilmHeight())
        assertEquals(53.13010235415598, camera.getEffectiveFOV(), 1e-12)

        camera.aspect = 2.0
        assertEquals(40.0, camera.getFilmWidth())
        assertEquals(20.0, camera.getFilmHeight())
        val target = Vector2()
        camera.updateProjectionMatrix()
        assertSame(target, camera.getViewSize(2.0, target))
        assertEquals(4.0, target.x, 1e-12)
        assertEquals(2.0, target.y, 1e-12)
    }

    @Test
    fun projectionsMapNearAndFarPlanesForBothCoordinateConventions() {
        val cameras = listOf(
            PerspectiveCamera(near = 1.0, far = 9.0),
            OrthographicCamera(near = 1.0, far = 9.0),
        )
        for (camera in cameras) {
            for (system in CoordinateSystem.entries) {
                camera.coordinateSystem = system
                for (reversed in listOf(false, true)) {
                    camera.reversedDepth = reversed
                    camera.updateProjectionMatrix()
                    val nearDepth = when {
                        reversed -> 1.0
                        system == CoordinateSystem.WebGL -> -1.0
                        else -> 0.0
                    }
                    val farDepth = if (reversed) 0.0 else 1.0
                    assertEquals(nearDepth, Vector3(0.0, 0.0, -1.0).project(camera).z, 1e-12)
                    assertEquals(farDepth, Vector3(0.0, 0.0, -9.0).project(camera).z, 1e-12)
                    assertEquals(-9.0, Vector3(0.0, 0.0, farDepth).unproject(camera).z, 1e-12)
                }
            }
        }
    }

    @Test
    fun invalidViewOffsetPreservesTheLastValidProjection() {
        val camera = PerspectiveCamera()
        camera.setViewOffset(200, 100, 100, 0, 100, 50)
        val view = camera.viewOffset
        val projection = camera.projectionMatrix.copy()
        val inverse = camera.projectionMatrixInverse.copy()

        assertFailsWith<IllegalArgumentException> {
            camera.setViewOffset(200, 0, 100, 0, 100, 50)
        }
        assertFailsWith<IllegalArgumentException> {
            camera.setViewOffset(200, 100, 100, 0, 100, 0)
        }
        assertEquals(view, camera.viewOffset)
        assertEquals(projection, camera.projectionMatrix)
        assertEquals(inverse, camera.projectionMatrixInverse)
        assertEquals(2.0, camera.aspect)
        camera.clearViewOffset()
        assertNull(camera.viewOffset)
    }

    @Test
    fun perspectiveTileAndFilmOffsetShiftViewBounds() {
        val camera = PerspectiveCamera(fov = 90.0, near = 1.0)
        camera.setViewOffset(200, 100, 100, 0, 100, 100)
        val min = Vector2()
        val max = Vector2()
        camera.getViewBounds(2.0, min, max)
        assertEquals(0.0, min.x, 1e-12)
        assertEquals(4.0, max.x, 1e-12)
        assertEquals(4.0, camera.getViewSize(2.0, Vector2()).y, 1e-12)

        camera.clearViewOffset()
        camera.filmOffset = camera.getFilmWidth() / 2.0
        camera.updateProjectionMatrix()
        camera.getViewBounds(2.0, min, max)
        assertEquals(-3.0, min.x, 1e-12)
        assertEquals(5.0, max.x, 1e-12)
        camera.setFocalLength(45.0)
        assertEquals(45.0, camera.getFocalLength(), 1e-12)
    }

    @Test
    fun orthographicViewSupportsZoomTilingAndUnprojection() {
        val camera = OrthographicCamera(-2.0, 2.0, 1.0, -1.0, 0.0, 10.0)
        camera.zoom = 2.0
        camera.setViewOffset(200, 100, 100, 0, 100, 100)
        val point = Vector3(0.25, 0.0, -5.0).project(camera)
        assertEquals(-0.5, point.x, 1e-12)
        assertEquals(0.0, point.y, 1e-12)
        assertEquals(0.0, point.z, 1e-12)
        assertEquals(0.25, point.unproject(camera).x, 1e-12)
        camera.clearViewOffset()
        assertEquals(0.25, Vector3(0.25, 0.0, -8.0).project(camera).x, 1e-12)
    }

    @Test
    fun orthographicTileOffsetsBothAxesOfAnAsymmetricFrustum() {
        val camera = OrthographicCamera(0.0, 8.0, 6.0, 2.0, 1.0, 9.0)
        camera.setViewOffset(400, 200, 100, 50, 200, 100)
        val upperLeft = Vector3(2.0, 5.0, -1.0).project(camera)
        val lowerRight = Vector3(6.0, 3.0, -9.0).project(camera)
        assertEquals(Vector3(-1.0, 1.0, -1.0), upperLeft)
        assertEquals(Vector3(1.0, -1.0, 1.0), lowerRight)
    }

    @Test
    fun cameraViewExcludesInheritedScaleAndUpdatesAncestors() {
        val parent = Object3D()
        val camera = PerspectiveCamera()
        parent.scale.set(2.0, 3.0, 4.0)
        parent.position.x = 5.0
        camera.position.z = 1.0
        parent.add(camera)
        val direction = camera.getWorldDirection(Vector3())
        assertEquals(0.0, direction.x, 1e-12)
        assertEquals(0.0, direction.y, 1e-12)
        assertEquals(-1.0, direction.z, 1e-12)
        val inView = Vector3(6.0, 3.0, 2.0).applyMatrix4(camera.matrixWorldInverse)
        assertEquals(Vector3(1.0, 3.0, -2.0), inView)
    }

    @Test
    fun cameraViewHandlesReflectionsAndSingularWorldTransforms() {
        val camera = OrthographicCamera()
        camera.position.set(2.0, 3.0, 4.0)
        camera.scale.set(-1.0, 1.0, 1.0)
        camera.updateMatrixWorld()
        assertEquals(Vector3(1.0, 0.0, -1.0), Vector3(3.0, 3.0, 3.0).applyMatrix4(camera.matrixWorldInverse))

        camera.scale.x = 0.0
        camera.updateMatrixWorld()
        assertEquals(Matrix4().multiplyScalar(0.0), camera.matrixWorldInverse)
    }
}
