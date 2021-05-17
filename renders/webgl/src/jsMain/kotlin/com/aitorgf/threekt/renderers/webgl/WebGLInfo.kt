package com.aitorgf.threekt.renderers.webgl

import org.khronos.webgl.WebGLRenderingContext

@ExperimentalJsExport
@JsExport
class WebGLInfo {
    data class Memory(
        var geometries: Int = 0,
        var textures: Int = 0
    )

    data class Render(
        var frame: Int = 0,
        var calls: Int = 0,
        var triangles: Int = 0,
        var points: Int = 0,
        var lines: Int = 0
    )

    val memory = Memory()
    val render = Render()

    fun update(count: Int, mode: Int, instanceCount: Int) {
        render.calls++;

        when (mode) {
            WebGLRenderingContext.TRIANGLES -> render.triangles += instanceCount * (count / 3)
            WebGLRenderingContext.LINES -> render.lines += instanceCount * (count / 2)
            WebGLRenderingContext.LINE_STRIP -> render.lines += instanceCount * (count - 1)
            WebGLRenderingContext.LINE_LOOP -> render.lines += instanceCount * count
            WebGLRenderingContext.POINTS -> render.points += instanceCount * count
            else -> WebGLException("Unknown draw mode: $mode")
        }
    }

    fun reset() {
        this.render.frame++
        this.render.calls = 0
        this.render.triangles = 0
        this.render.points = 0
        this.render.lines = 0
    }
}
