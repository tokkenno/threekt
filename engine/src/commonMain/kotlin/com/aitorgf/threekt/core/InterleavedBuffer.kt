package com.aitorgf.threekt.core

import kotlin.js.ExperimentalJsStatic
import kotlin.js.JsExport
import kotlin.js.JsStatic

/**
 * Typed vertex storage shared by interleaved attribute views. [stride] is measured in elements.
 * Kotlin uses typed factories in place of the upstream typed-array constructor.
 */
@JsExport
class InterleavedBuffer private constructor(internal val storage: BufferAttribute, val stride: Int) {
    val count: Int get() = storage.count

    /** Changes the GPU usage hint before the first draw. */
    fun setUsage(value: Usage): InterleavedBuffer {
        storage.setUsage(value)
        return this
    }

    /** Marks the shared array for upload before the next draw. */
    fun markUpdated() = storage.markUpdated()

    @OptIn(ExperimentalJsStatic::class)
    companion object {
        @JsStatic
        fun fromFloat32(array: FloatArray, stride: Int) =
            InterleavedBuffer(Float32BufferAttribute(array, stride), stride)

        /** Retains the unsigned bytes in Kotlin's signed byte array. */
        @JsStatic
        fun fromUint8(array: ByteArray, stride: Int) =
            InterleavedBuffer(Uint8BufferAttribute(array, stride), stride)
    }
}
