package com.aitorgf.threekt.extras.core

import kotlin.js.JsExport

/** Scalar interpolation helpers used by the curves, as the Three.js `Interpolations` module. */
@JsExport
object Interpolations {
    /** Catmull–Rom interpolation with tension 0.5 between [p1] and [p2]. */
    fun catmullRom(t: Double, p0: Double, p1: Double, p2: Double, p3: Double): Double {
        val v0 = (p2 - p0) * 0.5
        val v1 = (p3 - p1) * 0.5
        val t2 = t * t
        val t3 = t * t2
        return (2 * p1 - 2 * p2 + v0 + v1) * t3 + (-3 * p1 + 3 * p2 - 2 * v0 - v1) * t2 + v0 * t + p1
    }

    /** Quadratic Bézier interpolation. */
    fun quadraticBezier(t: Double, p0: Double, p1: Double, p2: Double): Double {
        val k = 1 - t
        return k * k * p0 + 2 * k * t * p1 + t * t * p2
    }

    /** Cubic Bézier interpolation. */
    fun cubicBezier(t: Double, p0: Double, p1: Double, p2: Double, p3: Double): Double {
        val k = 1 - t
        return k * k * k * p0 + 3 * k * k * t * p1 + 3 * k * t * t * p2 + t * t * t * p3
    }
}
