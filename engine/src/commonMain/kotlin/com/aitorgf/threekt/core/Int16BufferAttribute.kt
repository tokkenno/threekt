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
}
