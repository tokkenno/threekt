package com.aitorgf.threekt.types

import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

/**
 * Represents a complex buffer where each item in the buffer occupies several values.
 */
expect interface ComplexBuffer: Buffer {
    /**
     * Get the value in [itemPosition] for the item in the buffer [position].
     */
    fun getComplex(position: Int, itemPosition: Int): Double

    /**
     * The number of values for each element.
     */
    val itemSize: Int

    /**
     * The number of items in this buffer.
     */
    val count: Int
}
