package com.aitorgf.threekt.extras.core

import com.aitorgf.threekt.extras.ShapeUtils
import com.aitorgf.threekt.math.Box2
import com.aitorgf.threekt.math.Color
import com.aitorgf.threekt.math.Vector2
import kotlin.js.JsExport
import kotlin.math.abs

/**
 * A set of 2D subpaths, as produced by fonts and SVG, that [toShapes] turns into [Shape]s with holes, as
 * Three.js `ShapePath`. Three.js reads the fill rule from `userData.style.fillRule`; Threekt exposes
 * [fillRule].
 */
@JsExport
class ShapePath {
    /** Fill rule that decides which nested subpaths are holes. */
    enum class FillRule { NonZero, EvenOdd }

    /** Color of the path, for example from an SVG fill. */
    val color = Color()

    /** Subpaths, one per [moveTo]. */
    val subPaths: MutableList<Path> = mutableListOf()

    /** Subpath that drawing commands extend, or null before the first [moveTo]. */
    var currentPath: Path? = null
        private set

    var fillRule = FillRule.NonZero

    /** Starts a new subpath at ([x], [y]). Returns this shape path. */
    fun moveTo(x: Double, y: Double): ShapePath {
        val path = Path()
        currentPath = path
        subPaths.add(path)
        path.moveTo(x, y)
        return this
    }

    fun lineTo(x: Double, y: Double): ShapePath {
        currentPath!!.lineTo(x, y)
        return this
    }

    fun quadraticCurveTo(aCPx: Double, aCPy: Double, aX: Double, aY: Double): ShapePath {
        currentPath!!.quadraticCurveTo(aCPx, aCPy, aX, aY)
        return this
    }

    fun bezierCurveTo(aCP1x: Double, aCP1y: Double, aCP2x: Double, aCP2y: Double, aX: Double, aY: Double): ShapePath {
        currentPath!!.bezierCurveTo(aCP1x, aCP1y, aCP2x, aCP2y, aX, aY)
        return this
    }

    fun splineThru(pts: Array<Vector2>): ShapePath {
        currentPath!!.splineThru(pts)
        return this
    }

    private class Entry(
        val subPath: Path,
        val points: List<Vector2>,
        val boundingBox: Box2,
        val interiorPoint: Vector2,
        val absArea: Double,
        var winding: Int,
    ) {
        var container: Entry? = null
        var exclude = false
        var isHole = false
    }

    /**
     * Converts the subpaths into shapes. Subpaths are sorted by area; each one's winding number at an
     * interior point, under [fillRule], decides whether it is an outline, a hole of the subpath that
     * contains it, or a redundant overlap that is skipped. Degenerate subpaths are ignored. The shapes
     * share curve objects with the subpaths.
     */
    fun toShapes(): Array<Shape> {
        fun isInside(winding: Int) = if (fillRule == FillRule.NonZero) winding != 0 else (winding and 1) != 0

        val entries = mutableListOf<Entry>()
        for (subPath in subPaths) {
            val points = subPath.getPoints()
            if (points.size < 3) continue
            val area = ShapeUtils.area(points)
            if (area == 0.0) continue
            val boundingBox = Box2()
            for (point in points) boundingBox.expandByPoint(point)
            entries.add(Entry(subPath, points, boundingBox, interiorPoint(points, boundingBox), abs(area), if (area < 0) -1 else 1))
        }
        // Sorted by decreasing area, any possible container of an entry comes before it.
        entries.sortByDescending { it.absArea }

        for ((i, entry) in entries.withIndex()) {
            var containerWinding = 0
            for (j in i - 1 downTo 0) {
                val candidate = entries[j]
                if (!candidate.boundingBox.containsBox(entry.boundingBox)) continue
                if (!pointInPolygon(entry.interiorPoint, candidate.points)) continue
                entry.container = if (candidate.exclude) candidate.container else candidate
                containerWinding = candidate.winding
                entry.winding += containerWinding
                break
            }
            // A subpath that does not change the insideness is a redundant overlap.
            if (isInside(entry.winding) == isInside(containerWinding)) entry.exclude = true
        }
        // A retained entry is an outline when it has no container or its container is a hole.
        for (entry in entries) {
            if (entry.exclude) continue
            val container = entry.container
            entry.isHole = container != null && !container.isHole
        }

        val shapes = mutableListOf<Shape>()
        val shapeByEntry = mutableMapOf<Entry, Shape>()
        for (entry in entries) {
            if (entry.exclude || entry.isHole) continue
            val shape = Shape()
            shape.curves.addAll(entry.subPath.curves)
            shapes.add(shape)
            shapeByEntry[entry] = shape
        }
        for (entry in entries) {
            if (entry.exclude || !entry.isHole) continue
            val shape = shapeByEntry[entry.container] ?: continue
            val hole = Path()
            hole.curves.addAll(entry.subPath.curves)
            shape.holes.add(hole)
        }
        return shapes.toTypedArray()
    }

    private companion object {
        /** Even-odd ray-casting point-in-polygon test, valid for simple polygons. */
        fun pointInPolygon(p: Vector2, polygon: List<Vector2>): Boolean {
            var inside = false
            val n = polygon.size
            var j = n - 1
            for (i in 0 until n) {
                val a = polygon[i]
                val b = polygon[j]
                if ((a.y > p.y) != (b.y > p.y) && p.x < (b.x - a.x) * (p.y - a.y) / (b.y - a.y) + a.x) inside = !inside
                j = i
            }
            return inside
        }

        /**
         * Returns a point strictly inside [polygon]: the bounding-box center when it is inside, otherwise the
         * midpoint of the first two crossings of a horizontal ray through it, as paper.js does.
         */
        fun interiorPoint(polygon: List<Vector2>, boundingBox: Box2): Vector2 {
            val point = boundingBox.getCenter(Vector2())
            if (pointInPolygon(point, polygon)) return point
            val y = point.y
            val intercepts = mutableListOf<Double>()
            val n = polygon.size
            for (i in 0 until n) {
                val a = polygon[i]
                val b = polygon[(i + 1) % n]
                // The half-open crossing rule counts each vertex once and skips horizontal edges.
                if ((a.y > y) != (b.y > y)) intercepts.add(a.x + (y - a.y) * (b.x - a.x) / (b.y - a.y))
            }
            if (intercepts.size > 1) {
                intercepts.sort()
                point.x = (intercepts[0] + intercepts[1]) / 2
            }
            return point
        }
    }
}
