package math

import kotlin.random.Random


class Vector2(
    var x: Double = 0.0,
    var y: Double = 0.0
) {
    constructor(x: Int, y: Int): this(x.toDouble(), y.toDouble())
    constructor(x: Double, y: Int): this(x, y.toDouble())
    constructor(x: Int, y: Double): this(x.toDouble(), y)

    var width: Double
        get() {
            return this.x
        }
        set(value) {
            this.x = value
        }

    var height: Double
        get() {
            return this.y
        }
        set(value) {
            this.y = value
        }

    fun set(x: Double, y: Double) {
        this.x = x
        this.y = y
    }

    fun set(value: Double) {
        this.x = value
        this.y = value
    }

    fun set(vector: Vector2) {
        this.x = vector.x
        this.y = vector.y
    }

    fun clone(): Vector2 {
        return Vector2(this.x, this.y)
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other === this) return true
        if (other !is Vector2) return false
        return this.x == other.x && this.y == other.y
    }

    override fun hashCode(): Int {
        return 31 * this.x.hashCode() + this.y.hashCode()
    }

    override fun toString(): String {
        return "Vector { x: $x, y: $y }"
    }

    fun toVector3(): Vector3 {
        return Vector3(this.x, this.y, 0.0)
    }

    fun copyTo(data: DoubleArray, offset: Int = 0) {
        if (data.size < offset && offset >= 0) data[offset] = this.x
        if (data.size < offset + 1 && offset >= 0) data[offset + 1] = this.y
    }

    fun add(vector: Vector2): Vector2 {
        this.x += vector.x
        this.y += vector.y
        return this
    }

    fun add(value: Double): Vector2 {
        this.x += value
        this.y += value
        return this
    }

    operator fun plusAssign(vector: Vector2) {
        this.add(vector)
    }

    operator fun plusAssign(value: Double) {
        this.add(value)
    }

    operator fun plus(vector: Vector2): Vector2 {
        return this.clone().add(vector)
    }

    operator fun plus(value: Double): Vector2 {
        return this.clone().add(value)
    }

    fun addScaledVector(vector: Vector2, scale: Double): Vector2 {
        this.x += vector.x * scale
        this.y += vector.y * scale
        return this
    }

    fun sub(vector: Vector2): Vector2 {
        this.x -= vector.x
        this.y -= vector.y
        return this
    }

    fun sub(value: Double): Vector2 {
        this.x -= value
        this.y -= value
        return this
    }

    operator fun minusAssign(vector: Vector2) {
        this.sub(vector)
    }

    operator fun minusAssign(value: Double) {
        this.sub(value)
    }

    operator fun minus(vector: Vector2): Vector2 {
        return this.clone().sub(vector)
    }

    operator fun minus(value: Double): Vector2 {
        return this.clone().sub(value)
    }

    fun multiply(vector: Vector2): Vector2 {
        this.x *= vector.x
        this.y *= vector.y
        return this
    }

    fun multiply(value: Double): Vector2 {
        this.x *= value
        this.y *= value
        return this
    }

    operator fun timesAssign(vector: Vector2) {
        this.times(vector)
    }

    operator fun timesAssign(value: Double) {
        this.times(value)
    }

    operator fun times(vector: Vector2): Vector2 {
        return this.clone().multiply(vector)
    }

    operator fun times(value: Double): Vector2 {
        return this.clone().multiply(value)
    }

    fun divide(vector: Vector2): Vector2 {
        this.x /= vector.x
        this.y /= vector.y
        return this
    }

    fun divide(value: Double): Vector2 {
        this.x /= value
        this.y /= value
        return this
    }

    operator fun divAssign(vector: Vector2) {
        this.divide(vector)
    }

    operator fun divAssign(value: Double) {
        this.divide(value)
    }

    operator fun div(vector: Vector2): Vector2 {
        return this.clone().divide(vector)
    }

    operator fun div(value: Double): Vector2 {
        return this.clone().divide(value)
    }

    fun min(vector: Vector2): Vector2 {
        this.x = kotlin.math.min(vector.x, this.x)
        this.y = kotlin.math.min(vector.y, this.y)
        return this
    }

    fun max(vector: Vector2): Vector2 {
        this.x = kotlin.math.max(vector.x, this.x)
        this.y = kotlin.math.max(vector.y, this.y)
        return this
    }

    fun clamp(min: Vector2, max: Vector2): Vector2 {
        this.x = kotlin.math.max(min.x, kotlin.math.min(max.x, this.x))
        this.y = kotlin.math.max(min.y, kotlin.math.min(max.y, this.y))
        return this
    }

    fun clamp(min: Double, max: Double): Vector2 {
        this.x = kotlin.math.max(min, kotlin.math.min(max, this.x))
        this.y = kotlin.math.max(min, kotlin.math.min(max, this.y))
        return this
    }

    fun clampLength(min: Double, max: Double): Vector2 {
        val length = this.length()
        return this.normalize().multiply(kotlin.math.max(min, kotlin.math.min(max, length)))
    }

    fun floor(): Vector2 {
        this.x = kotlin.math.floor(this.x)
        this.y = kotlin.math.floor(this.y)
        return this
    }

    fun ceil(): Vector2 {
        this.x = kotlin.math.ceil(this.x)
        this.y = kotlin.math.ceil(this.y)
        return this
    }

    fun round(): Vector2 {
        this.x = kotlin.math.round(this.x)
        this.y = kotlin.math.round(this.y)
        return this
    }

    fun roundToZero(): Vector2 {
        this.x = if (this.x < 0) kotlin.math.ceil(this.x) else kotlin.math.floor(this.x)
        this.y = if (this.y < 0) kotlin.math.ceil(this.y) else kotlin.math.floor(this.y)
        return this
    }

    fun negate(): Vector2 {
        this.x = -this.x
        this.y = -this.y
        return this
    }

    fun normalize(): Vector2 {
        val length = this.length()
        return this.divide(if (length != 0.0) length else 1.0)
    }

    fun dot(vector: Vector2): Double {
        return this.x * vector.x + this.y * vector.y
    }

    fun cross(vector: Vector2): Double {
        return this.x * vector.y - this.y * vector.y
    }

    fun length(): Double {
        return kotlin.math.sqrt(this.lengthSq())
    }

    fun setLength(length: Double): Vector2 {
        return this.normalize().multiply(length)
    }

    fun lengthSq(): Double {
        return this.x * this.x + this.y * this.y
    }

    fun manhattanLength(): Double {
        return kotlin.math.abs(this.x) + kotlin.math.abs(this.y)
    }

    fun angleFromX(): Double {
        return kotlin.math.atan2(-this.y, -this.x) + kotlin.math.PI
    }

    fun distanceTo(vector: Vector2): Double {
        return kotlin.math.sqrt(this.distanceToSquared(vector))
    }

    fun distanceToSquared(vector: Vector2): Double {
        val dx = this.x - vector.x
        val dy = this.y - vector.y
        return dx * dx + dy * dy;
    }

    fun manhattanDistanceTo(vector: Vector2): Double {
        return kotlin.math.abs(this.x - vector.x) + kotlin.math.abs(this.y - vector.y)
    }

    fun lerp(vector: Vector2, alpha: Double): Vector2 {
        this.x += (vector.x - this.x) * alpha
        this.y += (vector.y - this.y) * alpha
        return this
    }

    fun lerpVectors(v1: Vector2, v2: Vector2, alpha: Double): Vector2 {
        this.set(v1.clone().lerp(v2, alpha))
        return this;
    }

    fun rotate(rotationCenter: Vector2, angle: Double): Vector2 {
        val cos = kotlin.math.cos(angle)
        val sin = kotlin.math.sin(angle)

        val cx = this.x - rotationCenter.x
        val cy = this.y - rotationCenter.y

        this.x = cx * cos - cy * sin + rotationCenter.x;
        this.y = cx * sin + cy * cos + rotationCenter.y;

        return this;
    }

    companion object {
        fun random(): Vector2 {
            return Vector2(Random.nextDouble(), Random.nextDouble())
        }

        fun fromArray(data: DoubleArray, offset: Int = 0): Vector2 {
            return Vector2(
                if (data.size < offset && offset >= 0) data[offset] else 0.0,
                if (data.size < offset + 1 && offset >= 0) data[offset + 1] else 0.0
            )
        }
    }
}