package com.aitorgf.threekt.cameras

import com.aitorgf.threekt.math.Matrix4

/** Builds an off-axis perspective view for each eye from a source perspective camera. */
class StereoCamera {
    /** Multiplier applied to the source camera's aspect ratio for each eye. */
    var aspect = 1.0

    /** Distance between the two eyes in camera units. */
    var eyeSep = 0.064

    /** The perspective view for the left eye. */
    val cameraL = PerspectiveCamera().apply { matrixAutoUpdate = false }

    /** The perspective view for the right eye. */
    val cameraR = PerspectiveCamera().apply { matrixAutoUpdate = false }

    private val eyeOffset = Matrix4()

    /** Updates both eye poses and projections from [camera] and its current projection. */
    fun update(camera: PerspectiveCamera) {
        require(camera !== cameraL && camera !== cameraR) { "An eye camera cannot be the stereo source." }
        val eyeAspect = camera.aspect * aspect
        require(camera.focus > 0.0 && eyeAspect > 0.0 && eyeSep >= 0.0) {
            "Stereo focus and aspect must be positive, and eye separation must be nonnegative."
        }
        camera.updateWorldMatrix(updateParents = true, updateChildren = false)

        val halfWidth = camera.near * camera.tanVerticalHalfFov() * eyeAspect / camera.zoom
        val halfSeparation = eyeSep * 0.5
        updateEye(cameraL, camera, -halfSeparation, halfWidth, eyeAspect)
        updateEye(cameraR, camera, halfSeparation, halfWidth, eyeAspect)
    }

    private fun updateEye(
        eye: PerspectiveCamera, source: PerspectiveCamera, offset: Double, halfWidth: Double, eyeAspect: Double,
    ) {
        eye.coordinateSystem = source.coordinateSystem
        eye.reversedDepth = source.reversedDepth
        eye.fov = source.fov
        eye.aspect = eyeAspect
        eye.near = source.near
        eye.far = source.far
        eye.focus = source.focus
        eye.zoom = source.zoom

        eye.projectionMatrix.copy(source.projectionMatrix)
        eye.projectionMatrix.elements[0] = source.near / halfWidth
        eye.projectionMatrix.elements[8] = -offset * source.near / (source.focus * halfWidth)
        eye.projectionMatrixInverse.copy(eye.projectionMatrix).invert()

        eye.matrix.multiplyMatrices(source.matrixWorld, eyeOffset.makeTranslation(offset, 0.0, 0.0))
        eye.parent?.let { parent ->
            parent.updateWorldMatrix(updateParents = true, updateChildren = false)
            eye.matrix.premultiply(eyeOffset.copy(parent.matrixWorld).invert())
        }
        eye.updateMatrixWorld()
    }
}
