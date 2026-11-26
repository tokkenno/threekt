package com.aitorgf.threekt.renderers

import com.aitorgf.threekt.math.color.ColorSpace
import com.aitorgf.threekt.textures.Texture
import com.aitorgf.threekt.textures.TextureFilter

internal class RenderTargetTexture(val target: WebGLRenderTarget, val depth: Boolean) : Texture() {
    init {
        colorSpace = ColorSpace.None
        flipY = false
        generateMipmaps = false
        minFilter = if (depth || target.type == RenderTargetType.Float) TextureFilter.Nearest else TextureFilter.Linear
        magFilter = minFilter
    }
}
