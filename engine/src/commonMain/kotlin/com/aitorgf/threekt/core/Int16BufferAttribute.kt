package com.aitorgf.threekt.core

import kotlin.js.JsExport

/**
 * Attribute backed by signed 16-bit integers, typically compact normals. When [normalized] is set,
 * values map to `[-1, 1]` as `max(value / 32767, -1)`. The supplied [array] is retained.
 */
@JsExport
class Int16BufferAttribute(
    val array: ShortArray,
    itemSize: Int,
    normalized: Boolean = false,
) : BufferAttribute(itemSize, normalized) {
    init {
        requireWholeItems()
    }

    override val size: Int get() = array.size

    override fun read(offset: Int): Double {
        val value = array[offset].toDouble()
        return if (normalized) maxOf(value / 32767.0, -1.0) else value
    }

    override fun clone(): Int16BufferAttribute =
        Int16BufferAttribute(array.copyOf(), itemSize, normalized).also { it.copySettings(this) }

    override fun write(offset: Int, value: Double) {
        array[offset] = kotlin.math.round(if (normalized) value * 32767.0 else value).toInt().toShort()
    }
}
