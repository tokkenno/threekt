package math

@ExperimentalJsExport
@JsExport
class AxisOrder private constructor(val id: Int) {
    companion object {
        val XYZ = AxisOrder(0)
        val YZX = AxisOrder(1)
        val ZXY = AxisOrder(2)
        val XZY = AxisOrder(3)
        val YXZ = AxisOrder(4)
        val ZYX = AxisOrder(5)
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other !is AxisOrder) return false
        return this.id == other.id
    }
}
