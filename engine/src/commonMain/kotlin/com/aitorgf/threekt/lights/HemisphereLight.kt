package com.aitorgf.threekt.lights

import com.aitorgf.threekt.math.Color
import com.aitorgf.threekt.core.Object3D
import kotlin.js.JsExport

/**
 * Indirect light that blends its [groundColor] and sky [color] according to the surface normal.
 * Its world position defines the sky direction and defaults to +Y. Colors are copied.
 */
@JsExport
class HemisphereLight(
    skyColor: Color = Color(1.0, 1.0, 1.0),
    groundColor: Color = Color(1.0, 1.0, 1.0),
    intensity: Double = 1.0,
) : Light(skyColor, intensity) {
    /** Color on the hemisphere opposite the light's direction. */
    val groundColor = Color(groundColor.r, groundColor.g, groundColor.b)

    init {
        position.copy(DEFAULT_UP)
        updateMatrix()
    }

    /** Copies the light settings and [groundColor] of [source] when it is a hemisphere light. */
    override fun copy(source: Object3D, recursive: Boolean): HemisphereLight {
        super.copy(source, recursive)
        if (source is HemisphereLight) groundColor.copy(source.groundColor)
        return this
    }

    override fun clone(recursive: Boolean): HemisphereLight = HemisphereLight().copy(this, recursive)
}
