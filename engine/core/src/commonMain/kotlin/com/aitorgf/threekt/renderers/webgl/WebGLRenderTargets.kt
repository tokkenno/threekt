package com.aitorgf.threekt.renderers.webgl

import com.aitorgf.threekt.core.DispatchedEvent
import com.aitorgf.threekt.renderers.RenderTargetType
import com.aitorgf.threekt.renderers.WebGLRenderTarget

/** Framebuffer and attachment storage shared by rendering, sampling and readback. */
internal class WebGLRenderTargets(private val gl: GLContext) {
    class Binding(
        val framebuffer: Any,
        val color: Any,
        val depthTexture: Any?,
        val depthBuffer: Any?,
        val onDispose: (DispatchedEvent) -> Unit,
    )

    private val bindings = mutableMapOf<WebGLRenderTarget, Binding>()

    fun get(target: WebGLRenderTarget): Binding = bindings.getOrPut(target) { create(target) }

    private fun create(target: WebGLRenderTarget): Binding {
        if (target.type == RenderTargetType.Float) {
            check(gl.hasExtension("EXT_color_buffer_float")) { "Float render targets require EXT_color_buffer_float." }
        }
        val previousFramebuffer = gl.getParameterHandle(GL.DRAW_FRAMEBUFFER_BINDING)
        val previousTexture = gl.getParameterHandle(GL.TEXTURE_BINDING_2D)
        val previousRenderbuffer = gl.getParameterHandle(GL.RENDERBUFFER_BINDING)
        val binding = Binding(
            gl.createFramebuffer(), gl.createTexture(),
            if (target.depthTexture != null) gl.createTexture() else null,
            if (target.depthBuffer && target.depthTexture == null) gl.createRenderbuffer() else null,
            { release(target) },
        )
        try {
            gl.bindFramebuffer(GL.DRAW_FRAMEBUFFER, binding.framebuffer)
            val format = if (target.type == RenderTargetType.Float) GL.RGBA32F else GL.RGBA8
            attach(target, binding.color, format, GL.COLOR_ATTACHMENT0)
            binding.depthTexture?.let { attach(target, it, GL.DEPTH_COMPONENT24, GL.DEPTH_ATTACHMENT) }
            binding.depthBuffer?.let {
                gl.bindRenderbuffer(GL.RENDERBUFFER, it)
                gl.renderbufferStorage(GL.RENDERBUFFER, GL.DEPTH_COMPONENT24, target.width, target.height)
                gl.framebufferRenderbuffer(GL.DRAW_FRAMEBUFFER, GL.DEPTH_ATTACHMENT, GL.RENDERBUFFER, it)
            }
            check(gl.checkFramebufferStatus(GL.DRAW_FRAMEBUFFER) == GL.FRAMEBUFFER_COMPLETE) {
                "The WebGL render target is incomplete."
            }
            target.addEventListener("dispose", binding.onDispose)
        } catch (error: Throwable) {
            delete(binding)
            throw error
        } finally {
            gl.bindFramebuffer(GL.DRAW_FRAMEBUFFER, previousFramebuffer)
            gl.bindTexture(GL.TEXTURE_2D, previousTexture)
            gl.bindRenderbuffer(GL.RENDERBUFFER, previousRenderbuffer)
        }
        return binding
    }

    private fun attach(target: WebGLRenderTarget, handle: Any, format: Int, point: Int) {
        gl.bindTexture(GL.TEXTURE_2D, handle)
        gl.texStorage2D(GL.TEXTURE_2D, 1, format, target.width, target.height)
        gl.texParameteri(GL.TEXTURE_2D, GL.TEXTURE_MAX_LEVEL, 0)
        gl.framebufferTexture2D(GL.DRAW_FRAMEBUFFER, point, GL.TEXTURE_2D, handle, 0)
    }

    private fun release(target: WebGLRenderTarget) {
        val binding = bindings.remove(target) ?: return
        target.removeEventListener("dispose", binding.onDispose)
        delete(binding)
    }

    private fun delete(binding: Binding) {
        gl.deleteFramebuffer(binding.framebuffer)
        gl.deleteTexture(binding.color)
        binding.depthTexture?.let(gl::deleteTexture)
        binding.depthBuffer?.let(gl::deleteRenderbuffer)
    }

    fun dispose() = bindings.keys.toList().forEach(::release)
}
