package com.aitorgf.threekt.lights

import com.aitorgf.threekt.core.Object3D
import com.aitorgf.threekt.math.Color
import kotlin.js.JsExport

/** Base class for lights. The light's contribution is [color] multiplied by [intensity]. */
@JsExport
abstract class Light(color: Color, var intensity: Double) : Object3D() {
    /** Light color in the working color space. A copy of the color passed to the constructor. */
    val color = Color(color.r, color.g, color.b)

    /** Lights, like cameras, aim their local negative Z axis at the target of [lookAt]. */
    override val looksAlongNegativeZ: Boolean get() = true

    /** Copies [color] and [intensity] of [source] when it is a light. Returns this light. */
    override fun copy(source: Object3D, recursive: Boolean): Light {
        super.copy(source, recursive)
        if (source is Light) {
            color.copy(source.color)
            intensity = source.intensity
        }
        return this
    }
}
