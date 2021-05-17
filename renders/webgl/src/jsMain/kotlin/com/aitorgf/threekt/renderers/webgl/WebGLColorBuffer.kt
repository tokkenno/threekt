package com.aitorgf.threekt.renderers.webgl

import com.aitorgf.threekt.math.MutableVector4
import org.khronos.webgl.WebGLRenderingContext

@ExperimentalJsExport
@JsExport
class WebGLColorBuffer(
    private val gl: WebGLRenderingContext
) {
    private var locked: Boolean = false

    private val color: MutableVector4 = MutableVector4()
    private val currentColorClear: MutableVector4 = MutableVector4(0.0, 0.0, 0.0, 0.0)
    private var currentColorMask: Boolean? = null

    fun setMask(colorMask: Boolean) {
        if (currentColorMask != colorMask && !locked) {
            gl.colorMask(colorMask, colorMask, colorMask, colorMask)
            currentColorMask = colorMask
        }
    }

    fun setLocked(lock: Boolean) {
        locked = lock
    }

    fun setClear(r: Double, g: Double, b: Double, a: Double, premultipliedAlpha: Boolean) {
        var rCalc = r
        var gCalc = g
        var bCalc = b

        if (premultipliedAlpha) {
            rCalc *= a
            gCalc *= a
            bCalc *= a
        }

        color.set(rCalc, gCalc, bCalc, a)

        if (!currentColorClear.equals(color)) {
            gl.clearColor(rCalc.toFloat(), gCalc.toFloat(), bCalc.toFloat(), a.toFloat())
            currentColorClear.set(color)
        }
    }

    fun reset() {
        locked = false
        currentColorMask = null
        currentColorClear.set(-1.0, 0.0, 0.0, 0.0)
    }
}
