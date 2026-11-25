package com.aitorgf.threekt.cameras

import com.aitorgf.threekt.core.Object3D
import com.aitorgf.threekt.math.Vector3
import com.aitorgf.threekt.renderers.CoordinateSystem
import kotlin.test.Test
import kotlin.test.assertEquals

class CubeCameraTest {
    @Test
    fun cubeUpdatesClipPlanesAndInversesAfterProjectionSettingsChange() {
        val cube = CubeCamera()
        cube.update { _, _ -> }
        cube.near = 2.0
        cube.far = 10.0
        cube.reversedDepth = true

        cube.update { face, camera ->
            val direction = camera.getWorldDirection(Vector3())
            val near = direction.copy().multiplyScalar(2.0).project(camera)
            val far = direction.copy().multiplyScalar(10.0).project(camera)
            assertEquals(1.0, near.z, 1e-12, face.name)
            assertEquals(0.0, far.z, 1e-12, face.name)
            assertEquals(0.0, far.unproject(camera).distanceTo(direction.multiplyScalar(10.0)), 1e-12)
        }
    }

    @Test
    fun verticalFacesUseOppositeUpDirectionsInWebGlAndWebGpu() {
        val cube = CubeCamera()
        for (system in CoordinateSystem.entries) {
            cube.coordinateSystem = system
            cube.update { _, _ -> }
            val sign = if (system == CoordinateSystem.WebGL) 1.0 else -1.0
            val upOnPositiveY = Vector3(0.0, 1.0, -sign).project(cube.getCamera(CubeCamera.Face.PositiveY))
            val upOnNegativeY = Vector3(0.0, -1.0, sign).project(cube.getCamera(CubeCamera.Face.NegativeY))
            assertEquals(1.0, upOnPositiveY.y, 1e-12)
            assertEquals(1.0, upOnNegativeY.y, 1e-12)
        }
    }

    @Test
    fun capturesSixFacesWithTheSelectedCoordinateConvention() {
        val root = Object3D()
        root.position.set(2.0, 3.0, 4.0)
        val cube = CubeCamera(near = 1.0, far = 5.0)
        root.add(cube)

        for (system in CoordinateSystem.entries) {
            cube.coordinateSystem = system
            val captured = mutableListOf<CubeCamera.Face>()
            cube.update { face, camera ->
                captured.add(face)
                val direction = camera.getWorldDirection(Vector3())
                val expectedDirection = Vector3()
                expectedDirection[face.axis] =
                    if (system == CoordinateSystem.WebGPU && face.axis == 0) -face.sign else face.sign
                assertEquals(0.0, direction.distanceTo(expectedDirection), 1e-12)
                assertEquals(system, camera.coordinateSystem)
                assertEquals(root.position, Vector3().setFromMatrixPosition(camera.matrixWorld))

                val nearCenter = root.position.copy().add(expectedDirection).project(camera)
                assertEquals(0.0, nearCenter.x, 1e-12)
                assertEquals(0.0, nearCenter.y, 1e-12)
                assertEquals(if (system == CoordinateSystem.WebGL) -1.0 else 0.0, nearCenter.z, 1e-12)
            }
            assertEquals(CubeCamera.Face.entries.toList(), captured)
        }
    }
}
