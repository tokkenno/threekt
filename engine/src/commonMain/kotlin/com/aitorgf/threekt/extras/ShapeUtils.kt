package com.aitorgf.threekt.extras

import com.aitorgf.threekt.math.Vector2
import kotlin.js.JsExport

/** Utilities for 2D contours, as Three.js `ShapeUtils`. */
@JsExport
object ShapeUtils {
    /** Returns the signed area of [contour], positive when it is counterclockwise. */
    fun area(contour: List<Vector2>): Double {
        val n = contour.size
        var a = 0.0
        var p = n - 1
        for (q in 0 until n) {
            a += contour[p].x * contour[q].y - contour[q].x * contour[p].y
            p = q
        }
        return a * 0.5
    }

    /** Returns whether [pts] wind clockwise. */
    fun isClockWise(pts: List<Vector2>): Boolean = area(pts) < 0

    /**
     * Triangulates [contour] with [holes] and returns triangles as index triples into the contour followed
     * by the holes. As in Three.js, a duplicated closing point is removed from the end of [contour] and of
     * each hole, so callers must build their vertex lists from the trimmed lists.
     */
    fun triangulateShape(contour: MutableList<Vector2>, holes: List<MutableList<Vector2>>): List<IntArray> {
        val vertices = mutableListOf<Double>()
        removeDupEndPts(contour)
        addContour(vertices, contour)
        var holeIndex = contour.size
        holes.forEach(::removeDupEndPts)
        val holeIndices = IntArray(holes.size)
        for ((i, hole) in holes.withIndex()) {
            holeIndices[i] = holeIndex
            holeIndex += hole.size
            addContour(vertices, hole)
        }
        val triangles = Earcut.triangulate(vertices.toDoubleArray(), holeIndices)
        return List(triangles.size / 3) { triangles.copyOfRange(it * 3, it * 3 + 3) }
    }

    private fun removeDupEndPts(points: MutableList<Vector2>) {
        val l = points.size
        if (l > 2 && points[l - 1] == points[0]) points.removeAt(l - 1)
    }

    private fun addContour(vertices: MutableList<Double>, contour: List<Vector2>) {
        for (point in contour) {
            vertices.add(point.x)
            vertices.add(point.y)
        }
    }
}
