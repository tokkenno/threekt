package com.aitorgf.threekt.textures

import kotlin.js.JsExport

/** Sampling filters for a texture, replacing the numeric Three.js filter constants. */
@JsExport
enum class TextureFilter(internal val glValue: Int) {
    Nearest(0x2600),
    Linear(0x2601),
    NearestMipmapNearest(0x2700),
    LinearMipmapNearest(0x2701),
    NearestMipmapLinear(0x2702),
    LinearMipmapLinear(0x2703),
}
