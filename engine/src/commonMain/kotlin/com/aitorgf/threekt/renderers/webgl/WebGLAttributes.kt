package com.aitorgf.threekt.renderers.webgl

import com.aitorgf.threekt.core.BufferAttribute
import com.aitorgf.threekt.core.Float32BufferAttribute
import com.aitorgf.threekt.core.Int16BufferAttribute
import com.aitorgf.threekt.core.Uint8BufferAttribute

/**
 * GPU buffers for [BufferAttribute]s, as Three.js `WebGLAttributes`. Each attribute is uploaded on first
 * use with its [BufferAttribute.usage]. When its [BufferAttribute.version] changes, the buffer contents
 * are replaced in place with `bufferSubData`; attribute arrays cannot change size. Buffers live until
 * [dispose].
 */
internal class WebGLAttributes(private val gl: GLContext) {
    private class Binding(val buffer: Any, var version: Int)

    private val bindings = mutableMapOf<BufferAttribute, Binding>()

    /** Binds the buffer of [attribute] to `ARRAY_BUFFER`, uploading its data when needed. */
    fun bind(attribute: BufferAttribute) {
        val binding = bindings[attribute]
        if (binding == null) {
            val buffer = gl.createBuffer()
            gl.bindBuffer(GL.ARRAY_BUFFER, buffer)
            createBuffer(attribute)
            bindings[attribute] = Binding(buffer, attribute.version)
            return
        }
        gl.bindBuffer(GL.ARRAY_BUFFER, binding.buffer)
        if (binding.version == attribute.version) return
        updateBuffer(attribute)
        binding.version = attribute.version
    }

    /** Returns the WebGL component type of [attribute]. */
    fun componentType(attribute: BufferAttribute): Int = when (attribute) {
        is Float32BufferAttribute -> GL.FLOAT
        is Uint8BufferAttribute -> GL.UNSIGNED_BYTE
        is Int16BufferAttribute -> GL.SHORT
        else -> error("Unsupported attribute type ${attribute::class.simpleName}.")
    }

    private fun createBuffer(attribute: BufferAttribute) {
        val usage = attribute.usage.glValue
        when (attribute) {
            is Float32BufferAttribute -> gl.bufferFloatData(GL.ARRAY_BUFFER, attribute.array, usage)
            is Uint8BufferAttribute -> gl.bufferByteData(GL.ARRAY_BUFFER, attribute.array, usage)
            is Int16BufferAttribute -> gl.bufferShortData(GL.ARRAY_BUFFER, attribute.array, usage)
            else -> error("Unsupported attribute type ${attribute::class.simpleName}.")
        }
    }

    private fun updateBuffer(attribute: BufferAttribute) {
        when (attribute) {
            is Float32BufferAttribute -> gl.bufferFloatSubData(GL.ARRAY_BUFFER, 0, attribute.array)
            is Uint8BufferAttribute -> gl.bufferByteSubData(GL.ARRAY_BUFFER, 0, attribute.array)
            is Int16BufferAttribute -> gl.bufferShortSubData(GL.ARRAY_BUFFER, 0, attribute.array)
            else -> error("Unsupported attribute type ${attribute::class.simpleName}.")
        }
    }

    /** Deletes every buffer. */
    fun dispose() {
        for (binding in bindings.values) gl.deleteBuffer(binding.buffer)
        bindings.clear()
    }
}
