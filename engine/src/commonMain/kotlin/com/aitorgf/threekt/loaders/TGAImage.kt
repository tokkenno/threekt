package com.aitorgf.threekt.loaders

import kotlin.js.JsExport

/** Decoded TGA pixels in top-to-bottom RGBA order. Unsigned components are stored as bytes. */
@JsExport
class TGAImage(val width: Int, val height: Int, val pixels: ByteArray)
