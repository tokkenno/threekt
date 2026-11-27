package com.aitorgf.threekt.renderers.webgl

import com.aitorgf.threekt.core.BufferAttribute
import com.aitorgf.threekt.core.Float16BufferAttribute
import com.aitorgf.threekt.core.Float32BufferAttribute
import com.aitorgf.threekt.core.Int16BufferAttribute
import com.aitorgf.threekt.core.Int32BufferAttribute
import com.aitorgf.threekt.core.Int8BufferAttribute
import com.aitorgf.threekt.core.InterleavedBufferAttribute
import com.aitorgf.threekt.core.Uint16BufferAttribute
import com.aitorgf.threekt.core.Uint32BufferAttribute
import com.aitorgf.threekt.core.Uint8BufferAttribute
import com.aitorgf.threekt.core.Uint8ClampedBufferAttribute

/**
 * GPU buffers for [BufferAttribute]s, as Three.js `WebGLAttributes`. [update] uploads an attribute on
 * first use with its [BufferAttribute.usage], and again when its [BufferAttribute.version] changes,
 * replacing the whole array or only its [BufferAttribute.updateRanges]. Interleaved attributes share the
 * buffer of their [com.aitorgf.threekt.core.InterleavedBuffer]. Arrays cannot change size after the first
 * upload. Buffers live until [remove] or [dispose].
 */
internal class WebGLAttributes(private val gl: GLContext) {
    /** A GPU buffer with the WebGL component [type] and element size of the uploaded array. */
    class Buffer(val buffer: Any, val type: Int, val bytesPerElement: Int, var version: Int, val size: Int)

    private val buffers = mutableMapOf<BufferAttribute, Buffer>()

    private fun key(attribute: BufferAttribute): BufferAttribute =
        (attribute as? InterleavedBufferAttribute)?.data?.storage ?: attribute

    /** Returns the buffer of [attribute], or null before its first [update]. */
    fun get(attribute: BufferAttribute): Buffer? = buffers[key(attribute)]

    /** Deletes the buffer of [attribute], if any. */
    fun remove(attribute: BufferAttribute) {
        buffers.remove(key(attribute))?.let { gl.deleteBuffer(it.buffer) }
    }

    /**
     * Creates or refreshes the buffer of [attribute] for [bufferType], `ARRAY_BUFFER` or
     * `ELEMENT_ARRAY_BUFFER`, and leaves it bound to that target.
     */
    fun update(attribute: BufferAttribute, bufferType: Int) {
        val source = key(attribute)
        val data = buffers[source]
        if (data == null) {
            buffers[source] = createBuffer(source, bufferType)
        } else {
            gl.bindBuffer(bufferType, data.buffer)
            if (data.version < source.version) {
                check(data.size == source.size * source.bytesPerElement) {
                    "The size of the buffer attribute's array does not match the original size. " +
                        "Resizing buffer attributes is not supported."
                }
                updateBuffer(source, bufferType)
                data.version = source.version
            }
        }
    }

    private fun createBuffer(attribute: BufferAttribute, bufferType: Int): Buffer {
        val buffer = gl.createBuffer()
        gl.bindBuffer(bufferType, buffer)
        val usage = attribute.usage.glValue
        when (attribute) {
            is Float32BufferAttribute -> gl.bufferFloatData(bufferType, attribute.array, usage)
            is Float16BufferAttribute -> gl.bufferShortData(bufferType, attribute.array, usage)
            is Int16BufferAttribute -> gl.bufferShortData(bufferType, attribute.array, usage)
            is Uint16BufferAttribute -> gl.bufferShortData(bufferType, attribute.array, usage)
            is Int32BufferAttribute -> gl.bufferIntData(bufferType, attribute.array, usage)
            is Uint32BufferAttribute -> gl.bufferIntData(bufferType, attribute.array, usage)
            is Int8BufferAttribute -> gl.bufferByteData(bufferType, attribute.array, usage)
            is Uint8BufferAttribute -> gl.bufferByteData(bufferType, attribute.array, usage)
            is Uint8ClampedBufferAttribute -> gl.bufferByteData(bufferType, attribute.array, usage)
            else -> error("Unsupported buffer data format: ${attribute::class.simpleName}.")
        }
        attribute.onUploadCallback()
        return Buffer(buffer, componentType(attribute), attribute.bytesPerElement, attribute.version, attribute.size * attribute.bytesPerElement)
    }

    private fun updateBuffer(attribute: BufferAttribute, bufferType: Int) {
        val ranges = attribute.updateRanges.sortedBy { it.start }.toMutableList()
        if (ranges.isEmpty()) {
            upload(attribute, bufferType, 0, attribute.size)
        } else {
            // Merge overlapping or adjacent ranges, as Three.js does, before uploading them.
            var mergeIndex = 0
            for (i in 1 until ranges.size) {
                val previous = ranges[mergeIndex]
                val range = ranges[i]
                if (range.start <= previous.start + previous.count + 1) {
                    previous.count = maxOf(previous.count, range.start + range.count - previous.start)
                } else {
                    mergeIndex++
                    ranges[mergeIndex] = range
                }
            }
            for (i in 0..mergeIndex) upload(attribute, bufferType, ranges[i].start, ranges[i].count)
            attribute.clearUpdateRanges()
        }
        attribute.onUploadCallback()
    }

    private fun upload(attribute: BufferAttribute, bufferType: Int, start: Int, count: Int) {
        val offset = start * attribute.bytesPerElement
        when (attribute) {
            is Float32BufferAttribute -> gl.bufferFloatSubData(bufferType, offset, attribute.array, start, count)
            is Float16BufferAttribute -> gl.bufferShortSubData(bufferType, offset, attribute.array, start, count)
            is Int16BufferAttribute -> gl.bufferShortSubData(bufferType, offset, attribute.array, start, count)
            is Uint16BufferAttribute -> gl.bufferShortSubData(bufferType, offset, attribute.array, start, count)
            is Int32BufferAttribute -> gl.bufferIntSubData(bufferType, offset, attribute.array, start, count)
            is Uint32BufferAttribute -> gl.bufferIntSubData(bufferType, offset, attribute.array, start, count)
            is Int8BufferAttribute -> gl.bufferByteSubData(bufferType, offset, attribute.array, start, count)
            is Uint8BufferAttribute -> gl.bufferByteSubData(bufferType, offset, attribute.array, start, count)
            is Uint8ClampedBufferAttribute -> gl.bufferByteSubData(bufferType, offset, attribute.array, start, count)
            else -> error("Unsupported buffer data format: ${attribute::class.simpleName}.")
        }
    }

    /** Returns the WebGL component type of the array of [attribute]. */
    private fun componentType(attribute: BufferAttribute): Int = when (attribute) {
        is Float32BufferAttribute -> GL.FLOAT
        is Float16BufferAttribute -> GL.HALF_FLOAT
        is Uint16BufferAttribute -> GL.UNSIGNED_SHORT
        is Int16BufferAttribute -> GL.SHORT
        is Uint32BufferAttribute -> GL.UNSIGNED_INT
        is Int32BufferAttribute -> GL.INT
        is Int8BufferAttribute -> GL.BYTE
        is Uint8BufferAttribute, is Uint8ClampedBufferAttribute -> GL.UNSIGNED_BYTE
        else -> error("Unsupported buffer data format: ${attribute::class.simpleName}.")
    }

    /** Deletes every buffer. */
    fun dispose() {
        for (buffer in buffers.values) gl.deleteBuffer(buffer.buffer)
        buffers.clear()
    }
}
