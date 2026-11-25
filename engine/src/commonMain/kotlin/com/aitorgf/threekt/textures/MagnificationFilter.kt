package com.aitorgf.threekt.textures

import kotlin.js.JsExport

/** Sampling mode used when a texture is drawn larger than its source. */
@JsExport
enum class MagnificationFilter(internal val glValue: Int) { Nearest(0x2600), Linear(0x2601) }
