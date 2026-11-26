package com.aitorgf.threekt.core

import kotlin.js.JsExport
import kotlin.math.round

/**
 * Attribute backed by unsigned bytes that clamp written values to `0..255` and round them to the
 * nearest integer, ties to even, as a `Uint8ClampedArray`. The bytes are stored in a [ByteArray] (an
 * `Int8Array` in JavaScript) and read as `0..255`. Three.js stores it in a `Uint8ClampedArray`. The
 * supplied [array] is retained.
 */
@JsExport
open class Uint8ClampedBufferAttribute(
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
        array[offset] = (if (value.isNaN()) 0.0 else round(value.coerceIn(0.0, 255.0))).toInt().toByte()
    }

    /** Copies [value] into the array from element [offset]. Returns this attribute. */
    fun set(value: ByteArray, offset: Int = 0): Uint8ClampedBufferAttribute {
        value.copyInto(array, offset)
        return this
    }

    /** Copies [value] into the start of the array. Returns this attribute. */
    fun copyArray(value: ByteArray): Uint8ClampedBufferAttribute = set(value)

    /**
     * Copies the settings of [source], which must be a [Uint8ClampedBufferAttribute], and replaces the array with a copy of
     * its array. The [itemSize] of this attribute is kept. Returns this attribute.
     */
    override fun copy(source: BufferAttribute): Uint8ClampedBufferAttribute {
        require(source is Uint8ClampedBufferAttribute) { "Expected a Uint8ClampedBufferAttribute." }
        super.copy(source)
        array = source.array.copyOf()
        return this
    }

    override fun clone(): Uint8ClampedBufferAttribute = Uint8ClampedBufferAttribute(array, itemSize, normalized).copy(this)
}
