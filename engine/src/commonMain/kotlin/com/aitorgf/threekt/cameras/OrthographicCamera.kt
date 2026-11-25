package com.aitorgf.threekt.cameras

/** Camera that projects objects at a constant apparent size regardless of their distance. */
class OrthographicCamera(
    var left: Double = -1.0,
    var right: Double = 1.0,
    var top: Double = 1.0,
    var bottom: Double = -1.0,
    var near: Double = 0.1,
    var far: Double = 2000.0,
) : Camera() {
    init {
        updateProjectionMatrix()
    }

    /** Rebuilds the orthographic projection and its inverse from the current bounds and zoom. */
    override fun updateProjectionMatrix() {
        val width = (right - left) / zoom
        val height = (top - bottom) / zoom
        val centerX = (right + left) * 0.5
        val centerY = (top + bottom) * 0.5
        var viewLeft = centerX - width * 0.5
        var viewRight = centerX + width * 0.5
        var viewTop = centerY + height * 0.5
        var viewBottom = centerY - height * 0.5

        viewOffset?.let { view ->
            val scaleX = width / view.fullWidth
            val scaleY = height / view.fullHeight
            viewLeft += view.x * scaleX
            viewRight = viewLeft + view.width * scaleX
            viewTop -= view.y * scaleY
            viewBottom = viewTop - view.height * scaleY
        }
        projectionMatrix.makeOrthographic(viewLeft, viewRight, viewTop, viewBottom, near, far, coordinateSystem, reversedDepth)
        projectionMatrixInverse.copy(projectionMatrix).invert()
    }
}
