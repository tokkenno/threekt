package com.aitorgf.threekt.materials

import kotlin.js.JsExport

/** An operand multiplier for [Blending.Custom]. [SrcAlphaSaturate] is valid only as a source factor. */
@JsExport
enum class BlendFactor {
    Zero, One, SrcColor, OneMinusSrcColor, SrcAlpha, OneMinusSrcAlpha,
    DstAlpha, OneMinusDstAlpha, DstColor, OneMinusDstColor, SrcAlphaSaturate,
}
