package com.aitorgf.threekt.core

import kotlin.js.JsExport

/**
 * Attribute backed by signed bytes. When [normalized] is set, values map to `[-1, 1]` as `max(value /
 * 127, -1)`. Three.js stores it in a `Int8Array`. The supplied [array] is retained.
 */
@JsExport
open class Int8BufferAttribute(
    array: ByteArray,
    itemSize: Int,
    normalized: Boolean = false,
) : BufferAttribute(itemSize, normalized) {
    /** The stored values. Element writes take effect on the next upload after [markUpdated]; [copy] replaces the array. */
    var array: ByteArray = array
        private set

    init {
        requireWholeItems()
    }

    override val size: Int get() = array.size

    override val bytesPerElement: Int get() = 1

    override val normalizationScale: Double? get() = 127.0

    override fun readRaw(offset: Int): Double = array[offset].toDouble()

    override fun writeRaw(offset: Int, value: Double) {
        array[offset] = toInt32(value).toByte()
    }

    /** Copies [value] into the array from element [offset]. Returns this attribute. */
    fun set(value: ByteArray, offset: Int = 0): Int8BufferAttribute {
        value.copyInto(array, offset)
        return this
    }

    /** Copies [value] into the start of the array. Returns this attribute. */
    fun copyArray(value: ByteArray): Int8BufferAttribute = set(value)

    /**
     * Copies the settings of [source], which must be a [Int8BufferAttribute], and replaces the array with a copy of
     * its array. The [itemSize] of this attribute is kept. Returns this attribute.
     */
    override fun copy(source: BufferAttribute): Int8BufferAttribute {
        require(source is Int8BufferAttribute) { "Expected a Int8BufferAttribute." }
        super.copy(source)
        array = source.array.copyOf()
        return this
    }

    override fun clone(): Int8BufferAttribute = Int8BufferAttribute(array, itemSize, normalized).copy(this)
}
