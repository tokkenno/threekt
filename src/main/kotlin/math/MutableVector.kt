package math

@ExperimentalJsExport
@JsExport
external interface MutableVector : Vector {
    fun set(values: Array<Double>): Vector
    fun setIndex(index: Int, value: Double): Vector
}
