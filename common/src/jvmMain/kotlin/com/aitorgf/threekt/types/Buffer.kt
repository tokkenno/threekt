package com.aitorgf.threekt.types

/**
 * Represents a buffer of values.
 */
actual interface Buffer {
    /**
     * Get the value in [position].
     */
    actual fun get(position: Int): Double

    /**
     * Get [count] values from buffer in [position].
     */
    actual fun get(position: Int, count: Int): DoubleArray

    /**
     * Set the [value] in [position].
     */
    actual fun set(value: Double, position: Int): Buffer

    /**
     * Set the [arr] values in buffer, starting by [position].
     */
    actual fun set(arr: DoubleArray, position: Int): Buffer

    /**
     * Set the [arr] values in buffer, starting by [position].
     */
    actual fun set(arr: FloatArray, position: Int): Buffer

    /** Get the size of the buffer */
    actual val size: Int
}
