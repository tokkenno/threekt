package com.aitorgf.threekt.renderers.webgl

import org.khronos.webgl.WebGLRenderingContext

@ExperimentalJsExport
@JsExport
class WebGLStencilBuffer(
    private val gl: WebGLRenderingContext,
    private val capabilities: WebGLStateCapabilities
) {
    var locked: Boolean = false
    var currentStencilMask: Int? = null
    var currentStencilFunc: Int? = null
    var currentStencilRef: Int? = null
    var currentStencilFuncMask: Int? = null
    var currentStencilFail: Int? = null
    var currentStencilZFail: Int? = null
    var currentStencilZPass: Int? = null
    var currentStencilClear: Int? = null

    fun setTest(stencilTest: Boolean) {
        if (!locked) {
            if (stencilTest) {
                this.capabilities.enable(WebGLRenderingContext.STENCIL_TEST)
            } else {
                this.capabilities.disable(WebGLRenderingContext.STENCIL_TEST)
            }
        }
    }

    fun setMask(stencilMask: Int) {
        if (currentStencilMask != stencilMask && !locked) {
            this.gl.stencilMask(stencilMask)
            currentStencilMask = stencilMask
        }
    }

    fun setFunc(stencilFunc: Int, stencilRef: Int, stencilMask: Int) {
        if (currentStencilFunc != stencilFunc ||
            currentStencilRef != stencilRef ||
            currentStencilFuncMask != stencilMask
        ) {
            this.gl.stencilFunc(stencilFunc, stencilRef, stencilMask)
            currentStencilFunc = stencilFunc
            currentStencilRef = stencilRef
            currentStencilFuncMask = stencilMask
        }
    }

    fun setOp(stencilFail: Int, stencilZFail: Int, stencilZPass: Int) {
        if (currentStencilFail != stencilFail ||
            currentStencilZFail != stencilZFail ||
            currentStencilZPass != stencilZPass
        ) {
            this.gl.stencilOp(stencilFail, stencilZFail, stencilZPass)
            currentStencilFail = stencilFail
            currentStencilZFail = stencilZFail
            currentStencilZPass = stencilZPass
        }
    }

    fun setLocked(lock: Boolean) {
        locked = lock
    }

    fun setClear(stencil: Int) {
        if (currentStencilClear != stencil) {
            gl.clearStencil(stencil)
            currentStencilClear = stencil
        }
    }

    fun reset() {
        locked = false

        currentStencilMask = null
        currentStencilFunc = null
        currentStencilRef = null
        currentStencilFuncMask = null
        currentStencilFail = null
        currentStencilZFail = null
        currentStencilZPass = null
        currentStencilClear = null
    }
}
