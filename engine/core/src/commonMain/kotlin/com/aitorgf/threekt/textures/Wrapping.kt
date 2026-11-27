package com.aitorgf.threekt.textures

import kotlin.js.JsExport

/** How texture coordinates outside the `[0, 1]` range are sampled. */
@JsExport
enum class Wrapping {
    /** Tiles the image infinitely. */
    Repeat,
    /** Stretches the edge texels to the boundary of the mesh. */
    ClampToEdge,
    /** Tiles the image and mirrors every other repetition. */
    MirroredRepeat,
}
