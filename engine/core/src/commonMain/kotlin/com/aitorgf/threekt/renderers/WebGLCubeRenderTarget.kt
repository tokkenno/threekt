package com.aitorgf.threekt.renderers

import com.aitorgf.threekt.cameras.CubeCamera
import com.aitorgf.threekt.renderers.webgl.GL
import com.aitorgf.threekt.renderers.webgl.GLContext

/** Six RGBA8 cube-map faces with a shared depth attachment for WebGL2 capture. */
internal class WebGLCubeRenderTarget(val gl: GLContext, val size: Int) {
    val texture: Any
    private val framebuffer: Any
    private val depthBuffer: Any
    private var disposed = false

    init {
        require(size in 1..gl.getParameterInt(GL.MAX_CUBE_MAP_TEXTURE_SIZE)) { "Cube-map size exceeds WebGL limits." }
        val previousTexture = gl.getParameterHandle(GL.TEXTURE_BINDING_CUBE_MAP)
        val previousRenderbuffer = gl.getParameterHandle(GL.RENDERBUFFER_BINDING)
        texture = gl.createTexture()
        framebuffer = gl.createFramebuffer()
        depthBuffer = gl.createRenderbuffer()
        try {
            gl.bindTexture(GL.TEXTURE_CUBE_MAP, texture)
            gl.texStorage2D(GL.TEXTURE_CUBE_MAP, 1, GL.RGBA8, size, size)
            gl.texParameteri(GL.TEXTURE_CUBE_MAP, GL.TEXTURE_MIN_FILTER, GL.LINEAR)
            gl.texParameteri(GL.TEXTURE_CUBE_MAP, GL.TEXTURE_MAG_FILTER, GL.LINEAR)
            gl.texParameteri(GL.TEXTURE_CUBE_MAP, GL.TEXTURE_WRAP_S, GL.CLAMP_TO_EDGE)
            gl.texParameteri(GL.TEXTURE_CUBE_MAP, GL.TEXTURE_WRAP_T, GL.CLAMP_TO_EDGE)
            gl.texParameteri(GL.TEXTURE_CUBE_MAP, GL.TEXTURE_WRAP_R, GL.CLAMP_TO_EDGE)
            gl.bindRenderbuffer(GL.RENDERBUFFER, depthBuffer)
            gl.renderbufferStorage(GL.RENDERBUFFER, GL.DEPTH_COMPONENT24, size, size)
        } catch (error: Throwable) {
            dispose()
            throw error
        } finally {
            gl.bindTexture(GL.TEXTURE_CUBE_MAP, previousTexture)
            gl.bindRenderbuffer(GL.RENDERBUFFER, previousRenderbuffer)
        }
    }

    /** Binds the requested face and depth storage to the drawing framebuffer. */
    fun bindFace(face: CubeCamera.Face) {
        check(!disposed) { "The WebGL cube render target has been disposed." }
        gl.bindFramebuffer(GL.DRAW_FRAMEBUFFER, framebuffer)
        gl.framebufferTexture2D(GL.DRAW_FRAMEBUFFER, GL.COLOR_ATTACHMENT0, GL.TEXTURE_CUBE_MAP_POSITIVE_X + face.ordinal, texture, 0)
        gl.framebufferRenderbuffer(GL.DRAW_FRAMEBUFFER, GL.DEPTH_ATTACHMENT, GL.RENDERBUFFER, depthBuffer)
        check(gl.checkFramebufferStatus(GL.DRAW_FRAMEBUFFER) == GL.FRAMEBUFFER_COMPLETE) {
            "The WebGL cube render target is incomplete."
        }
    }

    /** Releases the cube texture, framebuffer, and depth storage. */
    fun dispose() {
        if (disposed) return
        disposed = true
        gl.deleteTexture(texture)
        gl.deleteFramebuffer(framebuffer)
        gl.deleteRenderbuffer(depthBuffer)
    }
}
