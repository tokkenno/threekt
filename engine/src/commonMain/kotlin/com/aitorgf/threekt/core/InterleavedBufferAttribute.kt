package com.aitorgf.threekt.core

import kotlin.js.JsExport

/**
 * Views [itemSize] consecutive components at [offset] within each stride of [data], without copying.
 * Extends [BufferAttribute] in Kotlin so both attribute types can be passed to [BufferGeometry].
 * Unlike Three.js, the typed array is accessed through the original array passed to [data].
 */
@JsExport
class InterleavedBufferAttribute(
    val data: InterleavedBuffer,
    itemSize: Int,
    /** Offset in array elements from the start of each stride. */
    val offset: Int,
    normalized: Boolean = false,
) : BufferAttribute(itemSize, normalized) {
    init {
        require(offset >= 0 && offset <= data.stride - itemSize) { "The attribute must fit within one stride." }
    }

    override val size: Int get() = data.count * itemSize
    override val version: Int get() = data.version
    override val usage: Usage get() = data.usage

    /** Sets the usage hint for the shared buffer and returns this attribute. */
    override fun setUsage(value: Usage): InterleavedBufferAttribute {
        data.setUsage(value)
        return this
    }

    /** Marks the entire shared buffer for upload. */
    override fun markUpdated() = data.markUpdated()

    override val bytesPerElement: Int get() = data.bytesPerElement

    override val normalizationScale: Double? get() = data.storage.normalizationScale

    private fun storageOffset(offset: Int): Int = (offset / itemSize) * data.stride + this.offset + offset % itemSize

    override fun readRaw(offset: Int): Double = data.storage.readRaw(storageOffset(offset))

    override fun writeRaw(offset: Int, value: Double) = data.storage.writeRaw(storageOffset(offset), value)

    /**
     * Returns the values as a separate, non-interleaved [Float32BufferAttribute], as Three.js does when
     * cloning without the shared buffer.
     */
    override fun clone(): BufferAttribute {
        val values = FloatArray(count * itemSize) { read(it).toFloat() }
        return Float32BufferAttribute(values, itemSize, false).also { copy ->
            copy.name = name
            copy.setUsage(usage)
            copy.gpuType = gpuType
        }
    }
}
