package com.aitorgf.threekt.math

import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport
import kotlin.js.JsName

/**
 * Represents a color
 * @property red The red color level between 0.0 and 1.0
 * @property green The green color level between 0.0 and 1.0
 * @property blue The blue color level between 0.0 and 1.0
 */
@ExperimentalJsExport
@JsExport
class Color private constructor(
     red: Double,
     green: Double,
     blue: Double,
     alpha: Double = 1.0
) {
    val red: Double = Math.clamp(red , 0.0, 1.0)
    val green: Double = Math.clamp(green , 0.0, 1.0)
    val blue: Double = Math.clamp(blue , 0.0, 1.0)
    val alpha: Double = Math.clamp(alpha , 0.0, 1.0)

    @JsName("plusScalar")
    operator fun plus(scalar: Double): Color {
        return Color(
            Math.clamp(this.red + scalar, 0.0, 1.0),
            Math.clamp(this.green + scalar, 0.0, 1.0),
            Math.clamp(this.blue + scalar, 0.0, 1.0),
            this.alpha
        )
    }

    operator fun plus(color: Color): Color {
        return Color(
            Math.clamp(this.red + color.red, 0.0, 1.0),
            Math.clamp(this.green + color.green, 0.0, 1.0),
            Math.clamp(this.blue + color.blue, 0.0, 1.0),
            (this.alpha + color.alpha) / 2.0
        )
    }

    @JsName("minusScalar")
    operator fun minus(scalar: Double): Color {
        return Color(
            Math.clamp(this.red - scalar, 0.0, 1.0),
            Math.clamp(this.green - scalar, 0.0, 1.0),
            Math.clamp(this.blue - scalar, 0.0, 1.0),
            this.alpha
        )
    }

    operator fun minus(color: Color): Color {
        return Color(
            Math.clamp(this.red - color.red, 0.0, 1.0),
            Math.clamp(this.green - color.green, 0.0, 1.0),
            Math.clamp(this.blue - color.blue, 0.0, 1.0),
            this.alpha
        )
    }

    @JsName("divScalar")
    operator fun div(scalar: Double): Color {
        return Color(
            Math.clamp(this.red / scalar, 0.0, 1.0),
            Math.clamp(this.green / scalar, 0.0, 1.0),
            Math.clamp(this.blue / scalar, 0.0, 1.0),
            this.alpha
        )
    }

    operator fun div(color: Color): Color {
        return Color(
            Math.clamp(this.red / color.red, 0.0, 1.0),
            Math.clamp(this.green / color.green, 0.0, 1.0),
            Math.clamp(this.blue / color.blue, 0.0, 1.0),
            this.alpha
        )
    }

    @JsName("timesScalar")
    operator fun times(scalar: Double): Color {
        return Color(
            Math.clamp(this.red * scalar, 0.0, 1.0),
            Math.clamp(this.green * scalar, 0.0, 1.0),
            Math.clamp(this.blue * scalar, 0.0, 1.0),
            this.alpha
        )
    }

    operator fun times(color: Color): Color {
        return Color(
            Math.clamp(this.red * color.red, 0.0, 1.0),
            Math.clamp(this.green * color.green, 0.0, 1.0),
            Math.clamp(this.blue * color.blue, 0.0, 1.0),
            this.alpha
        )
    }

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

        fun fromHSL(h: Double, s: Double, l: Double): Color {
            // h,s,l ranges are in 0.0 - 1.0
            val hn = Math.euclideanModulo(h, 1.0);
            val sn = Math.clamp(s, 0.0, 1.0);
            val ln = Math.clamp(l, 0.0, 1.0);

            return if (sn == 0.0) {
                Color(ln, ln, ln)
            } else {
                val p = if (ln <= 0.5) {
                    ln * (1 + sn)
                } else ln + sn - (ln * sn);
                val q = (2 * ln) - p;

                Color(
                    hueToRGB(q, p, hn + 1 / 3),
                    hueToRGB(q, p, hn),
                    hueToRGB(q, p, hn - 1 / 3)
                )
            }
        }

        private fun hueToRGB(p: Double, q: Double, t: Double): Double {
            var tn = t
            if (tn < 0) tn += 1;
            if (tn > 1) tn -= 1;
            if (tn < 1 / 6) return p + (q - p) * 6 * tn;
            if (tn < 1 / 2) return q;
            if (tn < 2 / 3) return p + (q - p) * 6 * (2 / 3 - tn);
            return p;
        }
    }

    fun clone(): Color {
        return Color(this.red, this.green, this.blue)
    }
}
