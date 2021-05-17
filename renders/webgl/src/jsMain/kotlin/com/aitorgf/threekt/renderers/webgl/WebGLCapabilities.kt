package com.aitorgf.threekt.renderers.webgl

import org.khronos.webgl.WebGLRenderingContext

@ExperimentalJsExport
@JsExport
class WebGLCapabilities(
    val context: WebGLRenderingContext,
    val extensions: WebGLExtensions
) {
    private var maxAnisotropyCache: Int? = null

    val isWebGL2: Boolean = false

    val maxAnisotropy: Int
        get() {
            val cache = maxAnisotropyCache
            if (cache != null) {
                return cache
            };

            if (this.extensions.has("EXT_texture_filter_anisotropic")) {
                val extension = this.extensions.get("EXT_texture_filter_anisotropic");
                val index = extension.MAX_TEXTURE_MAX_ANISOTROPY_EXT as Int
                maxAnisotropyCache = this.context.getParameter(index) as Int? ?: 0;
            } else {
                maxAnisotropyCache = 0;
            }

            return maxAnisotropy;
        }
}
