package com.aitorgf.threekt.loaders

import com.aitorgf.threekt.math.Color
import com.aitorgf.threekt.math.color.ColorSpace
import kotlin.js.JsExport

/**
 * Parses Wavefront MTL libraries for [OBJLoader]. Reads diffuse, specular and emissive RGB, shininess,
 * opacity and plain `map_Kd` filenames. Other map types and map options are ignored.
 * The caller supplies the text and resolves images on its platform.
 */
@JsExport
class MTLLoader {
    /** Parses [text] into a creator for the named materials and their diffuse texture files. */
    fun parse(text: String): MaterialCreator {
        val materials = linkedMapOf<String, MaterialCreator.MaterialInfo>()
        var current: MaterialCreator.MaterialInfo? = null
        for (rawLine in text.lineSequence()) {
            val line = rawLine.substringBefore('#').trim()
            if (line.isEmpty()) continue
            val boundary = line.indexOfFirst { it.isWhitespace() }
            if (boundary < 0) continue
            val key = line.substring(0, boundary).lowercase()
            val value = line.substring(boundary).trim()
            if (key == "newmtl") {
                current = MaterialCreator.MaterialInfo()
                materials[value] = current
                continue
            }

            val material = current ?: continue
            when (key) {
                "kd" -> material.color = readColor(value)
                "ks" -> material.specular = readColor(value)
                "ke" -> material.emissive = readColor(value)
                "ns" -> material.shininess = value.toDouble()
                "d" -> material.opacity = value.removePrefix("-halo").trim().toDouble()
                "tr" -> material.opacity = 1 - value.toDouble()
                "map_kd" -> if (!value.startsWith("-")) material.map = value
            }
        }
        return MaterialCreator(materials)
    }

    private fun readColor(value: String): Color {
        val channels = value.split(mtlWhitespace, limit = 4)
        require(channels.size >= 3) { "An MTL color requires three RGB components." }
        return Color().setRGB(channels[0].toDouble(), channels[1].toDouble(), channels[2].toDouble(), ColorSpace.SRGB)
    }
}

private val mtlWhitespace = Regex("\\s+")
