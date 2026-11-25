package com.aitorgf.threekt.materials

import com.aitorgf.threekt.math.Color
import com.aitorgf.threekt.textures.Texture
import kotlin.js.JsExport

/**
 * Unlit material that draws meshes in a solid [color], optionally multiplied by a color texture [map]
 * and by vertex colors. Three.js takes a parameters object; Threekt takes the color and map as
 * constructor arguments.
 */
@JsExport
class MeshBasicMaterial(color: Color = Color(1.0, 1.0, 1.0), var map: Texture? = null) : Material() {
    /** Creates a material from a packed sRGB [hex] color, as `0xff0000` for red. */
    @JsExport.Ignore
    constructor(hex: Int, map: Texture? = null) : this(Color(hex), map)

    /** Base color in the working color space. A copy of the color passed to the constructor. */
    val color = Color(color.r, color.g, color.b)

    /** Whether the scene's fog affects this material. */
    var fog = true
}
