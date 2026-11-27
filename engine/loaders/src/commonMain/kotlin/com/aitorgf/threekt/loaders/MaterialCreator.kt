package com.aitorgf.threekt.loaders

import com.aitorgf.threekt.materials.MeshPhongMaterial
import com.aitorgf.threekt.math.Color
import com.aitorgf.threekt.textures.Texture
import kotlin.js.JsExport

/** Named Phong material definitions from an [MTLLoader]. */
@JsExport
class MaterialCreator internal constructor(private val materials: Map<String, MaterialInfo>) {
    /** Distinct diffuse texture filenames as written in the MTL file. */
    val textureFiles: Array<String> get() = materials.values.mapNotNull { it.map }.distinct().toTypedArray()

    private val textures = mutableMapOf<String, Texture>()

    /** Associates a decoded [texture] with its MTL [fileName]. */
    fun setTexture(fileName: String, texture: Texture) {
        textures[fileName] = texture
    }

    /**
     * Creates a new material, or returns null when [materialName] is absent. Unlike Three.js, material
     * instances are independent so the OBJ loader can set per-mesh shading. They share decoded textures.
     */
    fun create(materialName: String): MeshPhongMaterial? {
        val info = materials[materialName] ?: return null
        return MeshPhongMaterial().apply {
            info.color?.let(color::copy)
            info.specular?.let(specular::copy)
            info.emissive?.let(emissive::copy)
            shininess = info.shininess
            opacity = info.opacity
            transparent = opacity < 1
            map = info.map?.let(textures::get)
        }
    }

    internal class MaterialInfo {
        var color: Color? = null
        var specular: Color? = null
        var emissive: Color? = null
        var shininess = 30.0
        var opacity = 1.0
        var map: String? = null
    }
}
