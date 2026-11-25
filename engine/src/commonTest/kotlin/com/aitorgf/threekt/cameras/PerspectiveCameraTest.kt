package com.aitorgf.threekt.cameras

import com.aitorgf.threekt.core.Object3D
import com.aitorgf.threekt.math.Vector3
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals

class PerspectiveCameraTest {
    @Test
    fun projectionMapsFrustumEdgesToWebGlClipSpace() {
        val camera = PerspectiveCamera(fov = 90.0, aspect = 2.0, near = 1.0, far = 11.0)
        val nearCorner = Vector3(2.0, 1.0, -1.0).applyMatrix4(camera.projectionMatrix)
        val farCenter = Vector3(0.0, 0.0, -11.0).applyMatrix4(camera.projectionMatrix)

        assertEquals(1.0, nearCorner.x, 1e-12)
        assertEquals(1.0, nearCorner.y, 1e-12)
        assertEquals(-1.0, nearCorner.z, 1e-12)
        assertEquals(1.0, farCenter.z, 1e-12)
    }

    @Test
    fun projectionAndInverseReflectAspectAndZoomChanges() {
        val camera = PerspectiveCamera(fov = 90.0)
        camera.aspect = 4.0
        camera.zoom = 2.0
        camera.updateProjectionMatrix()

        val point = Vector3(2.0, 0.5, -2.0).applyMatrix4(camera.projectionMatrix)
        assertEquals(0.5, point.x, 1e-12)
        assertEquals(0.5, point.y, 1e-12)

        point.applyMatrix4(camera.projectionMatrixInverse)
        assertEquals(2.0, point.x, 1e-12)
        assertEquals(0.5, point.y, 1e-12)
        assertEquals(-2.0, point.z, 1e-12)
    }

    @Test
    fun viewMatrixTracksCameraUnderTranslatedAndRotatedParent() {
        val root = Object3D()
        val camera = PerspectiveCamera(fov = 90.0, near = 1.0, far = 11.0)
        root.position.set(10.0, 2.0, 3.0)
        root.quaternion.setFromAxisAngle(Vector3(0.0, 1.0, 0.0), PI / 2)
        camera.position.z = 5.0
        root.add(camera)
        root.updateMatrixWorld()

        val point = Vector3(14.0, 2.0, 3.0)
            .applyMatrix4(camera.matrixWorldInverse)
            .applyMatrix4(camera.projectionMatrix)
        assertEquals(0.0, point.x, 1e-12)
        assertEquals(0.0, point.y, 1e-12)
        assertEquals(-1.0, point.z, 1e-12)
    }
}
