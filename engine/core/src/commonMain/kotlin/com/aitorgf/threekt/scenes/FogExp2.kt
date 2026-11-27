package com.aitorgf.threekt.scenes

import com.aitorgf.threekt.math.Color
import kotlin.js.JsExport

/** Fog that blends toward its color with exponential squared falloff using [density]. */
@JsExport
class FogExp2(color: Color, var density: Double = 0.00025) : SceneFog(color) {
    override fun clone(): FogExp2 = FogExp2(color, density).also { it.name = name }
}
