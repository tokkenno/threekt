package com.aitorgf.threekt.core

import kotlin.js.JsExport

/** Attribute view into [data], starting [offset] elements into each item. */
@JsExport
class InterleavedBufferAttribute(
    val data: InterleavedBuffer,
    itemSize: Int,
    val offset: Int,
    normalized: Boolean = false,
) : BufferAttribute(itemSize, normalized) {
    init {
        require(offset >= 0 && itemSize <= data.stride - offset)
    }

    override val usage: Usage get() = data.storage.usage

    /** Changes the hint for the shared data. */
    override fun setUsage(value: Usage): BufferAttribute {
        data.setUsage(value)
        return this
    }

    override val size: Int get() = data.count * itemSize
    override val version: Int get() = data.storage.version

    /** Marks the shared data used by all views for upload. */
    override fun markUpdated() = data.markUpdated()

    override fun read(offset: Int): Double {
        val value = data.storage.getComponent(offset / itemSize, this.offset + offset % itemSize)
        return if (normalized && data.storage is Uint8BufferAttribute) value / 255.0 else value
    }
}
