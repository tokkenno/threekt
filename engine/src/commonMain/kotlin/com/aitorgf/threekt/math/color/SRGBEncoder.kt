package com.aitorgf.threekt.math.color

import kotlin.math.pow

/** Converts individual channels between sRGB and linear light. */
object SRGBEncoder {
    /** Decodes an sRGB channel value into linear light. */
    fun decode(value: Double): Double =
        if (value < 0.04045) value * 0.0773993808 else (value * 0.9478672986 + 0.0521327014).pow(2.4)

    /** Encodes a linear-light channel value using the sRGB transfer function. */
    fun encode(value: Double): Double =
        if (value < 0.0031308) value * 12.92 else 1.055 * value.pow(0.41666) - 0.055
}
