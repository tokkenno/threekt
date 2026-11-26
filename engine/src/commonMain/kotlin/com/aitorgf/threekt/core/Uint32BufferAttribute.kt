package com.aitorgf.threekt.core

import kotlin.js.JsExport

/**
 * Attribute backed by unsigned 32-bit integers, typically indices of large geometries. The values are
 * stored in an [IntArray] (an `Int32Array` in JavaScript) and read as `0..4294967295`. Three.js stores
 * it in a `Uint32Array`. The supplied [array] is retained.
 */
@JsExport
open class Uint32BufferAttribute(
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

    override val normalizationScale: Double? get() = 4294967295.0

    override fun readRaw(offset: Int): Double = (array[offset].toLong() and 0xffffffffL).toDouble()

    override fun writeRaw(offset: Int, value: Double) {
        array[offset] = toInt32(value)
    }

    /** Copies [value] into the array from element [offset]. Returns this attribute. */
    fun set(value: IntArray, offset: Int = 0): Uint32BufferAttribute {
        value.copyInto(array, offset)
        return this
    }

    /** Copies [value] into the start of the array. Returns this attribute. */
    fun copyArray(value: IntArray): Uint32BufferAttribute = set(value)

    /**
     * Copies the settings of [source], which must be a [Uint32BufferAttribute], and replaces the array with a copy of
     * its array. The [itemSize] of this attribute is kept. Returns this attribute.
     */
    override fun copy(source: BufferAttribute): Uint32BufferAttribute {
        require(source is Uint32BufferAttribute) { "Expected a Uint32BufferAttribute." }
        super.copy(source)
        array = source.array.copyOf()
        return this
    }

    override fun clone(): Uint32BufferAttribute = Uint32BufferAttribute(array, itemSize, normalized).copy(this)
}
