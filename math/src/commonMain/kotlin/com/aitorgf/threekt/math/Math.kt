package com.aitorgf.threekt.math

import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

/**
 * Extends kotlin math library operations
 */
@ExperimentalJsExport
@JsExport
object Math {
    /**
     * Clamp a [value] between a [min] and [max] value
     */
    fun clamp(value: Double, min: Double, max: Double): Double {
        return kotlin.math.max(min, kotlin.math.min(max, value))
    }

    /**
     * Compute euclidian modulo of [m] % [n]
     * https://en.wikipedia.org/wiki/Modulo_operation
     */
    fun euclideanModulo(n: Double, m: Double): Double {
        return ((n % m) + m) % m;
    }
}
