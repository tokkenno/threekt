@file:OptIn(ThreektRendererApi::class)

package com.aitorgf.threekt.core

import com.aitorgf.threekt.ThreektRendererApi
import com.aitorgf.threekt.math.MathUtils
import kotlin.js.JsExport
import kotlin.js.JsName

/**
 * Holds the typed array shared by one or more [InterleavedBufferAttribute]s. [stride] is expressed in
 * array elements.
 *
 * Deviations from Three.js:
 * - Kotlin uses one constructor per array type in place of the generic typed array parameter, exported
 *   to JavaScript as named factories such as `InterleavedBuffer.fromFloat32Array`. Array types that share a
 *   Kotlin array with another, such as unsigned 16-bit integers, are created with the companion
 *   functions [fromUint16Array], [fromUint32Array], [fromInt8Array] and [fromFloat16Array].
 * - The array is not exposed as `array`; keep a reference to the array passed to the constructor and
 *   call [markUpdated] after changing it.
 * - [clone] always copies the array; Three.js can share it between clones through its `data` argument.
 * - `toJSON()` is not available yet.
 */
@JsExport
open class InterleavedBuffer internal constructor(storage: BufferAttribute, stride: Int) {
    /** Retains a 32-bit float array. */
    @JsName("fromFloat32Array")
    constructor(array: FloatArray, stride: Int) : this(Float32BufferAttribute(array, 1), stride)

    /** Retains an unsigned byte array, represented by an `Int8Array` in JavaScript. */
    @JsName("fromUint8Array")
    constructor(array: ByteArray, stride: Int) : this(Uint8BufferAttribute(array, 1), stride)

    /** Retains a signed 16-bit array. */
    @JsName("fromInt16Array")
    constructor(array: ShortArray, stride: Int) : this(Int16BufferAttribute(array, 1), stride)

    /** Retains a signed 32-bit array. */
    @JsName("fromInt32Array")
    constructor(array: IntArray, stride: Int) : this(Int32BufferAttribute(array, 1), stride)

    /**
     * The typed array, viewed as a one-component attribute. Renderers upload and track this attribute in
     * place of the buffer.
     */
    @ThreektRendererApi
    var storage: BufferAttribute = storage
        private set

    /** Number of array elements from the start of one vertex to the start of the next. */
    var stride: Int = stride
        private set

    init {
        require(stride > 0 && storage.size % stride == 0) { "The buffer must contain a whole number of strides." }
    }

    /** Random UUID of this buffer. */
    var uuid: String = MathUtils.generateUUID()

    /** Number of complete strides, that is, of vertices. */
    val count: Int get() = storage.size / stride

    /** Expected update pattern. Change it before the first draw. */
    val usage: Usage get() = storage.usage

    /** Incremented by [markUpdated]. */
    val version: Int get() = storage.version

    /** Sets the usage hint and returns this buffer. */
    fun setUsage(value: Usage): InterleavedBuffer {
        storage.setUsage(value)
        return this
    }

    /** Marks the shared array for upload, replacing the Three.js `needsUpdate` setter. */
    fun markUpdated() = storage.markUpdated()

    /** Ranges of array elements to upload on the next update, as [BufferAttribute.updateRanges]. */
    val updateRanges: Array<BufferAttribute.UpdateRange> get() = storage.updateRanges

    /** Adds a range of [count] array elements from element [start] to upload on the next update. */
    fun addUpdateRange(start: Int, count: Int) = storage.addUpdateRange(start, count)

    /** Clears [updateRanges], so the next update uploads the whole array. */
    fun clearUpdateRanges() = storage.clearUpdateRanges()

    /** Sets the callback that the renderer calls after each upload. Returns this buffer. */
    fun onUpload(callback: () -> Unit): InterleavedBuffer {
        storage.onUpload(callback)
        return this
    }

    /** Size in bytes of one array element. */
    internal val bytesPerElement: Int get() = storage.bytesPerElement

    /**
     * Copies the raw elements of vertex [index2] of [interleavedBuffer] into vertex [index1] of this
     * buffer. Returns this buffer.
     */
    fun copyAt(index1: Int, interleavedBuffer: InterleavedBuffer, index2: Int): InterleavedBuffer {
        val offset1 = index1 * stride
        val offset2 = index2 * interleavedBuffer.stride
        for (i in 0 until stride) storage.writeRaw(offset1 + i, interleavedBuffer.storage.readRaw(offset2 + i))
        return this
    }

    /**
     * Replaces the array with a copy of the array of [source], which must have the same array type, and
     * copies its [stride] and [usage]. Returns this buffer.
     */
    open fun copy(source: InterleavedBuffer): InterleavedBuffer {
        storage = storage.copy(source.storage)
        stride = source.stride
        return this
    }

    /** Returns a buffer with a copy of the array and the same [stride] and [usage]. */
    open fun clone(): InterleavedBuffer = InterleavedBuffer(storage.clone(), stride).also { it.setUsage(usage) }

    companion object {
        /** Returns a buffer that retains an unsigned 16-bit array, represented by an `Int16Array` in JavaScript. */
        fun fromUint16Array(array: ShortArray, stride: Int): InterleavedBuffer =
            InterleavedBuffer(Uint16BufferAttribute(array, 1), stride)

        /** Returns a buffer that retains an unsigned 32-bit array, represented by an `Int32Array` in JavaScript. */
        fun fromUint32Array(array: IntArray, stride: Int): InterleavedBuffer =
            InterleavedBuffer(Uint32BufferAttribute(array, 1), stride)

        /** Returns a buffer that retains a signed byte array. */
        fun fromInt8Array(array: ByteArray, stride: Int): InterleavedBuffer =
            InterleavedBuffer(Int8BufferAttribute(array, 1), stride)

        /** Returns a buffer that retains half floats, the bits produced by `DataUtils.toHalfFloat`. */
        fun fromFloat16Array(array: ShortArray, stride: Int): InterleavedBuffer =
            InterleavedBuffer(Float16BufferAttribute(array, 1), stride)
    }
}
