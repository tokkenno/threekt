package com.aitorgf.threekt.extras.core

import com.aitorgf.threekt.extras.curves.CubicBezierCurve
import com.aitorgf.threekt.extras.curves.EllipseCurve
import com.aitorgf.threekt.extras.curves.LineCurve
import com.aitorgf.threekt.extras.curves.QuadraticBezierCurve
import com.aitorgf.threekt.extras.curves.SplineCurve
import com.aitorgf.threekt.math.Vector2
import kotlin.js.JsExport

/**
 * A 2D path built with a canvas-like API, as Three.js `Path`. Each drawing command appends a curve from
 * [currentPoint] and moves it to the curve's end. Angles are in radians.
 */
@JsExport
open class Path(points: Array<Vector2>? = null) : CurvePath<Vector2>() {
    /** End of the last drawing command. */
    val currentPoint = Vector2()

    init {
        if (points != null) setFromPoints(points)
    }

    override fun createPoint(): Vector2 = Vector2()

    /** Moves to the first of [points] and draws lines through the others. Returns this path. */
    fun setFromPoints(points: Array<Vector2>): Path {
        moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
        return this
    }

    /** Moves the current point without drawing. Returns this path. */
    fun moveTo(x: Double, y: Double): Path {
        currentPoint.set(x, y)
        return this
    }

    /** Draws a straight line to ([x], [y]). Returns this path. */
    fun lineTo(x: Double, y: Double): Path {
        curves.add(LineCurve(currentPoint.copy(), Vector2(x, y)))
        currentPoint.set(x, y)
        return this
    }

    /** Draws a quadratic Bézier curve with control point ([aCPx], [aCPy]) to ([aX], [aY]). Returns this path. */
    fun quadraticCurveTo(aCPx: Double, aCPy: Double, aX: Double, aY: Double): Path {
        curves.add(QuadraticBezierCurve(currentPoint.copy(), Vector2(aCPx, aCPy), Vector2(aX, aY)))
        currentPoint.set(aX, aY)
        return this
    }

    /** Draws a cubic Bézier curve with two control points to ([aX], [aY]). Returns this path. */
    fun bezierCurveTo(aCP1x: Double, aCP1y: Double, aCP2x: Double, aCP2y: Double, aX: Double, aY: Double): Path {
        curves.add(CubicBezierCurve(currentPoint.copy(), Vector2(aCP1x, aCP1y), Vector2(aCP2x, aCP2y), Vector2(aX, aY)))
        currentPoint.set(aX, aY)
        return this
    }

    /** Draws a Catmull–Rom spline from the current point through [pts]. Returns this path. */
    fun splineThru(pts: Array<Vector2>): Path {
        curves.add(SplineCurve(arrayOf(currentPoint.copy()) + pts))
        currentPoint.copy(pts[pts.size - 1])
        return this
    }

    /** Draws a circular arc centered at an offset from the current point. Returns this path. */
    fun arc(aX: Double, aY: Double, aRadius: Double, aStartAngle: Double, aEndAngle: Double, aClockwise: Boolean = false): Path {
        absarc(aX + currentPoint.x, aY + currentPoint.y, aRadius, aStartAngle, aEndAngle, aClockwise)
        return this
    }

    /** Draws a circular arc centered at ([aX], [aY]). Returns this path. */
    fun absarc(aX: Double, aY: Double, aRadius: Double, aStartAngle: Double, aEndAngle: Double, aClockwise: Boolean = false): Path {
        absellipse(aX, aY, aRadius, aRadius, aStartAngle, aEndAngle, aClockwise)
        return this
    }

    /** Draws an elliptical arc centered at an offset from the current point. Returns this path. */
    fun ellipse(
        aX: Double, aY: Double, xRadius: Double, yRadius: Double, aStartAngle: Double, aEndAngle: Double,
        aClockwise: Boolean = false, aRotation: Double = 0.0,
    ): Path {
        absellipse(aX + currentPoint.x, aY + currentPoint.y, xRadius, yRadius, aStartAngle, aEndAngle, aClockwise, aRotation)
        return this
    }

    /**
     * Draws an elliptical arc centered at ([aX], [aY]). When the path already has curves and the arc does
     * not start at the current point, a connecting line is added first. Returns this path.
     */
    fun absellipse(
        aX: Double, aY: Double, xRadius: Double, yRadius: Double, aStartAngle: Double, aEndAngle: Double,
        aClockwise: Boolean = false, aRotation: Double = 0.0,
    ): Path {
        val curve = EllipseCurve(aX, aY, xRadius, yRadius, aStartAngle, aEndAngle, aClockwise, aRotation)
        if (curves.isNotEmpty()) {
            val firstPoint = curve.getPoint(0.0)
            if (firstPoint != currentPoint) lineTo(firstPoint.x, firstPoint.y)
        }
        curves.add(curve)
        currentPoint.copy(curve.getPoint(1.0))
        return this
    }

}
