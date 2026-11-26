package com.aitorgf.threekt.extras.core

import com.aitorgf.threekt.math.Vector2
import kotlin.js.JsExport

/**
 * A closed 2D [Path] with optional [holes], used to build [com.aitorgf.threekt.geometries.ShapeGeometry]
 * and [com.aitorgf.threekt.geometries.ExtrudeGeometry], as Three.js `Shape`.
 */
@JsExport
class Shape(points: Array<Vector2>? = null) : Path(points) {
    /** Paths cut out of this shape. From JavaScript, add holes with `holes.asJsArrayView().push(path)`. */
    val holes: MutableList<Path> = mutableListOf()

    /** Returns the sampled points of each hole. */
    fun getPointsHoles(divisions: Int): List<List<Vector2>> = holes.map { it.getPoints(divisions) }

    /** Sampled outline and holes, as returned by [extractPoints]. */
    class ShapePoints(val shape: List<Vector2>, val holes: List<List<Vector2>>)

    /** Samples the outline and holes with [divisions] segments per curve. */
    fun extractPoints(divisions: Int): ShapePoints = ShapePoints(getPoints(divisions), getPointsHoles(divisions))
}
