package com.aitorgf.threekt.math

import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

@ExperimentalJsExport
@JsExport
open class Vector4(
    open val x: Double = 0.0,
    open val y: Double = 0.0,
    open val z: Double = 0.0,
    open val w: Double = 0.0
) : Vector {
    override val dimension: Int
        get() = TODO("Not yet implemented")

    override fun get(index: Int): Double {
        return when (index) {
            0 -> this.x
            1 -> this.y
            2 -> this.z
            3 -> this.w
            else -> throw IndexOutOfBoundsException("Invalid index <$index>. This vector has only 4 dimensions.")
        }
    }

    override fun toArray(): DoubleArray {
        return doubleArrayOf(this.x, this.y, this.z, this.w)
    }

    override fun toMutable(): MutableVector4 {
        return MutableVector4(this.x, this.y, this.z, this.w)
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other === this) return true
        if (other !is Vector4) return false
        return this.x == other.x && this.y == other.y && this.z == other.z && this.w == other.w
    }

    override fun hashCode(): Int {
        return 31 * (31 * (31 * this.x.hashCode() + this.y.hashCode()) + this.z.hashCode()) + this.w.hashCode()
    }

    override fun toString(): String {
        return "Vector4 { x: ${this.x}, y: ${this.y}, z: ${this.z}, w: ${this.w} }";
    }

    open fun clone(): Vector4 {
        return Vector4(this.x, this.y, this.z, this.w)
    }
}
