package com.aitorgf.threekt.materials

import com.aitorgf.threekt.math.Color
import com.aitorgf.threekt.textures.Texture
import kotlin.js.JsExport

/**
 * Unlit material for [com.aitorgf.threekt.objects.Sprite]s, as Three.js `SpriteMaterial`. It is
 * transparent by default. Three.js takes a parameters object; Threekt takes the color and map as
 * constructor arguments. Alpha maps are not supported yet.
 */
@JsExport
class SpriteMaterial(color: Color = Color(1.0, 1.0, 1.0), var map: Texture? = null) : Material() {
    /** Creates a material from a packed sRGB [hex] color. */
    @JsExport.Ignore
    constructor(hex: Int, map: Texture? = null) : this(Color(hex), map)

    /** Color multiplied with [map], in the working color space. A copy of the color passed in. */
    val color = Color(color.r, color.g, color.b)

    /** Rotation of the sprite in radians around the view direction. */
    var rotation = 0.0

    /** Whether the sprite shrinks with distance under a perspective camera. */
    var sizeAttenuation = true

    /** Whether the scene's fog affects this material. */
    var fog = true

    init {
        transparent = true
    }
}
