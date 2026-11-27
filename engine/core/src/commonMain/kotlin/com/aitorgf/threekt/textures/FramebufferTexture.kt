package com.aitorgf.threekt.textures

import kotlin.js.JsExport

/** RGBA8 destination for a rectangular framebuffer copy. Threekt uses zeroed data for its initial allocation. */
@JsExport
class FramebufferTexture(width: Int, height: Int) : DataTexture(ByteArray(width * height * 4), width, height)
