package com.aitorgf.threekt.core

import kotlin.js.JsExport

/**
 * Attribute backed by unsigned bytes, typically colors. The bytes are stored in a [ByteArray] (an
 * `Int8Array` in JavaScript), whose values are reinterpreted as `0..255`: a stored `-1` reads as `255`.
 * Assigning numbers from `0` to `255` to an `Int8Array` produces exactly these bytes. The supplied
 * [array] is retained.
 */
@JsExport
class Uint8BufferAttribute(
    val array: ByteArray,
    itemSize: Int,
    normalized: Boolean = false,
) : BufferAttribute(itemSize, normalized) {
    init {
        requireWholeItems()
    }

    override val size: Int get() = array.size

    /** Returns the unsigned value at [offset] of the underlying array. */
    fun getUnsigned(offset: Int): Int = array[offset].toInt() and 0xff

    override fun read(offset: Int): Double {
        val value = getUnsigned(offset).toDouble()
        return if (normalized) value / 255.0 else value
    }

    override fun clone(): Uint8BufferAttribute =
        Uint8BufferAttribute(array.copyOf(), itemSize, normalized).also { it.copySettings(this) }

    override fun write(offset: Int, value: Double) {
        array[offset] = kotlin.math.round(if (normalized) value * 255.0 else value).toInt().toByte()
    }
}
