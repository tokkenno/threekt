package com.aitorgf.threekt.core

import kotlin.js.JsExport

/**
 * Attribute backed by unsigned bytes, typically colors. When [normalized] is set, values map to `[0,
 * 1]` as `value / 255`. The bytes are stored in a [ByteArray] (an `Int8Array` in JavaScript) and read
 * as `0..255`: a stored `-1` reads as `255`. Assigning numbers from `0` to `255` to an `Int8Array`
 * produces exactly these bytes. Three.js stores it in a `Uint8Array`. The supplied [array] is
 * retained.
 */
@JsExport
open class Uint8BufferAttribute(
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

    override val normalizationScale: Double? get() = 255.0

    override fun readRaw(offset: Int): Double = (array[offset].toInt() and 0xff).toDouble()

    override fun writeRaw(offset: Int, value: Double) {
        array[offset] = toInt32(value).toByte()
    }

    /** Copies [value] into the array from element [offset]. Returns this attribute. */
    fun set(value: ByteArray, offset: Int = 0): Uint8BufferAttribute {
        value.copyInto(array, offset)
        return this
    }

    /** Copies [value] into the start of the array. Returns this attribute. */
    fun copyArray(value: ByteArray): Uint8BufferAttribute = set(value)

    /**
     * Copies the settings of [source], which must be a [Uint8BufferAttribute], and replaces the array with a copy of
     * its array. The [itemSize] of this attribute is kept. Returns this attribute.
     */
    override fun copy(source: BufferAttribute): Uint8BufferAttribute {
        require(source is Uint8BufferAttribute) { "Expected a Uint8BufferAttribute." }
        super.copy(source)
        array = source.array.copyOf()
        return this
    }

    override fun clone(): Uint8BufferAttribute = Uint8BufferAttribute(array, itemSize, normalized).copy(this)
}
