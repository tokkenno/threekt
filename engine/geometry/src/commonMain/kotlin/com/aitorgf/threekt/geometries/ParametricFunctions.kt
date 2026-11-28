package com.aitorgf.threekt.geometries

import com.aitorgf.threekt.math.Vector3
import kotlin.js.JsExport
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Sample surfaces for [ParametricGeometry], as the Three.js addon `ParametricFunctions` module. Each
 * function writes the point for `u, v ∈ [0, 1]` into `target`. From JavaScript, wrap them in an arrow
 * function when passing them to the geometry.
 */
@JsExport
object ParametricFunctions {
    /** A Klein bottle. */
    fun klein(v: Double, u: Double, target: Vector3) {
        val angleU = u * PI * 2
        val angleV = v * 2 * PI
        val x: Double
        val z: Double
        if (angleU < PI) {
            x = 3 * cos(angleU) * (1 + sin(angleU)) + (2 * (1 - cos(angleU) / 2)) * cos(angleU) * cos(angleV)
            z = -8 * sin(angleU) - 2 * (1 - cos(angleU) / 2) * sin(angleU) * cos(angleV)
        } else {
            x = 3 * cos(angleU) * (1 + sin(angleU)) + (2 * (1 - cos(angleU) / 2)) * cos(angleV + PI)
            z = -8 * sin(angleU)
        }
        val y = -2 * (1 - cos(angleU) / 2) * sin(angleV)
        target.set(x, y, z)
    }

    /** A unit square in the XZ plane. */
    fun plane(u: Double, v: Double, target: Vector3) {
        target.set(u, 0.0, v)
    }

    /** A Möbius strip. */
    fun mobius(u: Double, t: Double, target: Vector3) {
        val offset = u - 0.5
        val v = 2 * PI * t
        val a = 2.0
        target.set(
            cos(v) * (a + offset * cos(v / 2)),
            sin(v) * (a + offset * cos(v / 2)),
            offset * sin(v / 2),
        )
    }

    /** A Möbius strip with an elliptical cross-section. */
    fun mobius3d(u: Double, t: Double, target: Vector3) {
        val angleU = u * PI * 2
        val angleT = t * 2 * PI
        val phi = angleU / 2
        val major = 2.25
        val a = 0.125
        val b = 0.65
        var x = a * cos(angleT) * cos(phi) - b * sin(angleT) * sin(phi)
        val z = a * cos(angleT) * sin(phi) + b * sin(angleT) * cos(phi)
        val y = (major + x) * sin(angleU)
        x = (major + x) * cos(angleU)
        target.set(x, y, z)
    }
}
