package com.aitorgf.threekt.math

import com.aitorgf.threekt.math.color.ColorSpace
import kotlin.js.JsExport
import kotlin.math.roundToInt

/**
 * Scalar color lookup table. Colors carry sRGB components to match Three.js's Lut addon.
 * The browser example renders the legend from these entries instead of exposing DOM canvas methods here.
 */
@JsExport
class Lut(colormap: String = "rainbow", count: Int = 32) {
    var minV = 0.0
        private set
    var maxV = 1.0
        private set
    var n = 0
        private set
    private var lut = emptyArray<Color>()

    init {
        setColorMap(colormap, count)
    }

    fun setMin(min: Double): Lut = apply { minV = min }
    fun setMax(max: Double): Lut = apply { maxV = max }

    /** Samples [count] intervals, including both endpoints. Unknown names use rainbow. */
    fun setColorMap(colormap: String, count: Int = 32): Lut = apply {
        require(count > 0)
        n = count
        val colors = (maps[colormap] ?: maps.getValue("rainbow")).map {
            Color().setHex(it, ColorSpace.LinearSRGB)
        }
        lut = Array(count + 1) { index ->
            val alpha = index.toDouble() / count
            val stop = stops.indexOfFirst { it >= alpha }.coerceAtLeast(1)
            val t = (alpha - stops[stop - 1]) / (stops[stop] - stops[stop - 1])
            Color().lerpColors(colors[stop - 1], colors[stop], t)
        }
    }

    /** Returns the shared table entry closest to the clamped value. Copy it before modifying it. */
    fun getColor(value: Double): Color {
        val t = if (maxV > minV) (value.coerceIn(minV, maxV) - minV) / (maxV - minV) else 0.0
        return lut[(t * n).roundToInt()]
    }

    private companion object {
        val stops = doubleArrayOf(0.0, 0.2, 0.5, 0.8, 1.0)
        val maps = mapOf(
            "rainbow" to intArrayOf(0x0000ff, 0x00ffff, 0x00ff00, 0xffff00, 0xff0000),
            "cooltowarm" to intArrayOf(0x3c4ec2, 0x9bbcff, 0xdcdcdc, 0xf6a385, 0xb40426),
            "blackbody" to intArrayOf(0x000000, 0x780000, 0xe63200, 0xffff00, 0xffffff),
            "grayscale" to intArrayOf(0x000000, 0x404040, 0x7f7f80, 0xbfbfbf, 0xffffff),
        )
    }
}
