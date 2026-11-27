package com.aitorgf.threekt.materials

import com.aitorgf.threekt.math.Color
import com.aitorgf.threekt.textures.Texture
import kotlin.js.JsExport

/**
 * Unlit material for lines in a solid [color], optionally multiplied by a color texture [map] and by
 * vertex colors. Three.js takes a parameters object; Threekt takes the color as a constructor argument.
 * `linecap` and `linejoin` are omitted because WebGL ignores them.
 */
@JsExport
open class LineBasicMaterial(color: Color = Color(1.0, 1.0, 1.0)) : Material() {
    /** Creates a material from a packed sRGB [hex] color, as `0xff0000` for red. */
    @JsExport.Ignore
    constructor(hex: Int) : this(Color(hex))

    /** Line color in the working color space. A copy of the color passed to the constructor. */
    val color = Color(color.r, color.g, color.b)

    /** Color texture multiplied with [color], sampled with the geometry's `uv` attribute. */
    var map: Texture? = null

    /**
     * Line thickness in pixels. Most WebGL and OpenGL ES implementations draw lines only one pixel wide,
     * regardless of this value.
     */
    var linewidth = 1.0

    /** Whether the scene's fog affects this material. */
    var fog = true
}
