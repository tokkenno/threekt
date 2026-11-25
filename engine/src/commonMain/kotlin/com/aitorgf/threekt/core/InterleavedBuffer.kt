package com.aitorgf.threekt.core

import kotlin.js.JsExport
import kotlin.js.JsName

/**
 * Holds the typed array shared by one or more vertex attributes. [stride] is expressed in array
 * elements. Kotlin uses typed constructors in place of Three.js's generic typed array parameter.
 * Each typed constructor has a named JS export.
 */
@JsExport
open class InterleavedBuffer private constructor(internal val storage: BufferAttribute, val stride: Int) {
    @JsName("fromFloat32Array")
    constructor(array: FloatArray, stride: Int) : this(Float32BufferAttribute(array, 1), stride)

    /** Retains an unsigned byte array, represented by an Int8Array on JavaScript. */
    @JsName("fromUint8Array")
    constructor(array: ByteArray, stride: Int) : this(Uint8BufferAttribute(array, 1), stride)

    /** Retains a signed 16-bit array. */
    @JsName("fromInt16Array")
    constructor(array: ShortArray, stride: Int) : this(Int16BufferAttribute(array, 1), stride)

    init {
        require(stride > 0 && storage.size % stride == 0) { "The buffer must contain a whole number of strides." }
    }

    /** Number of complete strides. */
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

    internal val bytesPerElement: Int
        get() = when (storage) {
            is Float32BufferAttribute -> 4
            is Int16BufferAttribute -> 2
            else -> 1
        }

    internal fun write(index: Int, value: Double, normalized: Boolean) {
        val stored = if (!normalized) value else when (storage) {
            is Uint8BufferAttribute -> value * 255.0
            is Int16BufferAttribute -> value * 32767.0
            else -> value
        }
        storage.setX(index, stored)
    }

    internal fun read(index: Int, normalized: Boolean): Double {
        val value = storage.getX(index)
        if (!normalized) return value
        return when (storage) {
            is Uint8BufferAttribute -> value / 255.0
            is Int16BufferAttribute -> (value / 32767.0).coerceAtLeast(-1.0)
            else -> value
        }
    }
}
