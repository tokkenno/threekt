package com.aitorgf.threekt.core

import kotlin.js.JsExport

/**
 * Attribute backed by signed 32-bit integers. Three.js stores it in a `Int32Array`. The supplied
 * [array] is retained.
 */
@JsExport
open class Int32BufferAttribute(
    array: IntArray,
    itemSize: Int,
    normalized: Boolean = false,
) : BufferAttribute(itemSize, normalized) {
    /** The stored values. Element writes take effect on the next upload after [markUpdated]; [copy] replaces the array. */
    var array: IntArray = array
        private set

    init {
        requireWholeItems()
    }

    override val size: Int get() = array.size

    override val bytesPerElement: Int get() = 4

    override val normalizationScale: Double? get() = 2147483647.0

    override fun createEmpty(size: Int, itemSize: Int, normalized: Boolean): BufferAttribute =
        Int32BufferAttribute(IntArray(size), itemSize, normalized)

    override fun readRaw(offset: Int): Double = array[offset].toDouble()

    override fun writeRaw(offset: Int, value: Double) {
        array[offset] = toInt32(value)
    }

    /** Copies [value] into the array from element [offset]. Returns this attribute. */
    fun set(value: IntArray, offset: Int = 0): Int32BufferAttribute {
        value.copyInto(array, offset)
        return this
    }

    /** Copies [value] into the start of the array. Returns this attribute. */
    fun copyArray(value: IntArray): Int32BufferAttribute = set(value)

    /**
     * Copies the settings of [source], which must be a [Int32BufferAttribute], and replaces the array with a copy of
     * its array. The [itemSize] of this attribute is kept. Returns this attribute.
     */
    override fun copy(source: BufferAttribute): Int32BufferAttribute {
        require(source is Int32BufferAttribute) { "Expected a Int32BufferAttribute." }
        super.copy(source)
        array = source.array.copyOf()
        return this
    }

    override fun clone(): Int32BufferAttribute = Int32BufferAttribute(array, itemSize, normalized).copy(this)
}
