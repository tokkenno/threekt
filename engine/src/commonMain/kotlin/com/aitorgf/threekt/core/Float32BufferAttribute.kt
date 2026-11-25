package com.aitorgf.threekt.core

import kotlin.js.JsExport

/** Attribute backed by 32-bit floats. The supplied [array] is retained. */
@JsExport
class Float32BufferAttribute(
    val array: FloatArray,
    itemSize: Int,
    normalized: Boolean = false,
) : BufferAttribute(itemSize, normalized) {
    init {
        requireWholeItems()
    }

    override val size: Int get() = array.size

    override fun read(offset: Int): Double = array[offset].toDouble()
}
