package com.aitorgf.threekt.math

@ExperimentalJsExport
@JsExport
actual external interface Vector {
    /** The vector dimensionality */
    actual val dimension: Int

    /** Get the value in the vector [index]. */
    actual fun get(index: Int): Double

    /** Get the vector as array */
    actual fun toArray(): DoubleArray

    /** Converts the vector to mutable */
    actual fun toMutable(): MutableVector

}
