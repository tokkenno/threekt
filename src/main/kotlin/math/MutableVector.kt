package math

@ExperimentalJsExport
@JsExport
external interface MutableVector : Vector {
    fun set(vararg values: Double): Vector
    fun setIndex(index: Int, value: Double): Vector
}
