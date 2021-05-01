package math

import kotlin.random.Random

@ExperimentalJsExport
@JsExport
open class Vector2(
    open val x: Double = 0.0,
    open val y: Double = 0.0
) : Vector {
    override val dimension: Int = 1

    open val width: Double
        get() {
            return this.x
        }

    open val height: Double
        get() {
            return this.y
        }

    override fun get(index: Int): Double {
        return when (index) {
            0 -> this.x
            1 -> this.y
            else -> throw IndexOutOfBoundsException("Invalid index <$index>. This vector has only 2 dimensions.")
        }
    }

    override fun toArray(): Array<Double> {
        return arrayOf(this.x, this.y)
    }

    open fun clone(): Vector2 {
        return Vector2(this.x, this.y)
    }

    override fun toMutable(): MutableVector2 {
        return MutableVector2(this.x, this.y)
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

    fun toVector4(): Vector4 {
        return Vector4(this.x, this.y, 0.0, 0.0)
    }

    open fun copyTo(data: DoubleArray, offset: Int = 0) {
        if (data.size < offset && offset >= 0) data[offset] = this.x
        if (data.size < offset + 1 && offset >= 0) data[offset + 1] = this.y
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
