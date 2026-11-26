package com.aitorgf.threekt.extras.curves

import com.aitorgf.threekt.extras.core.Curve
import com.aitorgf.threekt.math.Vector2
import kotlin.js.JsExport
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * An elliptical arc centered at ([aX], [aY]) from [aStartAngle] to [aEndAngle], rotated by [aRotation],
 * as Three.js `EllipseCurve`. Angles are in radians; [aClockwise] reverses the sweep.
 */
@JsExport
open class EllipseCurve(
    var aX: Double = 0.0,
    var aY: Double = 0.0,
    var xRadius: Double = 1.0,
    var yRadius: Double = 1.0,
    var aStartAngle: Double = 0.0,
    var aEndAngle: Double = 2 * PI,
    var aClockwise: Boolean = false,
    var aRotation: Double = 0.0,
) : Curve<Vector2>() {
    override fun createPoint(): Vector2 = Vector2()

    override fun getPoint(t: Double, optionalTarget: Vector2?): Vector2 {
        val point = optionalTarget ?: Vector2()
        val twoPi = PI * 2
        var deltaAngle = aEndAngle - aStartAngle
        val samePoints = abs(deltaAngle) < EPSILON
        // Ensure that deltaAngle is in [0, 2π].
        while (deltaAngle < 0) deltaAngle += twoPi
        while (deltaAngle > twoPi) deltaAngle -= twoPi
        if (deltaAngle < EPSILON) deltaAngle = if (samePoints) 0.0 else twoPi
        if (aClockwise && !samePoints) deltaAngle = if (deltaAngle == twoPi) -twoPi else deltaAngle - twoPi
        val angle = aStartAngle + t * deltaAngle
        var x = aX + xRadius * cos(angle)
        var y = aY + yRadius * sin(angle)
        if (aRotation != 0.0) {
            val cos = cos(aRotation)
            val sin = sin(aRotation)
            val tx = x - aX
            val ty = y - aY
            // Rotate the point about the center of the ellipse.
            x = tx * cos - ty * sin + aX
            y = tx * sin + ty * cos + aY
        }
        return point.set(x, y)
    }

    private companion object {
        /** JavaScript `Number.EPSILON`. */
        const val EPSILON = 2.220446049250313e-16
    }
}
