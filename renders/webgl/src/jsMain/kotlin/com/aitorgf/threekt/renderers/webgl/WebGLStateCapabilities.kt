package com.aitorgf.threekt.renderers.webgl

import org.khronos.webgl.WebGLRenderingContext

@ExperimentalJsExport
@JsExport
class WebGLStateCapabilities(
    val gl: WebGLRenderingContext
) {
    private val enabled: MutableMap<Int, Boolean> = HashMap()

    fun enable(id: Int) {
        val isEnabled = this.enabled.get(id) ?: false
        if (!isEnabled) {
            gl.enable(id)
            this.enabled[id] = true
        }
    }

    fun disable(id: Int) {
        val isEnabled = this.enabled.get(id) ?: false
        if (isEnabled) {
            gl.disable(id)
            this.enabled[id] = false
        }
    }
}
