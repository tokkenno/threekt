package com.aitorgf.threekt.materials

import com.aitorgf.threekt.math.Color
import com.aitorgf.threekt.textures.Texture
import kotlin.js.JsExport

/**
 * Unlit material for [com.aitorgf.threekt.objects.Points] in a solid [color], optionally multiplied by a
 * color texture [map] spread across each point and by vertex colors. Three.js takes a parameters object;
 * Threekt takes the color as a constructor argument. `alphaMap` is not supported yet.
 */
@JsExport
class PointsMaterial(color: Color = Color(1.0, 1.0, 1.0)) : Material() {
    /** Creates a material from a packed sRGB [hex] color, as `0xff0000` for red. */
    @JsExport.Ignore
    constructor(hex: Int) : this(Color(hex))

    /** Point color in the working color space. A copy of the color passed to the constructor. */
    val color = Color(color.r, color.g, color.b)

    /** Color texture multiplied with [color], mapped across each point. */
    var map: Texture? = null

    /** Point size in pixels at a distance of half the view height, or in pixels without [sizeAttenuation]. */
    var size = 1.0

    /** Whether points shrink with distance under a perspective camera. */
    var sizeAttenuation = true
    /** Samples with alpha below this threshold are discarded. */
    var alphaTest = 0.0

    /** Whether the scene's fog affects this material. */
    var fog = true
}
