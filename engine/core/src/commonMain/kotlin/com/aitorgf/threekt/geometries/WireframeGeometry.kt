package com.aitorgf.threekt.geometries

import com.aitorgf.threekt.core.BufferGeometry
import com.aitorgf.threekt.core.Float32BufferAttribute
import kotlin.js.JsExport

/** Emits one segment for each unique triangle edge. Shares position matching with [EdgesGeometry]. */
@JsExport
class WireframeGeometry(geometry: BufferGeometry) : BufferGeometry() {
    init {
        setAttribute("position", Float32BufferAttribute(geometryEdges(geometry, null), 3))
    }
}
