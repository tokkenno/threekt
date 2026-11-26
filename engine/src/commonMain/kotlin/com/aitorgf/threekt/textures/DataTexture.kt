package com.aitorgf.threekt.textures

import com.aitorgf.threekt.math.color.ColorSpace
import kotlin.js.JsExport

/** A tightly packed RGBA8 pixel array with its first row at the bottom. Call [markUpdated] after writes. */
@JsExport
open class DataTexture(val data: ByteArray, val width: Int, val height: Int) : Texture() {
    init {
        require(width > 0 && height > 0 && width.toLong() * height * 4 == data.size.toLong())
        colorSpace = ColorSpace.None
        minFilter = TextureFilter.Nearest
        magFilter = TextureFilter.Nearest
        generateMipmaps = false
        flipY = false
    }
}
