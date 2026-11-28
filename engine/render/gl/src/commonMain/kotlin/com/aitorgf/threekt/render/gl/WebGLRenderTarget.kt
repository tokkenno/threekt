package com.aitorgf.threekt.render.gl

import com.aitorgf.threekt.core.EventDispatcher
import com.aitorgf.threekt.textures.Texture
import kotlin.js.JsExport

/** A 2D RGBA render target. Threekt declares optional sampleable depth when constructing the target. */
@JsExport
class WebGLRenderTarget(
    width: Int = 1,
    height: Int = 1,
    val type: RenderTargetType = RenderTargetType.UnsignedByte,
    val depthBuffer: Boolean = true,
    sampleDepth: Boolean = false,
) : EventDispatcher() {
    var width: Int = width
        private set
    var height: Int = height
        private set
    val texture: Texture = RenderTargetTexture(this, false)
    val depthTexture: Texture? = if (sampleDepth) RenderTargetTexture(this, true) else null

    init {
        require(width > 0 && height > 0 && (!sampleDepth || depthBuffer))
    }

    /** Resizes the attachments on next use. Previous contents are discarded. */
    fun setSize(width: Int, height: Int) {
        require(width > 0 && height > 0)
        if (width == this.width && height == this.height) return
        this.width = width
        this.height = height
        dispose()
    }

    /** Releases cached GPU attachments. Subsequent rendering allocates fresh storage. */
    fun dispose() = dispatchEvent("dispose")
}
