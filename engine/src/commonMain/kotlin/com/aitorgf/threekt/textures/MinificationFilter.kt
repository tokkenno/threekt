package com.aitorgf.threekt.textures

import kotlin.js.JsExport

/** Sampling mode used when a texture is drawn smaller than its source. */
@JsExport
enum class MinificationFilter(internal val glValue: Int) {
    Nearest(0x2600), Linear(0x2601), NearestMipmapNearest(0x2700), LinearMipmapNearest(0x2701),
    NearestMipmapLinear(0x2702), LinearMipmapLinear(0x2703),
}
