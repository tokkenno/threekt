package com.aitorgf.threekt.renderers.webgl

import kotlinx.browser.window

@ExperimentalJsExport
@JsExport
class WebGLAnimation {
    private var isAnimating: Boolean = false
    private var animationLoop: AnimationLoop? = null
    private var requestId: Int = 0

    fun start() {
        if (!isAnimating && animationLoop != null) {
            this.requestId = window.requestAnimationFrame {
                this.onAnimationFrame(it)
            }
            this.isAnimating = true
        }
    }

    fun stop() {
        window.cancelAnimationFrame(requestId)
        this.isAnimating = false
    }

    fun setAnimationLoop(callback: AnimationLoop) {
        this.animationLoop = callback
    }

    fun clearAnimationLoop() {
        this.animationLoop = null
    }

    private fun onAnimationFrame(time: Double): Unit {
        val localAnimationLoop = this.animationLoop
        if (localAnimationLoop != null) {
            localAnimationLoop(time)
            requestId = window.requestAnimationFrame(localAnimationLoop)
        } else {
            throw WebGLException("Animation frame is null")
        }
    }
}
