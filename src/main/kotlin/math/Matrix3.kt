package math

@ExperimentalJsExport
@JsExport
open class Matrix3: Matrix {
    open val elements: Array<Double> = arrayOf(
        1.0, 0.0, 0.0,
        0.0, 1.0, 0.0,
        0.0, 0.0, 1.0,
    )
}
