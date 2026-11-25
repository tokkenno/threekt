package com.aitorgf.threekt.materials

import com.aitorgf.threekt.math.Color
import com.aitorgf.threekt.textures.Texture
import kotlin.js.JsExport

/**
 * Material lit with the Blinn-Phong reflectance model, with a diffuse [color] and shiny [specular]
 * highlights. It responds to ambient and directional lights. Three.js takes a parameters object; Threekt
 * takes the color as a constructor argument. Texture maps other than [map], and point, spot, and
 * hemisphere lights, are not supported yet.
 */
@JsExport
class MeshPhongMaterial(color: Color = Color(1.0, 1.0, 1.0)) : Material() {
    /** Creates a material from a packed sRGB [hex] color, as `0xd5d5d5`. */
    @JsExport.Ignore
    constructor(hex: Int) : this(Color(hex))

    /** Diffuse color in the working color space. A copy of the color passed to the constructor. */
    val color = Color(color.r, color.g, color.b)

    /** Color of the specular highlights. */
    val specular = Color(0x111111)

    /** How sharp the specular highlights are. Higher values give smaller, brighter highlights. */
    var shininess = 30.0

    /** Color that the material emits regardless of lighting. */
    val emissive = Color(0.0, 0.0, 0.0)

    /** Multiplier for [emissive]. */
    var emissiveIntensity = 1.0

    /** Color texture multiplied with [color], sampled with the geometry's `uv` attribute. */
    var map: Texture? = null

    /** Whether each face is lit with its own geometric normal instead of interpolated vertex normals. */
    var flatShading = false

    /** Whether the scene's fog affects this material. */
    var fog = true
}
