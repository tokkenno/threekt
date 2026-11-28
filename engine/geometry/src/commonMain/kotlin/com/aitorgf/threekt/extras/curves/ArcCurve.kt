package com.aitorgf.threekt.extras.curves

import kotlin.js.JsExport
import kotlin.math.PI

/** A circular arc: an [EllipseCurve] with equal radii, as Three.js `ArcCurve`. */
@JsExport
class ArcCurve(
    aX: Double = 0.0,
    aY: Double = 0.0,
    aRadius: Double = 1.0,
    aStartAngle: Double = 0.0,
    aEndAngle: Double = 2 * PI,
    aClockwise: Boolean = false,
) : EllipseCurve(aX, aY, aRadius, aRadius, aStartAngle, aEndAngle, aClockwise)
