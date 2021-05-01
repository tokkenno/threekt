package math

/**
 * Represents a color
 * @property red The red color level between 0.0 and 1.0
 * @property green The green color level between 0.0 and 1.0
 * @property blue The blue color level between 0.0 and 1.0
 */
@ExperimentalJsExport
@JsExport
class Color private constructor(
    val red: Double,
    val green: Double,
    val blue: Double
) {
    companion object {
        /** Define a color from [red], [green] and [blue] color levels, represented in values from 0.0 to 1.0 */
        fun fromRGB(red: Double, green: Double, blue: Double): Color {
            return Color(
                kotlin.math.max(0.0, kotlin.math.min(1.0, red)),
                kotlin.math.max(0.0, kotlin.math.min(1.0, green)),
                kotlin.math.max(0.0, kotlin.math.min(1.0, blue))
            )
        }

        /** Define a color from [mask] number, using the lowest 12 bytes as 4-4-4 RGB values */
        @JsName("fromRGBMask")
        fun fromRGB(mask: Int): Color {
            return Color(
                (mask shr 16 and 255) / 255.0,
                (mask shr 8 and 255) / 255.0,
                (mask and 255) / 255.0
            )
        }
    }

    fun clone(): Color {
        return Color(this.red, this.green, this.blue)
    }
}
