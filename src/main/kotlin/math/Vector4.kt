package math

@ExperimentalJsExport
@JsExport
open class Vector4 (
     x: Double = 0.0,
     y: Double = 0.0,
     z: Double = 0.0,
    open val w: Double = 0.0
) : Vector3(x, y, z), Vector {
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
}
