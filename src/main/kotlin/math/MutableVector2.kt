package math

import kotlin.random.Random


@ExperimentalJsExport
@JsExport
open class MutableVector2(
    override var x: Double = 0.0,
    override var y: Double = 0.0
) : Vector2(x, y), MutableVector {
    override var width: Double
        get() {
            return this.x
        }
        set(value) {
            this.x = value
        }

    override var height: Double
        get() {
            return this.y
        }
        set(value) {
            this.y = value
        }

    override fun set(vararg values: Double): MutableVector2 {
        if (values.size > 0) {
            this.x = values[0]
        }
        if (values.size > 1) {
            this.y = values[1]
        }
        return this
    }

    override fun setIndex(index: Int, value: Double): MutableVector2 {
        when (index) {
            0 -> this.x = value
            1 -> this.y = value
            else -> throw IndexOutOfBoundsException("Invalid index <$index>. This vector has only 2 dimensions.")
        }
        return this
    }

    @JsName("setVector2")
    fun set(other: Vector2): MutableVector2 {
        this.x = other.x
        this.y = other.y
        return this
    }

    override fun clone(): MutableVector2 {
        return MutableVector2(this.x, this.y)
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other === this) return true
        if (other !is MutableVector2) return false
        return this.x == other.x && this.y == other.y
    }

    override fun copyTo(data: DoubleArray, offset: Int) {
        if (data.size < offset && offset >= 0) data[offset] = this.x
        if (data.size < offset + 1 && offset >= 0) data[offset + 1] = this.y
    }

    fun add(vector: Vector2): MutableVector2 {
        this.x += vector.x
        this.y += vector.y
        return this
    }

    @JsName("addScalar")
    fun add(value: Double): MutableVector2 {
        this.x += value
        this.y += value
        return this
    }

    operator fun plusAssign(vector: Vector2) {
        this.add(vector)
    }

    operator fun plus(vector: Vector2): MutableVector2 {
        return this.clone().add(vector)
    }

    fun addScaledVector(vector: Vector2, scale: Double): MutableVector2 {
        this.x += vector.x * scale
        this.y += vector.y * scale
        return this
    }

    fun sub(vector: Vector2): MutableVector2 {
        this.x -= vector.x
        this.y -= vector.y
        return this
    }

    @JsName("subScalar")
    fun sub(value: Double): MutableVector2 {
        this.x -= value
        this.y -= value
        return this
    }

    operator fun minusAssign(vector: Vector2) {
        this.sub(vector)
    }

    operator fun minus(vector: Vector2): MutableVector2 {
        return this.clone().sub(vector)
    }

    fun multiply(vector: Vector2): MutableVector2 {
        this.x *= vector.x
        this.y *= vector.y
        return this
    }

    @JsName("multiplyScalar")
    fun multiply(value: Double): MutableVector2 {
        this.x *= value
        this.y *= value
        return this
    }

    operator fun timesAssign(vector: Vector2) {
        this.times(vector)
    }

    operator fun times(vector: Vector2): MutableVector2 {
        return this.clone().multiply(vector)
    }

    fun divide(vector: Vector2): MutableVector2 {
        this.x /= vector.x
        this.y /= vector.y
        return this
    }

    @JsName("divideScalar")
    fun divide(value: Double): MutableVector2 {
        this.x /= value
        this.y /= value
        return this
    }

    operator fun divAssign(vector: Vector2) {
        this.divide(vector)
    }

    operator fun div(vector: Vector2): MutableVector2 {
        return this.clone().divide(vector)
    }

    fun min(vector: Vector2): MutableVector2 {
        this.x = kotlin.math.min(vector.x, this.x)
        this.y = kotlin.math.min(vector.y, this.y)
        return this
    }

    fun max(vector: Vector2): MutableVector2 {
        this.x = kotlin.math.max(vector.x, this.x)
        this.y = kotlin.math.max(vector.y, this.y)
        return this
    }

    fun clamp(min: Vector2, max: Vector2): MutableVector2 {
        this.x = kotlin.math.max(min.x, kotlin.math.min(max.x, this.x))
        this.y = kotlin.math.max(min.y, kotlin.math.min(max.y, this.y))
        return this
    }

    @JsName("clampScalar")
    fun clamp(min: Double, max: Double): MutableVector2 {
        this.x = kotlin.math.max(min, kotlin.math.min(max, this.x))
        this.y = kotlin.math.max(min, kotlin.math.min(max, this.y))
        return this
    }

    fun clampLength(min: Double, max: Double): MutableVector2 {
        val length = this.length()
        return this.normalize().multiply(kotlin.math.max(min, kotlin.math.min(max, length)))
    }

    fun floor(): MutableVector2 {
        this.x = kotlin.math.floor(this.x)
        this.y = kotlin.math.floor(this.y)
        return this
    }

    fun ceil(): MutableVector2 {
        this.x = kotlin.math.ceil(this.x)
        this.y = kotlin.math.ceil(this.y)
        return this
    }

    fun round(): MutableVector2 {
        this.x = kotlin.math.round(this.x)
        this.y = kotlin.math.round(this.y)
        return this
    }

    fun roundToZero(): MutableVector2 {
        this.x = if (this.x < 0) kotlin.math.ceil(this.x) else kotlin.math.floor(this.x)
        this.y = if (this.y < 0) kotlin.math.ceil(this.y) else kotlin.math.floor(this.y)
        return this
    }

    fun negate(): MutableVector2 {
        this.x = -this.x
        this.y = -this.y
        return this
    }

    fun normalize(): MutableVector2 {
        val length = this.length()
        return this.divide(if (length != 0.0) length else 1.0)
    }

    fun setLength(length: Double): MutableVector2 {
        return this.normalize().multiply(length)
    }

    fun lerp(vector: Vector2, alpha: Double): MutableVector2 {
        this.x += (vector.x - this.x) * alpha
        this.y += (vector.y - this.y) * alpha
        return this
    }

    fun lerpVectors(v1: Vector2, v2: Vector2, alpha: Double): MutableVector2 {
        this.set(v1.toMutable().lerp(v2, alpha))
        return this
    }

    fun rotate(rotationCenter: Vector2, angle: Double): MutableVector2 {
        val cos = kotlin.math.cos(angle)
        val sin = kotlin.math.sin(angle)

        val cx = this.x - rotationCenter.x
        val cy = this.y - rotationCenter.y

        this.x = cx * cos - cy * sin + rotationCenter.x
        this.y = cx * sin + cy * cos + rotationCenter.y

        return this
    }

    companion object {
        fun random(): MutableVector2 {
            return MutableVector2(Random.nextDouble(), Random.nextDouble())
        }

        fun fromArray(data: DoubleArray, offset: Int = 0): MutableVector2 {
            val im = Vector2.fromArray(data, offset)
            return MutableVector2(im.x, im.y)
        }
    }
}

