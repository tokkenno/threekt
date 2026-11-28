package com.aitorgf.threekt.textures

import kotlin.js.JsExport

/** Sampling filters for a texture, replacing the numeric Three.js filter constants. */
@JsExport
enum class TextureFilter {
    Nearest,
    Linear,
    NearestMipmapNearest,
    LinearMipmapNearest,
    NearestMipmapLinear,
    LinearMipmapLinear,
}
