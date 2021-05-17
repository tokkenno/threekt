package com.aitorgf.threekt.core.buffers

import com.aitorgf.threekt.core.BufferAttribute
import com.aitorgf.threekt.math.*
import org.khronos.webgl.Float32Array
import org.khronos.webgl.get
import org.khronos.webgl.set

actual class Float32BufferAttribute actual constructor(
    itemSize: Int,
    normalized: Boolean
): BufferAttribute(itemSize, normalized) {
    var array: Float32Array = Float32Array(0)

    actual constructor(
        data: DoubleArray,
        itemSize: Int,
        normalized: Boolean
    ): this(itemSize, normalized) {
        this.array = Float32Array(data.map { it.toFloat() }.toTypedArray())
    }

    actual constructor(
        data: FloatArray,
        itemSize: Int,
        normalized: Boolean
    ): this(itemSize, normalized) {
        this.array = Float32Array(data.toTypedArray())
    }

    constructor(
        data: Float32Array,
        itemSize: Int,
        normalized: Boolean
    ): this(itemSize, normalized) {
        this.array = data
    }

    actual fun set(source: Float32BufferAttribute): Float32BufferAttribute {
        this.name = source.name
        this.array = Float32Array(source.array)
        this.itemSize = source.itemSize
        this.normalized = source.normalized
        this.usage = source.usage

        return this
    }

    actual fun copyAt(index1: Int, attribute: Float32BufferAttribute, index2: Int): Float32BufferAttribute {
        val calcIndex1 = index1 * this.itemSize
        val calcIndex2 = index2 * attribute.itemSize

        for (i in 0..this.itemSize - 1) {
            this.array.set(calcIndex1 + i, attribute.array.get(calcIndex2 + i))
        }

        return this
    }

    actual fun set(arr: DoubleArray): Float32BufferAttribute {
        this.array = Float32Array(arr.map { it.toFloat() }.toTypedArray())
        return this
    }

    actual fun set(arr: FloatArray): Float32BufferAttribute {
        this.array = Float32Array(arr.toTypedArray())
        return this
    }

    @ExperimentalJsExport
    actual fun set(colors: Array<Color>): Float32BufferAttribute {
        return this
    }

    @ExperimentalJsExport
    actual fun set(vectors: Array<Vector2>): Float32BufferAttribute {
        return this
    }

    @ExperimentalJsExport
    actual fun set(vectors: Array<Vector3>): Float32BufferAttribute {
        return this
    }

    @ExperimentalJsExport
    actual fun set(vectors: Array<Vector4>): Float32BufferAttribute {
        return this
    }

    @ExperimentalJsExport
    actual override fun apply(m: Matrix3): Float32BufferAttribute {
        return this
    }

    @ExperimentalJsExport
    actual override fun apply(m: Matrix4): Float32BufferAttribute {
        return this
    }

    @ExperimentalJsExport
    actual override fun applyNormalMatrix(m: Matrix3): Float32BufferAttribute {
        return this
    }

    @ExperimentalJsExport
    actual override fun transformDirection(m: Matrix4): Float32BufferAttribute {
        return this
    }

    actual fun set(value: Double, offset: Int): Float32BufferAttribute {
        this.array.set(offset, value.toFloat())
        return this
    }

    actual fun getX(index: Int): Double {
        return this.array.get(index * this.itemSize).toDouble()
    }

    actual fun setX(index: Int, x: Double): Float32BufferAttribute {
        this.array.set(index * this.itemSize, x.toFloat())
        return this
    }

    actual fun getY(index: Int): Double {
        return this.array.get(index * this.itemSize + 1).toDouble()
    }

    actual fun setY(index: Int, y: Double): Float32BufferAttribute {
        this.array.set(index * this.itemSize + 1, y.toFloat())
        return this
    }

    actual fun getZ(index: Int): Double {
        return this.array.get(index * this.itemSize + 2).toDouble()
    }

    actual fun setZ(index: Int, z: Double): Float32BufferAttribute {
        this.array.set(index * this.itemSize + 2, z.toFloat())
        return this
    }

    actual fun getW(index: Int): Double {
        return this.array.get(index * this.itemSize + 3).toDouble()
    }

    actual fun setW(index: Int, w: Double): Float32BufferAttribute {
        this.array.set(index * this.itemSize + 3, w.toFloat())
        return this
    }

    actual fun setXY(index: Int, x: Double, y: Double): Float32BufferAttribute {
        val calcIndex = index * this.itemSize

        this.array[calcIndex + 0] = x.toFloat()
        this.array[calcIndex + 1] = y.toFloat()

        return this
    }

    actual fun setXYZ(index: Int, x: Double, y: Double, z: Double): Float32BufferAttribute {
        val calcIndex = index * this.itemSize

        this.array[calcIndex + 0] = x.toFloat()
        this.array[calcIndex + 1] = y.toFloat()
        this.array[calcIndex + 2] = z.toFloat()

        return this
    }

    actual fun setXYZW(index: Int, x: Double, y: Double, z: Double, w: Double): Float32BufferAttribute {
        val calcIndex = index * this.itemSize

        this.array[calcIndex + 0] = x.toFloat()
        this.array[calcIndex + 1] = y.toFloat()
        this.array[calcIndex + 2] = z.toFloat()
        this.array[calcIndex + 3] = w.toFloat()

        return this
    }

    actual override fun clone(): Float32BufferAttribute {
        return Float32BufferAttribute(this.array, itemSize, normalized)
    }
}
