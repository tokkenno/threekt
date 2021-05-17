package com.aitorgf.threekt.renderers.webgl

import org.khronos.webgl.WebGLRenderingContext

@ExperimentalJsExport
@JsExport
class WebGLDepthBuffer(
    private val gl: WebGLRenderingContext,
    private val capabilities: WebGLStateCapabilities
) {
    var locked: Boolean = false

    var currentDepthMask: Boolean? = null
    var currentDepthFunc: Int? = null
    var currentDepthClear: Float? = null

    fun setTest(depthTest: Boolean) {
        if (depthTest) {
            this.capabilities.enable(WebGLRenderingContext.DEPTH_TEST)
        } else {
            this.capabilities.disable(WebGLRenderingContext.DEPTH_TEST)
        }
    }

    fun setMask(depthMask: Boolean) {
        if (currentDepthMask != depthMask && !locked) {
            gl.depthMask(depthMask)
            currentDepthMask = depthMask
        }
    }

    fun setLocked(lock: Boolean) {
        this.locked = lock
    }

    fun setClear(depth: Float) {
        if (currentDepthClear != depth) {
            gl.clearDepth(depth)
            currentDepthClear = depth
        }
    }

    fun reset() {
        locked = false

        currentDepthMask = null
        currentDepthFunc = null
        currentDepthClear = null
    }

    fun setFunc(depthFunc: Int) {
        if (currentDepthFunc != depthFunc) {
            when (depthFunc) {
                NeverDepth -> gl.depthFunc(WebGLRenderingContext.NEVER)
                AlwaysDepth -> gl.depthFunc(WebGLRenderingContext.ALWAYS)
                LessDepth -> gl.depthFunc(WebGLRenderingContext.LESS)
                LessEqualDepth -> gl.depthFunc(WebGLRenderingContext.LEQUAL)
                EqualDepth -> gl.depthFunc(WebGLRenderingContext.EQUAL)
                GreaterEqualDepth -> gl.depthFunc(WebGLRenderingContext.GEQUAL)
                GreaterDepth -> gl.depthFunc(WebGLRenderingContext.GREATER)
                NotEqualDepth -> gl.depthFunc(WebGLRenderingContext.NOTEQUAL)
                else -> gl.depthFunc(WebGLRenderingContext.LEQUAL)
            }
        } else {
            gl.depthFunc(WebGLRenderingContext.LEQUAL)
        }

        currentDepthFunc = depthFunc
    }
}
