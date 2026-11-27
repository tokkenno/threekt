package com.aitorgf.threekt.cameras

import com.aitorgf.threekt.core.Object3D
import com.aitorgf.threekt.math.FrustumArray
import com.aitorgf.threekt.math.Vector3
import com.aitorgf.threekt.renderers.CoordinateSystem
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MultiViewCameraTest {
    @Test
    fun stereoUsesTheSourceDepthConventionAndAccountsForParentedEyes() {
        val source = PerspectiveCamera(fov = 90.0, aspect = 2.0, near = 1.0, far = 9.0)
        source.coordinateSystem = CoordinateSystem.WebGPU
        source.reversedDepth = true
        source.updateProjectionMatrix()
        source.position.x = 2.0
        val stereo = StereoCamera().apply {
            aspect = 0.5
            eyeSep = 0.2
        }
        val rig = Object3D()
        rig.position.x = 10.0
        rig.add(stereo.cameraL).add(stereo.cameraR)
        stereo.update(source)

        assertEquals(-8.1, stereo.cameraL.matrix.elements[12], 1e-12)
        assertEquals(1.9, stereo.cameraL.matrixWorld.elements[12], 1e-12)
        assertEquals(2.1, stereo.cameraR.matrixWorld.elements[12], 1e-12)
        for (eye in listOf(stereo.cameraL, stereo.cameraR)) {
            assertEquals(CoordinateSystem.WebGPU, eye.coordinateSystem)
            assertTrue(eye.reversedDepth)
            assertEquals(1.0, eye.aspect, 1e-12)
            assertEquals(1.0, eye.projectionMatrix.elements[0], 1e-12)
            assertEquals(0.0, Vector3(2.0, 0.0, -9.0).project(eye).z, 1e-12)
        }
    }

    @Test
    fun invalidStereoParametersDoNotChangeTheLastValidEyeProjection() {
        val source = PerspectiveCamera()
        val stereo = StereoCamera()
        stereo.update(source)
        val projection = stereo.cameraL.projectionMatrix.copy()
        val pose = stereo.cameraL.matrixWorld.copy()

        source.focus = 0.0
        assertFailsWith<IllegalArgumentException> { stereo.update(source) }
        assertFailsWith<IllegalArgumentException> { stereo.update(stereo.cameraL) }
        assertEquals(projection, stereo.cameraL.projectionMatrix)
        assertEquals(pose, stereo.cameraL.matrixWorld)
    }

    @Test
    fun arrayViewportRequiresPositiveDimensions() {
        assertFailsWith<IllegalArgumentException> { ArrayCamera.Viewport(0, 0, 0, 16) }
        assertFailsWith<IllegalArgumentException> { ArrayCamera.Viewport(0, 0, 16, -1) }
    }

    @Test
    fun stereoEyesAreOffsetAlongTheCameraAxisAndConvergeAtFocus() {
        val parent = Object3D()
        parent.position.x = 3.0
        parent.quaternion.setFromAxisAngle(Vector3(0.0, 1.0, 0.0), PI / 2)
        val source = PerspectiveCamera(fov = 90.0, near = 1.0)
        source.focus = 5.0
        parent.add(source)
        val stereo = StereoCamera().apply { eyeSep = 0.2 }
        stereo.update(source)

        assertEquals(0.1, stereo.cameraL.matrixWorld.elements[14], 1e-12)
        assertEquals(-0.1, stereo.cameraR.matrixWorld.elements[14], 1e-12)
        for (eye in listOf(stereo.cameraL, stereo.cameraR)) {
            val focalPoint = Vector3(-2.0, 0.0, 0.0).project(eye)
            assertEquals(0.0, focalPoint.x, 1e-12)
            assertEquals(-2.0, focalPoint.unproject(eye).x, 1e-12)
        }

        parent.position.x = 4.0
        stereo.update(source)
        assertEquals(4.0, stereo.cameraL.matrixWorld.elements[12], 1e-12)
    }

    @Test
    fun frustumArrayUsesTheSubcamerasOfAnArrayCamera() {
        val first = PerspectiveCamera(near = 1.0, far = 10.0)
        val second = PerspectiveCamera(near = 1.0, far = 10.0)
        second.position.x = 10.0
        second.updateMatrixWorld()
        val firstView = ArrayCamera.View(first, ArrayCamera.Viewport(0, 0, 100, 100))
        val secondView = ArrayCamera.View(second, ArrayCamera.Viewport(100, 0, 100, 100))
        val camera = ArrayCamera(listOf(firstView, secondView))
        val frustums = FrustumArray().setFromArrayCamera(camera)
        val point = Vector3(10.0, 0.0, -5.0)

        assertTrue(frustums.containsPoint(point))
        camera.views = listOf(firstView)
        assertFalse(frustums.setFromArrayCamera(camera).containsPoint(point))
    }
}
