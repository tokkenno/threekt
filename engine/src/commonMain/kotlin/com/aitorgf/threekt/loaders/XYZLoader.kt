package com.aitorgf.threekt.loaders

import com.aitorgf.threekt.core.BufferGeometry
import com.aitorgf.threekt.core.Float32BufferAttribute
import com.aitorgf.threekt.math.Color
import com.aitorgf.threekt.math.color.ColorSpace
import kotlin.js.JsExport

private val xyzWhitespace = Regex("\\s+")

/** Parses XYZ or XYZRGB point data supplied as text. RGB columns are sRGB bytes. */
@JsExport
class XYZLoader {
    /** Returns point geometry. Rows without RGB use white when other rows supply colors. */
    fun parse(text: String): BufferGeometry {
        val vertices = mutableListOf<Float>()
        val colors = mutableListOf<Float>()
        val color = Color()
        var hasColors = false
        for (row in text.lineSequence()) {
            val line = row.substringBefore('#').trim()
            val values = line.split(xyzWhitespace)
            if (values.size != 3 && values.size != 6) continue
            for (i in 0..2) vertices.add(values[i].toFloat())
            if (values.size == 6) {
                hasColors = true
                color.setRGB(values[3].toDouble() / 255, values[4].toDouble() / 255, values[5].toDouble() / 255, ColorSpace.SRGB)
            } else {
                color.set(1.0, 1.0, 1.0)
            }
            colors.add(color.r.toFloat())
            colors.add(color.g.toFloat())
            colors.add(color.b.toFloat())
        }
        return BufferGeometry().setAttribute("position", Float32BufferAttribute(vertices.toFloatArray(), 3)).apply {
            if (hasColors) setAttribute("color", Float32BufferAttribute(colors.toFloatArray(), 3))
        }
    }
}
