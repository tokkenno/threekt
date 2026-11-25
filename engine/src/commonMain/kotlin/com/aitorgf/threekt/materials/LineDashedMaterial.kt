package com.aitorgf.threekt.materials

import com.aitorgf.threekt.math.Color
import kotlin.js.JsExport

/**
 * Draws alternating dashes and gaps along the geometry's line distances. Compute them with
 * [com.aitorgf.threekt.objects.Line.computeLineDistances] before rendering.
 * Threekt takes a color and dash settings as constructor arguments.
 */
@JsExport
class LineDashedMaterial(
    color: Color = Color(1.0, 1.0, 1.0),
    var scale: Double = 1.0,
    var dashSize: Double = 3.0,
    var gapSize: Double = 1.0,
) : LineBasicMaterial(color)
