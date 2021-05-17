package com.aitorgf.threekt.types

/**
 * Represents a complex buffer where each item in the buffer occupies several values.
 */
@ExperimentalJsExport
@JsExport
actual external interface ComplexBuffer : Buffer {
    /**
     * Get the value in [itemPosition] for the item in the buffer [position].
     */
    @JsName("getComplexValue")
    actual fun getComplex(position: Int, itemPosition: Int): Double

    /**
     * The number of values for each element.
     */
    actual val itemSize: Int

    /**
     * The number of items in this buffer.
     */
    actual val count: Int
}
