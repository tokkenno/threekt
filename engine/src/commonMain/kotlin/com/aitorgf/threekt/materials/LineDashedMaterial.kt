package com.aitorgf.threekt.materials

import com.aitorgf.threekt.math.Color
import kotlin.js.JsExport

/** Dashed lines based on the geometry's cumulative line distances. Call Line.computeLineDistances before rendering. */
@JsExport
class LineDashedMaterial(color: Color = Color(1.0, 1.0, 1.0)) : LineBasicMaterial(color) {
    var scale = 1.0
    var dashSize = 3.0
    var gapSize = 1.0
}
