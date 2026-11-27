package com.aitorgf.threekt.cameras

import com.aitorgf.threekt.core.Object3D
import kotlin.js.JsExport

/** A collection of perspective views of one scene, each rendered into its own viewport. */
@JsExport
class ArrayCamera @JsExport.Ignore constructor(
    @JsExport.Ignore var views: List<View>,
) : PerspectiveCamera() {
    @kotlin.js.JsName("create")
    constructor() : this(emptyList())

    /** Replaces the camera and viewport pairs using arrays when called from JavaScript. */
    fun setViews(cameras: Array<PerspectiveCamera>, viewports: Array<Viewport>) {
        require(cameras.size == viewports.size)
        views = cameras.indices.map { View(cameras[it], viewports[it]) }
    }

    /** A subcamera and its destination viewport. */
    data class View(val camera: PerspectiveCamera, val viewport: Viewport)

    /** Rectangle in drawing-buffer pixels, with its origin at the lower left. */
    data class Viewport(val x: Int, val y: Int, val width: Int, val height: Int) {
        init {
            require(width > 0 && height > 0) { "A viewport must have positive dimensions." }
        }
    }

    /** Shares the [views] of [source] when it is an array camera. Returns this camera. */
    override fun copy(source: Object3D, recursive: Boolean): ArrayCamera {
        super.copy(source, recursive)
        if (source is ArrayCamera) views = source.views
        return this
    }

    override fun clone(recursive: Boolean): ArrayCamera = ArrayCamera().copy(this, recursive)
}
