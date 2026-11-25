package com.aitorgf.threekt.core

import com.aitorgf.threekt.math.Matrix3
import com.aitorgf.threekt.math.Matrix4
import com.aitorgf.threekt.math.Vector3
import kotlin.js.JsExport

/**
 * Vertex data exposed as items of [itemSize] components.
 * Renderers upload the data once and again after [markUpdated] is called, which replaces the Three.js
 * `needsUpdate = true` setter.
 */
@JsExport
abstract class BufferAttribute(
    /** Number of components per item, for example `3` for positions. */
    val itemSize: Int,
    /**
     * Whether integer components are mapped to `[0, 1]` (unsigned) or `[-1, 1]` (signed) when read by
     * shaders and by [getComponent]. Has no effect on floating-point attributes.
     */
    var normalized: Boolean,
) {
    init {
        require(itemSize > 0) { "Items must have at least one component." }
    }

    /** Expected update pattern, which renderers pass to the GPU as a hint. Change it before the first draw. */
    open val usage: Usage get() = attributeUsage
    private var attributeUsage = Usage.StaticDraw

    /** Sets [usage] and returns this attribute, as Three.js `setUsage()` does. */
    open fun setUsage(value: Usage): BufferAttribute {
        attributeUsage = value
        return this
    }

    /** Shader input type for integer-backed data. Has no effect on floating-point storage. */
    var gpuType = GpuType.Float

    /** Optional name, used for example as the key of a morph target. */
    var name = ""

    /** Number of components exposed by this attribute. */
    abstract val size: Int

    /** Number of complete items. */
    val count: Int get() = size / itemSize

    /** Incremented by [markUpdated] so that renderers know to upload the data again. */
    open val version: Int get() = attributeVersion
    private var attributeVersion = 0

    /** Marks the data as changed so that it is uploaded before the next draw. */
    open fun markUpdated() {
        attributeVersion++
    }

    /** Returns component [component] of item [index], normalized when [normalized] is set. */
    fun getComponent(index: Int, component: Int): Double = read(index * itemSize + component)

    /** Returns the first component of item [index]. */
    fun getX(index: Int): Double = getComponent(index, 0)

    /** Returns the second component of item [index]. */
    fun getY(index: Int): Double = getComponent(index, 1)

    /** Returns the third component of item [index]. */
    fun getZ(index: Int): Double = getComponent(index, 2)

    /** Returns the fourth component of item [index]. */
    fun getW(index: Int): Double = getComponent(index, 3)

    /**
     * Sets component [component] of item [index]. Normalized integer attributes store the value scaled to
     * their range and rounded, as Three.js does. Call [markUpdated] afterwards so the change is uploaded.
     */
    fun setComponent(index: Int, component: Int, value: Double): BufferAttribute {
        write(index * itemSize + component, value)
        return this
    }

    /** Sets the first component of item [index] and returns this attribute. */
    fun setX(index: Int, x: Double): BufferAttribute = setComponent(index, 0, x)

    /** Sets the second component of item [index] and returns this attribute. */
    fun setY(index: Int, y: Double): BufferAttribute = setComponent(index, 1, y)

    /** Sets the third component of item [index] and returns this attribute. */
    fun setZ(index: Int, z: Double): BufferAttribute = setComponent(index, 2, z)

    /** Sets the fourth component of item [index] and returns this attribute. */
    fun setW(index: Int, w: Double): BufferAttribute = setComponent(index, 3, w)

    /** Sets the first two components of item [index] and returns this attribute. */
    fun setXY(index: Int, x: Double, y: Double): BufferAttribute = setX(index, x).setY(index, y)

    /** Sets the first three components of item [index] and returns this attribute. */
    fun setXYZ(index: Int, x: Double, y: Double, z: Double): BufferAttribute = setXY(index, x, y).setZ(index, z)

    /** Sets four components of item [index] and returns this attribute. */
    fun setXYZW(index: Int, x: Double, y: Double, z: Double, w: Double): BufferAttribute =
        setXYZ(index, x, y, z).setW(index, w)

    /** Transforms each item as a point by [matrix]. Requires three components. Returns this attribute. */
    fun applyMatrix4(matrix: Matrix4): BufferAttribute {
        val vector = Vector3()
        for (i in 0 until count) {
            vector.fromBufferAttribute(this, i).applyMatrix4(matrix)
            setXYZ(i, vector.x, vector.y, vector.z)
        }
        return this
    }

    /** Transforms each item as a normal by [matrix] and normalizes it. Returns this attribute. */
    fun applyNormalMatrix(matrix: Matrix3): BufferAttribute {
        val vector = Vector3()
        for (i in 0 until count) {
            vector.fromBufferAttribute(this, i).applyNormalMatrix(matrix)
            setXYZ(i, vector.x, vector.y, vector.z)
        }
        return this
    }

    /** Transforms each item as a direction by [matrix] and normalizes it. Returns this attribute. */
    fun transformDirection(matrix: Matrix4): BufferAttribute {
        val vector = Vector3()
        for (i in 0 until count) {
            vector.fromBufferAttribute(this, i).transformDirection(matrix)
            setXYZ(i, vector.x, vector.y, vector.z)
        }
        return this
    }

    /** Returns an independent copy of this attribute with the same type, settings and values. */
    abstract fun clone(): BufferAttribute

    /** Copies [usage], [gpuType] and [name] from [source] and returns this attribute. */
    internal fun copySettings(source: BufferAttribute): BufferAttribute {
        attributeUsage = source.usage
        gpuType = source.gpuType
        name = source.name
        return this
    }

    /** Reads the array element at [offset], applying normalization when it applies. */
    protected abstract fun read(offset: Int): Double

    /** Writes the array element at [offset], applying normalization when it applies. */
    protected abstract fun write(offset: Int, value: Double)

    /** Validates that [size] holds a whole number of items. Call it from subclass initializers. */
    protected fun requireWholeItems() {
        require(size % itemSize == 0) { "The buffer must contain a whole number of items." }
    }
}
