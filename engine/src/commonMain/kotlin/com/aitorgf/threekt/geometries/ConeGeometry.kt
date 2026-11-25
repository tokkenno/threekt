package com.aitorgf.threekt.geometries

import kotlin.js.JsExport
import kotlin.math.PI

/** A Y-aligned cone: a [CylinderGeometry] with a zero top radius, as Three.js `ConeGeometry`. */
@JsExport
class ConeGeometry(
    radius: Double = 1.0,
    height: Double = 1.0,
    radialSegments: Int = 32,
    heightSegments: Int = 1,
    openEnded: Boolean = false,
    thetaStart: Double = 0.0,
    thetaLength: Double = 2 * PI,
) : CylinderGeometry(0.0, radius, height, radialSegments, heightSegments, openEnded, thetaStart, thetaLength)
