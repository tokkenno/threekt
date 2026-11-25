package com.aitorgf.threekt.renderers.webgl

import kotlin.js.JsExport

/** Limits and optional features reported by a renderer's [GLContext]. */
@JsExport
class WebGLCapabilities internal constructor(gl: GLContext) {
    /** Whether the context supports anisotropic filtering. */
    internal val anisotropicFiltering: Boolean = gl.hasExtension("EXT_texture_filter_anisotropic")

    private val maxAnisotropy: Int =
        if (anisotropicFiltering) gl.getParameterFloat(GL.MAX_TEXTURE_MAX_ANISOTROPY_EXT).toInt() else 1

    /**
     * Returns the largest supported value for [com.aitorgf.threekt.textures.Texture.anisotropy], or `1`
     * when anisotropic filtering is unavailable.
     */
    fun getMaxAnisotropy(): Int = maxAnisotropy
}
