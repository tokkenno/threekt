package math

@ExperimentalJsExport
@JsExport
object Utils {
    fun clamp(value: Double, min: Double, max: Double): Double {
        return kotlin.math.max(min, kotlin.math.min(max, value))
    }
}
