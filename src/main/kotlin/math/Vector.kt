package math

@ExperimentalJsExport
@JsExport
external interface Vector {
    val dimension: Int
    fun get(index: Int): Double
    fun toArray(): Array<Double>
    fun toMutable(): MutableVector
}
