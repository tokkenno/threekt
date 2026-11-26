package com.aitorgf.threekt.core

import kotlin.js.JsExport

/**
 * Attribute backed by 32-bit floats, the usual storage for positions, normals and UVs. Three.js stores
 * it in a `Float32Array`. The supplied [array] is retained.
 */
@JsExport
open class Float32BufferAttribute(
    array: FloatArray,
    itemSize: Int,
    normalized: Boolean = false,
) : BufferAttribute(itemSize, normalized) {
    /** The stored values. Element writes take effect on the next upload after [markUpdated]; [copy] replaces the array. */
    var array: FloatArray = array
        private set

    init {
        requireWholeItems()
    }

    override val size: Int get() = array.size

    override val bytesPerElement: Int get() = 4

    override val normalizationScale: Double? get() = null

    override fun readRaw(offset: Int): Double = array[offset].toDouble()

    override fun writeRaw(offset: Int, value: Double) {
        array[offset] = value.toFloat()
    }

    /** Copies [value] into the array from element [offset]. Returns this attribute. */
    fun set(value: FloatArray, offset: Int = 0): Float32BufferAttribute {
        value.copyInto(array, offset)
        return this
    }

    /** Copies [value] into the start of the array. Returns this attribute. */
    fun copyArray(value: FloatArray): Float32BufferAttribute = set(value)

    /**
     * Copies the settings of [source], which must be a [Float32BufferAttribute], and replaces the array with a copy of
     * its array. The [itemSize] of this attribute is kept. Returns this attribute.
     */
    override fun copy(source: BufferAttribute): Float32BufferAttribute {
        require(source is Float32BufferAttribute) { "Expected a Float32BufferAttribute." }
        super.copy(source)
        array = source.array.copyOf()
        return this
    }

    override fun clone(): Float32BufferAttribute = Float32BufferAttribute(array, itemSize, normalized).copy(this)
}
