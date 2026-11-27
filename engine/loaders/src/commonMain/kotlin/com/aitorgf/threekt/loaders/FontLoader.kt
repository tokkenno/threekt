package com.aitorgf.threekt.loaders

import com.aitorgf.threekt.utils.Json
import kotlin.js.JsExport

/**
 * Parses typefaces in the Three.js JSON format, as produced by facetype.js, into [Font]s, as the Three.js
 * addon `FontLoader`. Like the other Threekt loaders it parses text that the platform has already read;
 * Three.js also fetches the file.
 */
@JsExport
class FontLoader {
    /** Parses the JSON [text] of a typeface. Throws [IllegalArgumentException] if it is not one. */
    fun parse(text: String): Font {
        val json = Json.parse(text) as? Map<*, *> ?: throw IllegalArgumentException("A typeface must be a JSON object.")
        fun number(map: Map<*, *>, key: String) = (map[key] as? Double) ?: throw IllegalArgumentException("Missing '$key'.")
        val boundingBox = json["boundingBox"] as? Map<*, *> ?: throw IllegalArgumentException("Missing 'boundingBox'.")
        val glyphs = (json["glyphs"] as? Map<*, *> ?: throw IllegalArgumentException("Missing 'glyphs'."))
            .entries.associate { (key, value) ->
                val glyph = value as Map<*, *>
                key as String to Font.Glyph(number(glyph, "ha"), glyph["o"] as? String)
            }
        return Font(
            Font.Data(
                familyName = json["familyName"] as? String ?: "",
                resolution = number(json, "resolution"),
                ascender = number(json, "ascender"),
                underlineThickness = number(json, "underlineThickness"),
                boundingBoxYMin = number(boundingBox, "yMin"),
                boundingBoxYMax = number(boundingBox, "yMax"),
                glyphs = glyphs,
            ),
        )
    }
}
