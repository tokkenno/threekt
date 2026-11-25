package com.aitorgf.threekt.lights

import com.aitorgf.threekt.core.Object3D
import com.aitorgf.threekt.math.Color
import kotlin.js.JsExport

/**
 * Light with parallel rays, like sunlight. It shines from its world position toward the world position
 * of [target]; only the direction matters. The light starts at [Object3D.DEFAULT_UP], shining downward.
 * Shadows are not supported yet.
 */
@JsExport
class DirectionalLight(color: Color = Color(1.0, 1.0, 1.0), intensity: Double = 1.0) : Light(color, intensity) {
    /** Creates a light from a packed sRGB [hex] color, as `0xffffff`. */
    @JsExport.Ignore
    constructor(hex: Int, intensity: Double = 1.0) : this(Color(hex), intensity)

    /**
     * Object the light points at, at the origin by default. Add it to the scene to move it with the
     * scene's transforms; otherwise its world matrix is only updated when changed directly.
     */
    var target = Object3D()

    init {
        position.copy(DEFAULT_UP)
        updateMatrix()
    }
}
