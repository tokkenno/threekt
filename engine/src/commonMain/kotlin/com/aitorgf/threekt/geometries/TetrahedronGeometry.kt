package com.aitorgf.threekt.geometries

import kotlin.js.JsExport

private val vertices = doubleArrayOf(1.0, 1.0, 1.0, -1.0, -1.0, 1.0, -1.0, 1.0, -1.0, 1.0, -1.0, -1.0)
private val indices = intArrayOf(2, 1, 0, 0, 3, 2, 1, 3, 0, 2, 3, 1)

/** Tetrahedron with optional triangular subdivision projected onto [radius], as Three.js `TetrahedronGeometry`. */
@JsExport
class TetrahedronGeometry(radius: Double = 1.0, detail: Int = 0) : PolyhedronGeometry(vertices, indices, radius, detail)
