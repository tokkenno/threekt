package com.aitorgf.threekt.geometries

import kotlin.js.JsExport
import kotlin.math.sqrt

private val t = (1 + sqrt(5.0)) / 2
private val vertices = doubleArrayOf(
    -1.0, t, 0.0, 1.0, t, 0.0, -1.0, -t, 0.0, 1.0, -t, 0.0,
    0.0, -1.0, t, 0.0, 1.0, t, 0.0, -1.0, -t, 0.0, 1.0, -t,
    t, 0.0, -1.0, t, 0.0, 1.0, -t, 0.0, -1.0, -t, 0.0, 1.0,
)
private val indices = intArrayOf(
    0, 11, 5, 0, 5, 1, 0, 1, 7, 0, 7, 10, 0, 10, 11,
    1, 5, 9, 5, 11, 4, 11, 10, 2, 10, 7, 6, 7, 1, 8,
    3, 9, 4, 3, 4, 2, 3, 2, 6, 3, 6, 8, 3, 8, 9,
    4, 9, 5, 2, 4, 11, 6, 2, 10, 8, 6, 7, 9, 8, 1,
)

/** Icosahedron with optional triangular subdivision projected onto the supplied radius. */
@JsExport
class IcosahedronGeometry(radius: Double = 1.0, detail: Int = 0) : PolyhedronGeometry(vertices, indices, radius, detail)
