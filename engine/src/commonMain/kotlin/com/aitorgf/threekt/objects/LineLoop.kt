package com.aitorgf.threekt.objects

import com.aitorgf.threekt.core.BufferGeometry
import com.aitorgf.threekt.materials.LineBasicMaterial
import com.aitorgf.threekt.materials.Material
import kotlin.js.JsExport

/** A line through the vertices that also connects the last vertex back to the first. */
@JsExport
class LineLoop(
    geometry: BufferGeometry = BufferGeometry(),
    material: Material = LineBasicMaterial(),
) : Line(geometry, material) {
    override fun clone(recursive: Boolean): LineLoop = LineLoop().copy(this, recursive) as LineLoop
}
