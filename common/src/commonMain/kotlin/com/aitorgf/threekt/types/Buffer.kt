package com.aitorgf.threekt.types

import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport
import kotlin.js.JsName

/**
 * Represents a buffer of values.
 */
expect interface Buffer {
    /**
     * Get the value in [position].
     */
    fun get(position: Int): Double

    /**
     * Get [count] values from buffer in [position].
     */
    fun get(position: Int, count: Int): DoubleArray

    /**
     * Set the [value] in [position].
     */
    fun set(value: Double, position: Int): Buffer

    /**
     * Set the [arr] values in buffer, starting by [position].
     */
    fun set(arr: DoubleArray, position: Int): Buffer

    /**
     * Set the [arr] values in buffer, starting by [position].
     */
    fun set(arr: FloatArray, position: Int): Buffer

    /** Get the size of the buffer */
    val size: Int
}
