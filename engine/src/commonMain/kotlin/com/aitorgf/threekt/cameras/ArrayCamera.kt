package com.aitorgf.threekt.cameras

/** A collection of perspective views of one scene, each rendered into its own viewport. */
class ArrayCamera(var views: List<View> = emptyList()) : PerspectiveCamera() {
    /** A subcamera and its destination viewport. */
    data class View(val camera: PerspectiveCamera, val viewport: Viewport)

    /** Rectangle in drawing-buffer pixels, with its origin at the lower left. */
    data class Viewport(val x: Int, val y: Int, val width: Int, val height: Int) {
        init {
            require(width > 0 && height > 0) { "A viewport must have positive dimensions." }
        }
    }
}
