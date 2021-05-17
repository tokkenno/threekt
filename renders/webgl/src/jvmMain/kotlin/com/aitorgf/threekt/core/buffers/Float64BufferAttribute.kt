package com.aitorgf.threekt.core.buffers

import com.aitorgf.threekt.core.BufferAttribute
import com.aitorgf.threekt.math.*
import kotlin.js.ExperimentalJsExport

actual class Float64BufferAttribute actual constructor(
    itemSize: Int,
    normalized: Boolean
): BufferAttribute(itemSize, normalized) {
    var array: DoubleArray = DoubleArray(0)

    actual constructor(
        data: DoubleArray,
        itemSize: Int,
        normalized: Boolean
    ): this(itemSize, normalized) {
        this.array = data
    }

    actual fun set(source: Float64BufferAttribute): Float64BufferAttribute {
        this.name = source.name
        this.array = source.array.copyOf()
        this.itemSize = source.itemSize
        this.normalized = source.normalized
        this.usage = source.usage

        return this
    }

    actual fun copyAt(index1: Int, attribute: Float64BufferAttribute, index2: Int): Float64BufferAttribute {
        val calcIndex1 = index1 * this.itemSize
        val calcIndex2 = index2 * attribute.itemSize

        for (i in 0..this.itemSize - 1) {
            this.array[calcIndex1 + i] = attribute.array[calcIndex2 + i]
        }

        return this
    }

    actual fun set(arr: DoubleArray): Float64BufferAttribute {
        this.array = arr.copyOf()
        return this
    }

    @ExperimentalJsExport
    actual fun set(colors: Array<Color>): Float64BufferAttribute {
        return this
    }

    @ExperimentalJsExport
    actual fun set(vectors: Array<Vector2>): Float64BufferAttribute {
        return this
    }

    @ExperimentalJsExport
    actual fun set(vectors: Array<Vector3>): Float64BufferAttribute {
        return this
    }

    @ExperimentalJsExport
    actual fun set(vectors: Array<Vector4>): Float64BufferAttribute {
        return this
    }

    @ExperimentalJsExport
    actual override fun apply(m: Matrix3): Float64BufferAttribute {
        return this
    }

    @ExperimentalJsExport
    actual override fun apply(m: Matrix4): Float64BufferAttribute {
        return this
    }

    @ExperimentalJsExport
    actual override fun applyNormalMatrix(m: Matrix3): Float64BufferAttribute {
        return this
    }

    @ExperimentalJsExport
    actual override fun transformDirection(m: Matrix4): Float64BufferAttribute {
        return this
    }

    actual fun set(value: Double, offset: Int): Float64BufferAttribute {
        this.array[offset] = value
        return this
    }

    actual fun getX(index: Int): Double {
        return this.array[index * this.itemSize]
    }

    actual fun setX(index: Int, x: Double): Float64BufferAttribute {
        this.array[index * this.itemSize] = x

        return this
    }

    actual fun getY(index: Int): Double {
        return this.array[index * this.itemSize + 1]
    }

    actual fun setY(index: Int, y: Double): Float64BufferAttribute {
        this.array[index * this.itemSize + 1] = y

        return this
    }

    actual fun getZ(index: Int): Double {
        return this.array[index * this.itemSize + 2]
    }

    actual fun setZ(index: Int, z: Double): Float64BufferAttribute {
        this.array[index * this.itemSize + 2] = z

        return this
    }

    actual fun getW(index: Int): Double {
        return this.array[index * this.itemSize + 3]
    }

    actual fun setW(index: Int, w: Double): Float64BufferAttribute {
        this.array[index * this.itemSize + 3] = w

        return this
    }

    actual fun setXY(index: Int, x: Double, y: Double): Float64BufferAttribute {
        val calcIndex = index * this.itemSize

        this.array[calcIndex + 0] = x
        this.array[calcIndex + 1] = y

        return this
    }

    actual fun setXYZ(index: Int, x: Double, y: Double, z: Double): Float64BufferAttribute {
        val calcIndex = index * this.itemSize

        this.array[calcIndex + 0] = x
        this.array[calcIndex + 1] = y
        this.array[calcIndex + 2] = z

        return this
    }

    actual fun setXYZW(index: Int, x: Double, y: Double, z: Double, w: Double): Float64BufferAttribute {
        val calcIndex = index * this.itemSize

        this.array[calcIndex + 0] = x
        this.array[calcIndex + 1] = y
        this.array[calcIndex + 2] = z
        this.array[calcIndex + 3] = w

        return this
    }

    actual override fun clone(): Float64BufferAttribute {
        return Float64BufferAttribute(this.array, itemSize, normalized)
    }
}
