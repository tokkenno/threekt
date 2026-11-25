package com.aitorgf.threekt.math

import com.aitorgf.threekt.core.BufferAttribute
import kotlin.math.sqrt
import kotlin.js.JsExport
import kotlin.js.JsName

/** Fixed-size vector with mutable components. Arithmetic methods mutate the receiver. */
@JsExport
sealed class MutableVector<V : MutableVector<V>> {
    /** Number of components in the vector. */
    abstract val dimension: Int

    /** Returns the component at the zero-based [index]. */
    abstract operator fun get(index: Int): Double

    /** Sets the component at the zero-based [index]. */
    @JsName("setComponent")
    abstract operator fun set(index: Int, value: Double)

    /** Returns a new instance with the same values. */
    abstract fun copy(): V

    /** Computes the dot product with [vector]. */
    abstract fun dot(vector: V): Double

    /** Sets every component to [scalar] and returns this vector. */
    abstract fun setScalar(scalar: Double): V

    /** Multiplies each component by [scalar] and returns this vector. */
    abstract fun multiplyScalar(scalar: Double): V

    protected abstract val self: V

    /** Returns the squared Euclidean length. Use it for comparisons that do not require the square root. */
    fun lengthSq(): Double = dot(self)

    /** Returns the Euclidean length. */
    fun length(): Double = sqrt(lengthSq())

    /** Divides each component by [scalar] and returns this vector. */
    fun divideScalar(scalar: Double): V = multiplyScalar(1.0 / scalar)

    /** Scales the vector to unit length and returns it. A zero-length vector stays unchanged. */
    fun normalize(): V {
        val magnitude = length()
        return if (magnitude == 0.0) self else divideScalar(magnitude)
    }

    /** Scales the vector to the requested [length] while preserving its direction. Returns this vector. */
    fun setLength(length: Double): V = normalize().multiplyScalar(length)

    /**
     * Clamps the length to the interval [min] through [max] while preserving direction. A zero-length
     * vector stays unchanged.
     */
    fun clampLength(min: Double, max: Double): V {
        val magnitude = length()
        val clamped = magnitude.coerceIn(min, max)
        return if (magnitude == 0.0 || magnitude == clamped) self else multiplyScalar(clamped / magnitude)
    }

    /** Copies [dimension] components from [array] starting at [offset] and returns this vector. */
    fun fromArray(array: DoubleArray, offset: Int = 0): V {
        require(offset >= 0 && offset <= array.size - dimension)
        for (index in 0 until dimension) this[index] = array[offset + index]
        return self
    }

    /** Writes the components to [array] starting at [offset] and returns the array. */
    fun toArray(array: DoubleArray = DoubleArray(dimension), offset: Int = 0): DoubleArray {
        require(offset >= 0 && offset <= array.size - dimension)
        for (index in 0 until dimension) array[offset + index] = this[index]
        return array
    }

    /** Reads the first [dimension] components of the attribute item at [index] into this vector. */
    fun fromBufferAttribute(attribute: BufferAttribute, index: Int): V {
        require(index in 0 until attribute.count && dimension <= attribute.itemSize)
        for (component in 0 until dimension) this[component] = attribute.getComponent(index, component)
        return self
    }

    /** Compares corresponding components using Kotlin double value equality. */
    protected fun sameComponents(other: V): Boolean =
        (0 until dimension).all { this[it].toBits() == other[it].toBits() }

    /** Computes the current components' hash in component order. */
    protected fun componentHash(): Int {
        var hash = 1
        for (index in 0 until dimension) hash = 31 * hash + this[index].hashCode()
        return hash
    }
}
