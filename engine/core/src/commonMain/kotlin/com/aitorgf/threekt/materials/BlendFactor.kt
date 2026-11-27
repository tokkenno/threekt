package com.aitorgf.threekt.materials

import kotlin.js.JsExport

/** An operand multiplier for [Blending.Custom]. [SrcAlphaSaturate] is valid only as a source factor. */
@JsExport
enum class BlendFactor(internal val glValue: Int) {
    Zero(0), One(1), SrcColor(0x0300), OneMinusSrcColor(0x0301), SrcAlpha(0x0302), OneMinusSrcAlpha(0x0303),
    DstAlpha(0x0304), OneMinusDstAlpha(0x0305), DstColor(0x0306), OneMinusDstColor(0x0307), SrcAlphaSaturate(0x0308),
}
