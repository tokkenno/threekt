package com.aitorgf.threekt.geometries

import kotlin.js.JsExport

private val vertices = doubleArrayOf(1.0, 0.0, 0.0, -1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, -1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, -1.0)
private val indices = intArrayOf(0, 2, 4, 0, 4, 3, 0, 3, 5, 0, 5, 2, 1, 2, 5, 1, 5, 3, 1, 3, 4, 1, 4, 2)

/** Octahedron with optional triangular subdivision projected onto [radius], as Three.js `OctahedronGeometry`. */
@JsExport
class OctahedronGeometry(radius: Double = 1.0, detail: Int = 0) : PolyhedronGeometry(vertices, indices, radius, detail)
