package com.aitorgf.threekt.core

import com.aitorgf.threekt.extras.DataUtils
import kotlin.js.JsExport

/**
 * Attribute backed by half floats, which halve the memory of 32-bit floats at the cost of precision.
 * Each element holds the bits produced by [DataUtils.toHalfFloat]. The bits are stored in a
 * [ShortArray] (an `Int16Array` in JavaScript), where Three.js uses a `Uint16Array`. Three.js stores
 * it in a `Uint16Array`. The supplied [array] is retained.
 */
@JsExport
open class Float16BufferAttribute(
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

    override val normalizationScale: Double? get() = 65535.0

    override fun readRaw(offset: Int): Double = DataUtils.fromHalfFloat(array[offset].toInt() and 0xffff)

    override fun writeRaw(offset: Int, value: Double) {
        array[offset] = DataUtils.toHalfFloat(value).toShort()
    }

    /** Copies [value] into the array from element [offset]. Returns this attribute. */
    fun set(value: ShortArray, offset: Int = 0): Float16BufferAttribute {
        value.copyInto(array, offset)
        return this
    }

    /** Copies [value] into the start of the array. Returns this attribute. */
    fun copyArray(value: ShortArray): Float16BufferAttribute = set(value)

    /**
     * Copies the settings of [source], which must be a [Float16BufferAttribute], and replaces the array with a copy of
     * its array. The [itemSize] of this attribute is kept. Returns this attribute.
     */
    override fun copy(source: BufferAttribute): Float16BufferAttribute {
        require(source is Float16BufferAttribute) { "Expected a Float16BufferAttribute." }
        super.copy(source)
        array = source.array.copyOf()
        return this
    }

    override fun clone(): Float16BufferAttribute = Float16BufferAttribute(array, itemSize, normalized).copy(this)
}
