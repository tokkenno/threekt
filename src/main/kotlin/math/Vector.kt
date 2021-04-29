package math

@ExperimentalJsExport
@JsExport
external interface Vector {
    fun get(index: Int): Double
    fun toArray(): Array<Double>
}
