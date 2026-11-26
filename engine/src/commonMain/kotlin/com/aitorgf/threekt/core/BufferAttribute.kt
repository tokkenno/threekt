package com.aitorgf.threekt.core

import com.aitorgf.threekt.math.Matrix3
import com.aitorgf.threekt.math.Matrix4
import com.aitorgf.threekt.math.Vector2
import com.aitorgf.threekt.math.Vector3
import kotlin.js.JsExport
import kotlin.math.floor

/**
 * Vertex data exposed as items of [itemSize] components, stored in a typed array by one of the subclasses
 * such as [Float32BufferAttribute] or [Uint16BufferAttribute]. Renderers upload the data once and again
 * after [markUpdated] is called.
 *
 * Deviations from Three.js:
 * - Three.js has a single class that wraps any typed array; Threekt has one subclass per array type, each
 *   with a typed `array`, because Kotlin has no common type for its primitive arrays.
 * - [markUpdated] replaces the `needsUpdate = true` setter.
 * - `toJSON()` is not available yet, because the object loader has not been ported.
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
) : EventDispatcher() {
    init {
        require(itemSize > 0) { "Items must have at least one component." }
    }

    /** Unique number of this attribute, assigned in creation order. */
    val id: Int = nextId++

    /** Optional name, used for example as the key of a morph target. */
    var name = ""

    /** Expected update pattern, which renderers pass to the GPU as a hint. Change it before the first draw. */
    open val usage: Usage get() = attributeUsage
    private var attributeUsage = Usage.StaticDraw

    /** Sets [usage] and returns this attribute. */
    open fun setUsage(value: Usage): BufferAttribute {
        attributeUsage = value
        return this
    }

    /**
     * Ranges of array elements to upload on the next update instead of the whole array. The renderer merges
     * overlapping ranges, uploads them and clears the list. Read-only snapshot; change it with
     * [addUpdateRange] and [clearUpdateRanges].
     */
    val updateRanges: Array<UpdateRange> get() = ranges.toTypedArray()
    internal val ranges = mutableListOf<UpdateRange>()

    /** A range of [count] array elements, not items, from element [start]. */
    class UpdateRange(var start: Int, var count: Int)

    /**
     * Adds a range of [count] array elements from element [start] to upload on the next update. Call
     * [markUpdated] as well.
     */
    fun addUpdateRange(start: Int, count: Int) {
        ranges.add(UpdateRange(start, count))
    }

    /** Clears [updateRanges], so the next update uploads the whole array. */
    fun clearUpdateRanges() {
        ranges.clear()
    }

    /** Shader input type for integer-backed data. Has no effect on floating-point storage. */
    var gpuType = GpuType.Float

    /** Number of elements in the underlying array. */
    abstract val size: Int

    /** Number of complete items. */
    val count: Int get() = size / itemSize

    /** Size in bytes of one array element, as `BYTES_PER_ELEMENT` of the typed array. */
    abstract val bytesPerElement: Int

    /** Incremented by [markUpdated] so that renderers know to upload the data again. */
    open val version: Int get() = attributeVersion
    internal var attributeVersion = 0

    /** Marks the data as changed so that it is uploaded before the next draw. Replaces `needsUpdate = true`. */
    open fun markUpdated() {
        attributeVersion++
    }

    /** Called by the renderer after each upload of the data. Does nothing by default. */
    var onUploadCallback: () -> Unit = {}

    /**
     * Sets [onUploadCallback], for example to release the array after the data reaches the GPU. Returns
     * this attribute.
     */
    fun onUpload(callback: () -> Unit): BufferAttribute {
        onUploadCallback = callback
        return this
    }

    /**
     * Divisor of normalized integer data: the largest value of the array type, such as `255` for unsigned
     * bytes. Null for floating-point arrays, which are never normalized.
     */
    internal abstract val normalizationScale: Double?

    /** Returns a new attribute with the same array type, holding [size] zeroed elements. */
    internal abstract fun createEmpty(size: Int, itemSize: Int, normalized: Boolean): BufferAttribute

    /** Reads array element [offset] without normalization. */
    internal abstract fun readRaw(offset: Int): Double

    /** Writes array element [offset] without normalization, converting as a JavaScript typed array does. */
    internal abstract fun writeRaw(offset: Int, value: Double)

    private fun denormalize(value: Double): Double {
        val scale = normalizationScale ?: return value
        return maxOf(value / scale, -1.0)
    }

    private fun normalize(value: Double): Double {
        val scale = normalizationScale ?: return value
        return floor(value * scale + 0.5)
    }

    /** Reads array element [offset], denormalized when [normalized] is set. */
    internal open fun read(offset: Int): Double {
        val value = readRaw(offset)
        return if (normalized) denormalize(value) else value
    }

    /** Writes array element [offset], normalizing [value] when [normalized] is set. */
    internal open fun write(offset: Int, value: Double) {
        writeRaw(offset, if (normalized) normalize(value) else value)
    }

    /**
     * Copies the raw components of item [index2] of [attribute] into item [index1] of this attribute.
     * Returns this attribute.
     */
    fun copyAt(index1: Int, attribute: BufferAttribute, index2: Int): BufferAttribute {
        val offset1 = index1 * itemSize
        val offset2 = index2 * attribute.itemSize
        for (i in 0 until itemSize) writeRaw(offset1 + i, attribute.readRaw(offset2 + i))
        return this
    }

    /** Returns component [component] of item [index], denormalized when [normalized] is set. */
    fun getComponent(index: Int, component: Int): Double = read(index * itemSize + component)

    /**
     * Sets component [component] of item [index]. Normalized integer attributes store the value scaled to
     * their range and rounded. Call [markUpdated] afterwards so the change is uploaded. Returns this attribute.
     */
    fun setComponent(index: Int, component: Int, value: Double): BufferAttribute {
        write(index * itemSize + component, value)
        return this
    }

    /** Returns the first component of item [index]. */
    fun getX(index: Int): Double = getComponent(index, 0)

    /** Returns the second component of item [index]. */
    fun getY(index: Int): Double = getComponent(index, 1)

    /** Returns the third component of item [index]. */
    fun getZ(index: Int): Double = getComponent(index, 2)

    /** Returns the fourth component of item [index]. */
    fun getW(index: Int): Double = getComponent(index, 3)

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

    /**
     * Transforms each item by [m]: as a 2D point with a homogeneous coordinate when [itemSize] is 2, or as
     * a 3D vector when it is 3. Other item sizes are left unchanged. Returns this attribute.
     */
    fun applyMatrix3(m: Matrix3): BufferAttribute {
        if (itemSize == 2) {
            val vector = Vector2()
            for (i in 0 until count) {
                vector.fromBufferAttribute(this, i).applyMatrix3(m)
                setXY(i, vector.x, vector.y)
            }
        } else if (itemSize == 3) {
            val vector = Vector3()
            for (i in 0 until count) {
                vector.fromBufferAttribute(this, i).applyMatrix3(m)
                setXYZ(i, vector.x, vector.y, vector.z)
            }
        }
        return this
    }

    /** Transforms each item as a point by [m]. Requires three components. Returns this attribute. */
    fun applyMatrix4(m: Matrix4): BufferAttribute {
        val vector = Vector3()
        for (i in 0 until count) {
            vector.fromBufferAttribute(this, i).applyMatrix4(m)
            setXYZ(i, vector.x, vector.y, vector.z)
        }
        return this
    }

    /** Transforms each item as a normal by [m] and normalizes it. Returns this attribute. */
    fun applyNormalMatrix(m: Matrix3): BufferAttribute {
        val vector = Vector3()
        for (i in 0 until count) {
            vector.fromBufferAttribute(this, i).applyNormalMatrix(m)
            setXYZ(i, vector.x, vector.y, vector.z)
        }
        return this
    }

    /** Transforms each item as a direction by [m] and normalizes it. Returns this attribute. */
    fun transformDirection(m: Matrix4): BufferAttribute {
        val vector = Vector3()
        for (i in 0 until count) {
            vector.fromBufferAttribute(this, i).transformDirection(m)
            setXYZ(i, vector.x, vector.y, vector.z)
        }
        return this
    }

    /** Returns an independent copy of this attribute with the same type, settings and values. */
    abstract fun clone(): BufferAttribute

    /**
     * Copies the settings and a copy of the array of [source], which must have the same type as this
     * attribute. Returns this attribute.
     */
    open fun copy(source: BufferAttribute): BufferAttribute {
        name = source.name
        normalized = source.normalized
        attributeUsage = source.usage
        gpuType = source.gpuType
        return this
    }

    /** Dispatches a `dispose` event so that renderers release the GPU buffer of this attribute. */
    fun dispose() {
        dispatchEvent(DispatchedEvent(DISPOSE))
    }

    /** Validates that [size] holds a whole number of items. Call it from subclass initializers. */
    protected fun requireWholeItems() {
        require(size % itemSize == 0) { "The buffer must contain a whole number of items." }
    }

    companion object {
        private var nextId = 0

        /** Type of the event dispatched by [dispose]. */
        const val DISPOSE = "dispose"
    }
}

/**
 * Converts [value] to a 32-bit integer as JavaScript does when storing it in an integer typed array:
 * truncation toward zero, wrapping modulo 2³², and zero for NaN and infinities.
 */
internal fun toInt32(value: Double): Int {
    if (!value.isFinite()) return 0
    val truncated = if (value < 0) -floor(-value) else floor(value)
    return (truncated % 4294967296.0).toLong().toInt()
}
