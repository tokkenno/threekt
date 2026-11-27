package com.aitorgf.threekt.textures

import kotlin.js.JsExport

/** Texture backed by a decoded browser image or canvas. */
@JsExport
class ImageTexture(override val image: Any) : Texture()
