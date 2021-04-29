package math

@ExperimentalJsExport
@JsExport
class MutableVector4(
    override var x: Double = 0.0,
    override var y: Double = 0.0,
    override var z: Double = 0.0,
    override var w: Double = 0.0
) : Vector4(x, y, z, w), Vector {
    fun set(values: Array<Double>): MutableVector4 {
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

    fun setIndex(index: Int, value: Double): MutableVector4 {
        when (index) {
            0 -> this.x = value
            1 -> this.y = value
            2 -> this.z = value
            3 -> this.w = value
            else -> throw IndexOutOfBoundsException("Invalid index <$index>. This vector has only 4 dimensions.")
        }
        return this
    }
}
