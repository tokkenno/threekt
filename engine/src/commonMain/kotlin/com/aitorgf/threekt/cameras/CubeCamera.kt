package com.aitorgf.threekt.cameras

import com.aitorgf.threekt.core.Object3D
import com.aitorgf.threekt.math.Matrix4
import com.aitorgf.threekt.math.Vector3
import com.aitorgf.threekt.renderers.CoordinateSystem

/** Captures the six 90-degree views of a cube map from one world-space position. */
class CubeCamera(var near: Double = 0.1, var far: Double = 2000.0) : Object3D() {
    /** Clip-space and face orientation convention for the renderer receiving the views. */
    var coordinateSystem = CoordinateSystem.WebGL

    /** Whether the face projections use reversed depth. */
    var reversedDepth = false

    /** Cube-map attachment order used by renderers. */
    enum class Face(internal val axis: Int, internal val sign: Double) {
        PositiveX(0, 1.0), NegativeX(0, -1.0),
        PositiveY(1, 1.0), NegativeY(1, -1.0),
        PositiveZ(2, 1.0), NegativeZ(2, -1.0),
    }

    private val faceCameras = Face.entries.associateWith {
        PerspectiveCamera(fov = 90.0, near = near, far = far).also { add(it) }
    }
    private var orientedFor = coordinateSystem

    init {
        orientFaces()
    }

    /** Returns the perspective camera that captures [face]. */
    fun getCamera(face: Face): PerspectiveCamera = faceCameras.getValue(face)

    /** Updates the face cameras and passes each face and its camera to [renderFace] in cube-map order. */
    fun update(renderFace: (Face, PerspectiveCamera) -> Unit) {
        if (coordinateSystem != orientedFor) orientFaces()
        for (camera in faceCameras.values) {
            if (camera.near != near || camera.far != far ||
                camera.coordinateSystem != coordinateSystem || camera.reversedDepth != reversedDepth
            ) {
                camera.near = near
                camera.far = far
                camera.coordinateSystem = coordinateSystem
                camera.reversedDepth = reversedDepth
                camera.updateProjectionMatrix()
            }
        }
        updateWorldMatrix(updateParents = true, updateChildren = true)
        for ((face, camera) in faceCameras) renderFace(face, camera)
    }

    private fun orientFaces() {
        val webGPU = coordinateSystem == CoordinateSystem.WebGPU
        val vertical = if (webGPU) -1.0 else 1.0
        val direction = Vector3()
        val up = Vector3()
        val origin = Vector3()
        val rotation = Matrix4()
        for ((face, camera) in faceCameras) {
            direction.setScalar(0.0)
            direction[face.axis] = if (webGPU && face.axis == 0) -face.sign else face.sign
            if (face.axis == 1) up.set(0.0, 0.0, -face.sign * vertical) else up.set(0.0, vertical, 0.0)
            camera.quaternion.setFromRotationMatrix(rotation.lookAt(origin, direction, up))
        }
        orientedFor = coordinateSystem
    }
}
