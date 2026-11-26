package com.aitorgf.threekt.loaders

import com.aitorgf.threekt.extras.core.Shape
import com.aitorgf.threekt.extras.core.ShapePath
import kotlin.js.JsExport

/**
 * A typeface in the Three.js JSON format, created by [FontLoader], that turns text into [Shape]s, as the
 * Three.js addon `Font`. Each glyph outline is a sequence of `m`, `l`, `q` and `b` commands in font units.
 */
@JsExport
class Font(
    /** Font metrics and glyphs. */
    val data: Data,
) {
    /** Writing direction for [generateShapes]. Three.js uses the strings `ltr`, `rtl` and `tb`. */
    enum class Direction { Ltr, Rtl, Tb }

    /** One glyph: its horizontal advance [ha] and its outline commands [o], or null for blank glyphs. */
    class Glyph(val ha: Double, val o: String?) {
        internal val outline: List<String>? by lazy { o?.split(' ') }
    }

    /** Typeface metrics in font units, and glyphs by character. */
    class Data(
        val familyName: String,
        val resolution: Double,
        val ascender: Double,
        val underlineThickness: Double,
        val boundingBoxYMin: Double,
        val boundingBoxYMax: Double,
        @property:JsExport.Ignore
        val glyphs: Map<String, Glyph>,
    )

    /**
     * Returns the shapes of [text] at [size] units per em. Lines break at `\n`. Characters missing from
     * the font use the `?` glyph, or are skipped when it is missing too; Three.js logs an error instead.
     */
    fun generateShapes(text: String, size: Double = 100.0, direction: Direction = Direction.Ltr): Array<Shape> =
        createPaths(text, size, direction).flatMap { it.toShapes().asList() }.toTypedArray()

    private fun createPaths(text: String, size: Double, direction: Direction): List<ShapePath> {
        // Split into code points so that surrogate pairs stay together, as Array.from(text) does.
        val chars = mutableListOf<String>()
        var i = 0
        while (i < text.length) {
            val end = if (text[i].isHighSurrogate() && i + 1 < text.length) i + 2 else i + 1
            chars.add(text.substring(i, end))
            i = end
        }
        val scale = size / data.resolution
        val lineHeight = (data.boundingBoxYMax - data.boundingBoxYMin + data.underlineThickness) * scale
        val paths = mutableListOf<ShapePath>()
        var offsetX = 0.0
        var offsetY = 0.0
        if (direction == Direction.Rtl || direction == Direction.Tb) chars.reverse()
        for (char in chars) {
            if (char == "\n") {
                offsetX = 0.0
                offsetY -= lineHeight
                continue
            }
            val glyph = data.glyphs[char] ?: data.glyphs["?"] ?: continue
            paths.add(createPath(glyph, scale, offsetX, offsetY))
            if (direction == Direction.Tb) {
                offsetX = 0.0
                offsetY += data.ascender * scale
            } else {
                offsetX += glyph.ha * scale
            }
        }
        return paths
    }

    private fun createPath(glyph: Glyph, scale: Double, offsetX: Double, offsetY: Double): ShapePath {
        val path = ShapePath()
        val outline = glyph.outline ?: return path
        var i = 0
        fun x() = outline[i++].toDouble() * scale + offsetX
        fun y() = outline[i++].toDouble() * scale + offsetY
        while (i < outline.size) {
            when (outline[i++]) {
                "m" -> path.moveTo(x(), y())
                "l" -> path.lineTo(x(), y())
                "q" -> {
                    val cpx = x(); val cpy = y()
                    val cpx1 = x(); val cpy1 = y()
                    path.quadraticCurveTo(cpx1, cpy1, cpx, cpy)
                }
                "b" -> {
                    val cpx = x(); val cpy = y()
                    val cpx1 = x(); val cpy1 = y()
                    val cpx2 = x(); val cpy2 = y()
                    path.bezierCurveTo(cpx1, cpy1, cpx2, cpy2, cpx, cpy)
                }
            }
        }
        return path
    }
}
