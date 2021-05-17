package com.aitorgf.threekt.math

import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport
import kotlin.js.JsName

@ExperimentalJsExport
@JsExport
class MutableVector4(
    override var x: Double = 0.0,
    override var y: Double = 0.0,
    override var z: Double = 0.0,
    override var w: Double = 0.0
) : Vector4(x, y, z, w), MutableVector {
    fun set(x: Double, y: Double, z: Double, w: Double): MutableVector4 {
        this.x = x
        this.y = y
        this.z = z
        this.w = w
        return this
    }

    override fun set(vararg values: Double): MutableVector4 {
        if (values.size > 0) {
            this.x = values[0]
        }
        if (values.size > 1) {
            this.y = values[1]
        }
        if (values.size > 2) {
            this.z = values[2]
        }
        if (values.size > 3) {
            this.w = values[3]
        }
        return this
    }

    @JsName("setVector4")
    fun set(other: Vector4): MutableVector4 {
        this.x = other.x
        this.y = other.y
        this.z = other.z
        this.w = other.w
        return this
    }

    override fun setIndex(index: Int, value: Double): MutableVector4 {
        when (index) {
            0 -> this.x = value
            1 -> this.y = value
            2 -> this.z = value
            3 -> this.w = value
            else -> throw IndexOutOfBoundsException("Invalid index <$index>. This vector has only 4 dimensions.")
        }
        return this
    }

    override fun clone(): MutableVector4 {
        return MutableVector4(this.x, this.y, this.z, this.w)
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other === this) return true
        if (other !is MutableVector4) return false
        return this.x == other.x && this.y == other.y && this.z == other.z && this.w == other.w
    }

    fun add(vector: Vector4): MutableVector4 {
        this.x += vector.x
        this.y += vector.y
        this.z += vector.z
        this.w += vector.w
        return this
    }

    @JsName("addScalar")
    fun add(value: Double): MutableVector4 {
        this.x += value
        this.y += value
        this.z += value
        this.w += value
        return this
    }

    operator fun plusAssign(vector: Vector4) {
        this.add(vector)
    }

    operator fun plus(vector: Vector4): MutableVector4 {
        return this.clone().add(vector)
    }

    fun addScaledVector(vector: Vector4, scale: Double): MutableVector4 {
        this.x += vector.x * scale
        this.y += vector.y * scale
        this.z += vector.z * scale
        this.w += vector.w * scale
        return this
    }

    fun sub(vector: Vector4): MutableVector4 {
        this.x -= vector.x
        this.y -= vector.y
        this.z -= vector.z
        this.w -= vector.w
        return this
    }

    @JsName("subScalar")
    fun sub(value: Double): MutableVector4 {
        this.x -= value
        this.y -= value
        this.z -= value
        this.w -= value
        return this
    }

    operator fun minusAssign(vector: Vector4) {
        this.sub(vector)
    }

    operator fun minus(vector: Vector4): MutableVector4 {
        return this.clone().sub(vector)
    }

    fun multiply(vector: Vector4): MutableVector4 {
        this.x *= vector.x
        this.y *= vector.y
        this.z *= vector.z
        this.w *= vector.w
        return this
    }

    @JsName("multiplyScalar")
    fun multiply(value: Double): MutableVector4 {
        this.x *= value
        this.y *= value
        this.z *= value
        this.w *= value
        return this
    }

    operator fun timesAssign(vector: Vector4) {
        this.times(vector)
    }

    operator fun times(vector: Vector4): MutableVector4 {
        return this.clone().multiply(vector)
    }

    fun divide(vector: Vector4): MutableVector4 {
        this.x /= vector.x
        this.y /= vector.y
        this.z /= vector.z
        this.w /= vector.w
        return this
    }

    @JsName("divideScalar")
    fun divide(value: Double): MutableVector4 {
        this.x /= value
        this.y /= value
        this.z /= value
        this.w /= value
        return this
    }

    operator fun divAssign(vector: Vector4) {
        this.divide(vector)
    }

    operator fun div(vector: Vector4): MutableVector4 {
        return this.clone().divide(vector)
    }

    fun floor(): MutableVector4 {
        x = kotlin.math.floor(x)
        y = kotlin.math.floor(y)
        z = kotlin.math.floor(z)
        w = kotlin.math.floor(w)
        return this
    }
}
