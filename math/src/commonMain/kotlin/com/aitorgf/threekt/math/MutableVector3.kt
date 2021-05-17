package com.aitorgf.threekt.math


import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport
import kotlin.js.JsName

@ExperimentalJsExport
@JsExport
open class MutableVector3(
    override var x: Double = 0.0,
    override var y: Double = 0.0,
    override var z: Double = 0.0
) : Vector3(x, y, z), MutableVector {
    fun set(x: Double, y: Double, z: Double): MutableVector3 {
        this.x = x
        this.y = y
        this.z = z
        return this
    }

    override fun set(vararg values: Double): MutableVector3 {
        if (values.size > 0) {
            this.x = values[0]
        }
        if (values.size > 1) {
            this.y = values[1]
        }
        if (values.size > 2) {
            this.z = values[2]
        }
        return this
    }

    override fun setIndex(index: Int, value: Double): MutableVector3 {
        when (index) {
            0 -> this.x = value
            1 -> this.y = value
            2 -> this.z = value
            else -> throw IndexOutOfBoundsException("Invalid index <$index>. This vector has only 3 dimensions.")
        }
        return this
    }

    @JsName("setVector3")
    fun set(other: Vector3): MutableVector3 {
        this.x = other.x
        this.y = other.y
        this.z = other.z
        return this
    }

    fun setFromMatrixPosition(m: Matrix4): MutableVector3 {
        this.x = m.elements[12]
        this.y = m.elements[13]
        this.z = m.elements[14]
        return this
    }

    fun setFromMatrixScale(m: Matrix4): MutableVector3 {
        val sx = this.setFromMatrixColumn(m, 0).length()
        val sy = this.setFromMatrixColumn(m, 1).length()
        val sz = this.setFromMatrixColumn(m, 2).length()

        this.x = sx
        this.y = sy
        this.z = sz

        return this
    }

    fun setFromMatrixColumn(m: Matrix4, index: Int): MutableVector3 {
        return this.fromArray(m.elements, index * 4)
    }

    @JsName("applyMatrix3")
    fun apply(m: Matrix3): MutableVector3 {
        val x = this.x
        val y = this.y
        val z = this.z

        this.x = m.elements[0] * x + m.elements[3] * y + m.elements[6] * z
        this.y = m.elements[1] * x + m.elements[4] * y + m.elements[7] * z
        this.z = m.elements[2] * x + m.elements[5] * y + m.elements[8] * z

        return this
    }

    @JsName("applyMatrix4")
    fun apply(m: Matrix4): MutableVector3 {
        val x = this.x
        val y = this.y
        val z = this.z

        val w = 1 / (m.elements[3] * x + m.elements[7] * y + m.elements[11] * z + m.elements[15])

        this.x = (m.elements[0] * x + m.elements[4] * y + m.elements[8] * z + m.elements[12]) * w
        this.y = (m.elements[1] * x + m.elements[5] * y + m.elements[9] * z + m.elements[13]) * w
        this.z = (m.elements[2] * x + m.elements[6] * y + m.elements[10] * z + m.elements[14]) * w

        return this
    }

    fun applyNormalMatrix(m: Matrix3): MutableVector3 {
        return this.apply(m).normalize()
    }

    internal fun fromArray(array: DoubleArray, offset: Int = 0): MutableVector3 {
        this.x = array[offset]
        this.y = array[offset + 1]
        this.z = array[offset + 2]
        return this
    }

    override fun clone(): MutableVector3 {
        return MutableVector3(this.x, this.y, this.z)
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other === this) return true
        if (other !is MutableVector3) return false
        return this.x == other.x && this.y == other.y && this.z == other.z
    }

    fun min(v: Vector3): MutableVector3 {
        this.x = kotlin.math.min(this.x, v.x)
        this.y = kotlin.math.min(this.y, v.y)
        this.z = kotlin.math.min(this.z, v.z)
        return this
    }

    fun max(v: Vector3): MutableVector3 {
        this.x = kotlin.math.max(this.x, v.x)
        this.y = kotlin.math.max(this.y, v.y)
        this.z = kotlin.math.max(this.z, v.z)
        return this
    }

    fun add(vector: Vector3): MutableVector3 {
        this.x += vector.x
        this.y += vector.y
        this.z += vector.z
        return this
    }

    @JsName("addScalar")
    fun add(value: Double): MutableVector3 {
        this.x += value
        this.y += value
        this.z += value
        return this
    }

    @JsName("addVectors")
    fun add(a: Vector3, b: Vector3): MutableVector3 {
        this.x = a.x + b.x
        this.y = a.y + b.y
        this.z = a.z + b.z
        return this
    }

    operator fun plusAssign(vector: Vector3) {
        this.add(vector)
    }

    operator fun plus(vector: Vector3): MutableVector3 {
        return this.clone().add(vector)
    }

    fun addScaledVector(vector: Vector3, scale: Double): MutableVector3 {
        this.x += vector.x * scale
        this.y += vector.y * scale
        this.z += vector.z * scale
        return this
    }

    fun sub(vector: Vector3): MutableVector3 {
        this.x -= vector.x
        this.y -= vector.y
        this.z -= vector.z
        return this
    }

    @JsName("subScalar")
    fun sub(value: Double): MutableVector3 {
        this.x -= value
        this.y -= value
        this.z -= value
        return this
    }

    @JsName("subVectors")
    fun sub(a: Vector3, b: Vector3): MutableVector3 {
        this.x = a.x - b.x
        this.y = a.y - b.y
        this.z = a.z - b.z
        return this
    }

    operator fun minusAssign(vector: Vector3) {
        this.sub(vector)
    }

    operator fun minus(vector: Vector3): MutableVector3 {
        return this.clone().sub(vector)
    }

    fun multiply(vector: Vector3): MutableVector3 {
        this.x *= vector.x
        this.y *= vector.y
        this.z *= vector.z
        return this
    }

    @JsName("multiplyScalar")
    fun multiply(value: Double): MutableVector3 {
        this.x *= value
        this.y *= value
        this.z *= value
        return this
    }

    operator fun timesAssign(vector: Vector3) {
        this.times(vector)
    }

    operator fun times(vector: Vector3): MutableVector3 {
        return this.clone().multiply(vector)
    }

    fun divide(vector: Vector3): MutableVector3 {
        this.x /= vector.x
        this.y /= vector.y
        this.z /= vector.z
        return this
    }

    @JsName("divideScalar")
    fun divide(value: Double): MutableVector3 {
        this.x /= value
        this.y /= value
        this.z /= value
        return this
    }

    operator fun divAssign(vector: Vector3) {
        this.divide(vector)
    }

    operator fun div(vector: Vector3): MutableVector3 {
        return this.clone().divide(vector)
    }

    fun negate(): MutableVector3 {
        x = -x
        y = -y
        z = -z
        return this
    }

    fun cross(v: Vector3): MutableVector3 {
        return this.cross(this, v)
    }

    @JsName("crossVectors")
    fun cross(a: Vector3, b: Vector3): MutableVector3 {
        this.x = a.y * b.z - a.z * b.y
        this.y = a.z * b.x - a.x * b.z
        this.z = a.x * b.y - a.y * b.x

        return this
    }

    fun clamp(min: Vector3, max: Vector3): MutableVector3 {
        // assumes min < max, componentwise
        this.x = kotlin.math.max(min.x, kotlin.math.min(max.x, this.x))
        this.y = kotlin.math.max(min.y, kotlin.math.min(max.y, this.y))
        this.z = kotlin.math.max(min.z, kotlin.math.min(max.z, this.z))
        return this
    }

    @JsName("clampScalar")
    fun clamp(minVal: Double, maxVal: Double): MutableVector3 {
        this.x = kotlin.math.max(minVal, kotlin.math.min(maxVal, this.x))
        this.y = kotlin.math.max(minVal, kotlin.math.min(maxVal, this.y))
        this.z = kotlin.math.max(minVal, kotlin.math.min(maxVal, this.z))
        return this
    }

    fun clampLength(min: Double, max: Double): MutableVector3 {
        var length = this.length()
        if (length == 0.0) {
            length = 1.0
        }
        return this.divide(length).multiply(kotlin.math.max(min, kotlin.math.min(max, length)))
    }

    open fun normalize(): MutableVector3 {
        val length = this.length()
        return this.divide(if (length != 0.0) length else 1.0)
    }

    /**
     * NOTE: vector interpreted as a direction
     * @param m affine matrix
     */
    fun transformDirection(m: Matrix4): MutableVector3 {
        val x = this.x
        val y = this.y
        val z = this.z

        this.x = m.elements[0] * x + m.elements[4] * y + m.elements[8] * z
        this.y = m.elements[1] * x + m.elements[5] * y + m.elements[9] * z
        this.z = m.elements[2] * x + m.elements[6] * y + m.elements[10] * z

        return this.normalize()
    }
}
