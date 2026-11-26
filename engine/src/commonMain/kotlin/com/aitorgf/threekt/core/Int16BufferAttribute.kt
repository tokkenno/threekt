package com.aitorgf.threekt.core

import kotlin.js.JsExport

/**
 * Attribute backed by signed 16-bit integers, typically compact normals. When [normalized] is set,
 * values map to `[-1, 1]` as `max(value / 32767, -1)`. Three.js stores it in a `Int16Array`. The
 * supplied [array] is retained.
 */
@JsExport
open class Int16BufferAttribute(
    array: ShortArray,
    itemSize: Int,
    normalized: Boolean = false,
) : BufferAttribute(itemSize, normalized) {
    /** The stored values. Element writes take effect on the next upload after [markUpdated]; [copy] replaces the array. */
    var array: ShortArray = array
        private set

    init {
        requireWholeItems()
    }

    override val size: Int get() = array.size

    override val bytesPerElement: Int get() = 2

    override val normalizationScale: Double? get() = 32767.0

    override fun readRaw(offset: Int): Double = array[offset].toDouble()

    override fun writeRaw(offset: Int, value: Double) {
        array[offset] = toInt32(value).toShort()
    }

    /** Copies [value] into the array from element [offset]. Returns this attribute. */
    fun set(value: ShortArray, offset: Int = 0): Int16BufferAttribute {
        value.copyInto(array, offset)
        return this
    }

    /** Copies [value] into the start of the array. Returns this attribute. */
    fun copyArray(value: ShortArray): Int16BufferAttribute = set(value)

    /**
     * Copies the settings of [source], which must be a [Int16BufferAttribute], and replaces the array with a copy of
     * its array. The [itemSize] of this attribute is kept. Returns this attribute.
     */
    override fun copy(source: BufferAttribute): Int16BufferAttribute {
        require(source is Int16BufferAttribute) { "Expected a Int16BufferAttribute." }
        super.copy(source)
        array = source.array.copyOf()
        return this
    }

    override fun clone(): Int16BufferAttribute = Int16BufferAttribute(array, itemSize, normalized).copy(this)
}
