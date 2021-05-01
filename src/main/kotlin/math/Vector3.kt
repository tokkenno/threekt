package math

@ExperimentalJsExport
@JsExport
open class Vector3(
    open val x: Double = 0.0,
    open val y: Double = 0.0,
    open val z: Double = 0.0
) : Vector {
    companion object {
        val xAxis: Vector3 by lazy { Vector3(1.0, 0.0, 0.0) }
        val yAxis: Vector3 by lazy { Vector3(0.0, 1.0, 0.0) }
        val zAxis: Vector3 by lazy { Vector3(0.0, 0.0, 1.0) }
        val zero: Vector3 by lazy { Vector3(0.0, 0.0, 0.0) }
        val one: Vector3 by lazy { Vector3(1.0, 1.0, 1.0) }
    }

    override val dimension: Int = 3

    override fun toMutable(): MutableVector3 {
        return MutableVector3(this.x, this.y, this.z)
    }

    open fun clone(): Vector3 {
        return Vector3(this.x, this.y, this.z)
    }

    override fun get(index: Int): Double {
        return when (index) {
            0 -> this.x
            1 -> this.y
            2 -> this.z
            else -> throw IndexOutOfBoundsException("Invalid index <$index>. This vector has only 3 dimensions.")
        }
    }

    fun length(): Double {
        return kotlin.math.sqrt(this.lengthSq())
    }

    fun lengthSq(): Double {
        return this.x * this.x + this.y * this.y + this.z * this.z
    }

    override fun toArray(): Array<Double> {
        return arrayOf(this.x, this.y, this.z)
    }
}
