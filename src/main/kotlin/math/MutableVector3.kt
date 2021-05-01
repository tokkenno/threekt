package math


@ExperimentalJsExport
@JsExport
open class MutableVector3(
    override var x: Double = 0.0,
    override var y: Double = 0.0,
    override var z: Double = 0.0
) : Vector3(x, y, z), MutableVector {
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

    override fun clone(): MutableVector3 {
        return MutableVector3(this.x, this.y, this.z)
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other === this) return true
        if (other !is MutableVector3) return false
        return this.x == other.x && this.y == other.y && this.z == other.z
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

    open fun normalize(): MutableVector3 {
        val length = this.length()
        return this.divide(if (length != 0.0) length else 1.0)
    }
}
