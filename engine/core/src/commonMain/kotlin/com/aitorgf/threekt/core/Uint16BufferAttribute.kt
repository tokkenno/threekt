package com.aitorgf.threekt.core

import kotlin.js.JsExport

/**
 * Attribute backed by unsigned 16-bit integers, typically indices of geometries with fewer than 65535
 * vertices. The values are stored in a [ShortArray] (an `Int16Array` in JavaScript) and read as
 * `0..65535`. Three.js stores it in a `Uint16Array`. The supplied [array] is retained.
 */
@JsExport
open class Uint16BufferAttribute(
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

    override fun createEmpty(size: Int, itemSize: Int, normalized: Boolean): BufferAttribute =
        Uint16BufferAttribute(ShortArray(size), itemSize, normalized)

    override fun readRaw(offset: Int): Double = (array[offset].toInt() and 0xffff).toDouble()

    override fun writeRaw(offset: Int, value: Double) {
        array[offset] = toInt32(value).toShort()
    }

    /** Copies [value] into the array from element [offset]. Returns this attribute. */
    fun set(value: ShortArray, offset: Int = 0): Uint16BufferAttribute {
        value.copyInto(array, offset)
        return this
    }

    /** Copies [value] into the start of the array. Returns this attribute. */
    fun copyArray(value: ShortArray): Uint16BufferAttribute = set(value)

    /**
     * Copies the settings of [source], which must be a [Uint16BufferAttribute], and replaces the array with a copy of
     * its array. The [itemSize] of this attribute is kept. Returns this attribute.
     */
    override fun copy(source: BufferAttribute): Uint16BufferAttribute {
        require(source is Uint16BufferAttribute) { "Expected a Uint16BufferAttribute." }
        super.copy(source)
        array = source.array.copyOf()
        return this
    }

    override fun clone(): Uint16BufferAttribute = Uint16BufferAttribute(array, itemSize, normalized).copy(this)
}
