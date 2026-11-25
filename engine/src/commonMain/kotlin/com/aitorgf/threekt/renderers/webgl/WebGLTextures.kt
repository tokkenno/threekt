package com.aitorgf.threekt.renderers.webgl

import com.aitorgf.threekt.textures.Texture
import com.aitorgf.threekt.textures.Wrapping

/**
 * GPU textures for [Texture]s with an [Texture.image]. Pixels are uploaded on first use and again after
 * [Texture.markUpdated]; wrapping and anisotropy are applied whenever they change.
 */
internal class WebGLTextures(private val gl: GLContext, private val capabilities: WebGLCapabilities) {
    private class Binding(val handle: Any, var version: Int) {
        var wrapS: Wrapping? = null
        var wrapT: Wrapping? = null
        var anisotropy = 1
    }

    private val bindings = mutableMapOf<Texture, Binding>()

    /** Binds [texture] to texture [unit], uploading pixels and sampling settings when needed. */
    fun bind(texture: Texture, unit: Int) {
        gl.activeTexture(GL.TEXTURE0 + unit)
        val binding = bindings[texture] ?: create(texture)
        gl.bindTexture(GL.TEXTURE_2D, binding.handle)
        if (binding.version != texture.version) {
            upload(texture)
            binding.version = texture.version
        }
        applySampling(texture, binding)
    }

    private fun create(texture: Texture): Binding {
        val handle = gl.createTexture()
        try {
            gl.bindTexture(GL.TEXTURE_2D, handle)
            gl.texParameteri(GL.TEXTURE_2D, GL.TEXTURE_MIN_FILTER, GL.LINEAR_MIPMAP_LINEAR)
            gl.texParameteri(GL.TEXTURE_2D, GL.TEXTURE_MAG_FILTER, GL.LINEAR)
            upload(texture)
        } catch (error: Throwable) {
            gl.deleteTexture(handle)
            throw error
        }
        return Binding(handle, texture.version).also { bindings[texture] = it }
    }

    private fun applySampling(texture: Texture, binding: Binding) {
        if (binding.wrapS != texture.wrapS) {
            gl.texParameteri(GL.TEXTURE_2D, GL.TEXTURE_WRAP_S, glWrapping(texture.wrapS))
            binding.wrapS = texture.wrapS
        }
        if (binding.wrapT != texture.wrapT) {
            gl.texParameteri(GL.TEXTURE_2D, GL.TEXTURE_WRAP_T, glWrapping(texture.wrapT))
            binding.wrapT = texture.wrapT
        }
        if (!capabilities.anisotropicFiltering) return
        val anisotropy = texture.anisotropy.coerceIn(1, capabilities.getMaxAnisotropy())
        if (binding.anisotropy != anisotropy) {
            gl.texParameterf(GL.TEXTURE_2D, GL.TEXTURE_MAX_ANISOTROPY_EXT, anisotropy.toDouble())
            binding.anisotropy = anisotropy
        }
    }

    private fun glWrapping(wrapping: Wrapping): Int = when (wrapping) {
        Wrapping.Repeat -> GL.REPEAT
        Wrapping.ClampToEdge -> GL.CLAMP_TO_EDGE
        Wrapping.MirroredRepeat -> GL.MIRRORED_REPEAT
    }

    private fun upload(texture: Texture) {
        val image = requireNotNull(texture.image) { "The texture has no image to upload." }
        gl.texImage2D(GL.TEXTURE_2D, 0, GL.SRGB8_ALPHA8, GL.RGBA, GL.UNSIGNED_BYTE, image, flipY = true)
        gl.generateMipmap(GL.TEXTURE_2D)
    }

    /** Deletes every texture. */
    fun dispose() {
        for (binding in bindings.values) gl.deleteTexture(binding.handle)
        bindings.clear()
    }
}
