package math

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

    override fun toArray(): Array<Double> {
        return arrayOf(this.x, this.y, this.z, this.w)
    }

    override fun toMutable(): MutableVector4 {
        return MutableVector4(this.x, this.y, this.z, this.w)
    }

    open fun clone(): Vector4 {
        return Vector4(this.x, this.y, this.z, this.w)
    }
}
