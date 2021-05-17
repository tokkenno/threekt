package com.aitorgf.threekt.renderers.webgl

import org.khronos.webgl.WebGLRenderingContext
import com.aitorgf.threekt.types.Map

@ExperimentalJsExport
@JsExport
class WebGLExtensions(
    val context: WebGLRenderingContext
) {
    val extensions = Map()
    var showWarning: Boolean = true

    private fun getCached(name: String): dynamic {
        val extension = this.extensions.get(name)

        if (extension != null) {
            return extension
        }

        val readExtension = when (name) {
            "WEBGL_depth_texture" -> context.getExtension("WEBGL_depth_texture") ||
                    context.getExtension("MOZ_WEBGL_depth_texture") ||
                    context.getExtension("WEBKIT_WEBGL_depth_texture")
            "EXT_texture_filter_anisotropic" -> context.getExtension("EXT_texture_filter_anisotropic") ||
                    context.getExtension("MOZ_EXT_texture_filter_anisotropic") ||
                    context.getExtension("WEBKIT_EXT_texture_filter_anisotropic")
            "WEBGL_compressed_texture_s3tc" -> context.getExtension("WEBGL_compressed_texture_s3tc") ||
                    context.getExtension("MOZ_WEBGL_compressed_texture_s3tc") ||
                    context.getExtension("WEBKIT_WEBGL_compressed_texture_s3tc")
            "WEBGL_compressed_texture_pvrtc" -> context.getExtension("WEBGL_compressed_texture_pvrtc") ||
                    context.getExtension("WEBKIT_WEBGL_compressed_texture_pvrtc")
            else -> context.getExtension(name)
        }

        this.extensions.set(name, readExtension)
        return readExtension
    }

    fun has(name: String): Boolean {
        return this.getCached(name) != null
    }

    fun get(name: String): dynamic {
        return this.getCached(name)
    }

    /**
     * Check if the current context meets the requirements
     * @param isWebGL2 If TRUE, checks the requeriments for WebGL 2.0. Else, checks the requeriments for WebGL 1.0
     * @return TRUE if the context meets the requirements
     */
    fun check(isWebGL2: Boolean): Boolean {
        val toCheck: MutableSet<String> = LinkedHashSet()

        if (isWebGL2) {
            toCheck.add("EXT_color_buffer_float")
        } else {
            toCheck.add("WEBGL_depth_texture")
            toCheck.add("OES_texture_float")
            toCheck.add("OES_texture_half_float")
            toCheck.add("OES_texture_half_float_linear")
            toCheck.add("OES_standard_derivatives")
            toCheck.add("OES_element_index_uint")
            toCheck.add("OES_vertex_array_object")
            toCheck.add("ANGLE_instanced_arrays")
        }

        toCheck.add("OES_texture_float_linear")
        toCheck.add("EXT_color_buffer_half_float")

        val checked = toCheck.map { this.getCached(it) }.filterNotNull()

        if (showWarning) {
            checked.forEach { console.warn("WebGLRenderer: $it extension not supported.") }
            showWarning = false
        }

        return checked.size == 0
    }
}
