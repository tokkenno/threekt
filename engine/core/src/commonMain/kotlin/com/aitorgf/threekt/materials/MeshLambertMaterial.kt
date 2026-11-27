package com.aitorgf.threekt.materials

import com.aitorgf.threekt.math.Color
import com.aitorgf.threekt.textures.Texture
import kotlin.js.JsExport

/**
 * Matte material lit with the Lambertian reflectance model, without specular highlights, as Three.js
 * `MeshLambertMaterial`. It responds to ambient, hemisphere, directional and point lights. Three.js takes
 * a parameters object; Threekt takes the color as a constructor argument. Texture maps other than [map]
 * are not supported yet.
 */
@JsExport
class MeshLambertMaterial(color: Color = Color(1.0, 1.0, 1.0)) : Material() {
    /** Creates a material from a packed sRGB [hex] color, as `0xd5d5d5`. */
    @JsExport.Ignore
    constructor(hex: Int) : this(Color(hex))

    /** Diffuse color in the working color space. A copy of the color passed to the constructor. */
    val color = Color(color.r, color.g, color.b)

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
