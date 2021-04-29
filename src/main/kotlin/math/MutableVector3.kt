package math


@ExperimentalJsExport
@JsExport
open class MutableVector3(
    override var x: Double = 0.0,
    override var y: Double = 0.0,
    override var z: Double = 0.0
) : Vector3(x, y, z), Vector {
    fun set(values: Array<Double>): MutableVector3 {
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

    fun setIndex(index: Int, value: Double): MutableVector3 {
        when (index) {
            0 -> this.x = value
            1 -> this.y = value
            2 -> this.z = value
            else -> throw IndexOutOfBoundsException("Invalid index <$index>. This vector has only 3 dimensions.")
        }
        return this
    }
}
