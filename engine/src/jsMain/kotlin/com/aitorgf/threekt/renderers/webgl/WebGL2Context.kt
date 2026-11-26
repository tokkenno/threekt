package com.aitorgf.threekt.renderers.webgl

import kotlin.js.JsExport

/**
 * [GLContext] backed by the `webgl2` context of a browser [canvas]. Images can be any source accepted by
 * `texImage2D`, such as an `HTMLImageElement` or a canvas. Throws [IllegalStateException] if WebGL2 is
 * unavailable. Set [alpha] to false when the browser should composite the canvas as opaque.
 */
@JsExport
class WebGL2Context(private val canvas: dynamic, alpha: Boolean = true) : GLContext {
    /** The underlying `WebGL2RenderingContext`. */
    val gl: dynamic

    init {
        val attributes: dynamic = js("({})")
        attributes.alpha = alpha
        gl = canvas.getContext("webgl2", attributes) ?: error("WebGL2 is unavailable.")
    }

    override val drawingBufferWidth: Int get() = gl.drawingBufferWidth.unsafeCast<Int>()
    override val drawingBufferHeight: Int get() = gl.drawingBufferHeight.unsafeCast<Int>()

    override fun resizeDrawingBuffer(width: Int, height: Int) {
        canvas.width = width
        canvas.height = height
    }

    override fun hasExtension(name: String): Boolean = gl.getExtension(name) != null

    override fun getError(): Int = gl.getError().unsafeCast<Int>()
    override fun getParameterInt(pname: Int): Int = toInt(gl.getParameter(pname))
    override fun getParameterFloat(pname: Int): Double = gl.getParameter(pname).unsafeCast<Double>()
    override fun getParameterInts(pname: Int, target: IntArray): IntArray {
        val values = gl.getParameter(pname)
        for (index in target.indices) target[index] = values[index].unsafeCast<Int>()
        return target
    }
    override fun getParameterHandle(pname: Int): Any? = gl.getParameter(pname)

    override fun enable(cap: Int) { gl.enable(cap) }
    override fun disable(cap: Int) { gl.disable(cap) }
    override fun viewport(x: Int, y: Int, width: Int, height: Int) { gl.viewport(x, y, width, height) }
    override fun scissor(x: Int, y: Int, width: Int, height: Int) { gl.scissor(x, y, width, height) }
    override fun clearColor(red: Double, green: Double, blue: Double, alpha: Double) { gl.clearColor(red, green, blue, alpha) }
    override fun clearDepth(depth: Double) { gl.clearDepth(depth) }
    override fun clear(mask: Int) { gl.clear(mask) }
    override fun depthFunc(func: Int) { gl.depthFunc(func) }
    override fun depthMask(flag: Boolean) { gl.depthMask(flag) }
    override fun cullFace(mode: Int) { gl.cullFace(mode) }
    override fun frontFace(mode: Int) { gl.frontFace(mode) }
    override fun blendEquation(mode: Int) { gl.blendEquation(mode) }
    override fun blendFunc(sfactor: Int, dfactor: Int) { gl.blendFunc(sfactor, dfactor) }
    override fun blendFuncSeparate(srcRGB: Int, dstRGB: Int, srcAlpha: Int, dstAlpha: Int) {
        gl.blendFuncSeparate(srcRGB, dstRGB, srcAlpha, dstAlpha)
    }

    override fun createBuffer(): Any = gl.createBuffer() ?: error("Could not create a WebGL buffer.")
    override fun deleteBuffer(buffer: Any?) { gl.deleteBuffer(buffer) }
    override fun bindBuffer(target: Int, buffer: Any?) { gl.bindBuffer(target, buffer) }
    override fun bufferFloatData(target: Int, data: FloatArray, usage: Int) { gl.bufferData(target, data, usage) }
    override fun bufferByteData(target: Int, data: ByteArray, usage: Int) {
        // Kotlin stores bytes in an Int8Array; upload the same bytes through an unsigned view.
        val bytes: dynamic = data
        gl.bufferData(target, js("new Uint8Array(bytes.buffer, bytes.byteOffset, bytes.length)"), usage)
    }
    override fun bufferShortData(target: Int, data: ShortArray, usage: Int) { gl.bufferData(target, data, usage) }
    override fun bufferShortSubData(target: Int, offset: Int, data: ShortArray, srcOffset: Int, length: Int) {
        gl.bufferSubData(target, offset, data, srcOffset, length)
    }
    override fun bufferIntSubData(target: Int, offset: Int, data: IntArray, srcOffset: Int, length: Int) {
        gl.bufferSubData(target, offset, data, srcOffset, length)
    }
    override fun bufferIntData(target: Int, data: IntArray, usage: Int) { gl.bufferData(target, data, usage) }
    override fun bufferFloatSubData(target: Int, offset: Int, data: FloatArray, srcOffset: Int, length: Int) {
        gl.bufferSubData(target, offset, data, srcOffset, length)
    }
    override fun bufferByteSubData(target: Int, offset: Int, data: ByteArray, srcOffset: Int, length: Int) {
        val bytes: dynamic = data
        gl.bufferSubData(target, offset, js("new Uint8Array(bytes.buffer, bytes.byteOffset, bytes.length)"), srcOffset, length)
    }

    override fun enableVertexAttribArray(index: Int) { gl.enableVertexAttribArray(index) }
    override fun disableVertexAttribArray(index: Int) { gl.disableVertexAttribArray(index) }
    override fun vertexAttribPointer(index: Int, size: Int, type: Int, normalized: Boolean, stride: Int, offset: Int) {
        gl.vertexAttribPointer(index, size, type, normalized, stride, offset)
    }
    override fun vertexAttribIPointer(index: Int, size: Int, type: Int, stride: Int, offset: Int) {
        gl.vertexAttribIPointer(index, size, type, stride, offset)
    }
    override fun lineWidth(width: Double) { gl.lineWidth(width) }
    override fun drawArrays(mode: Int, first: Int, count: Int) { gl.drawArrays(mode, first, count) }
    override fun drawElements(mode: Int, count: Int, type: Int, offset: Int) { gl.drawElements(mode, count, type, offset) }
    override fun vertexAttribDivisor(index: Int, divisor: Int) { gl.vertexAttribDivisor(index, divisor) }
    override fun drawArraysInstanced(mode: Int, first: Int, count: Int, instanceCount: Int) {
        gl.drawArraysInstanced(mode, first, count, instanceCount)
    }
    override fun drawElementsInstanced(mode: Int, count: Int, type: Int, offset: Int, instanceCount: Int) {
        gl.drawElementsInstanced(mode, count, type, offset, instanceCount)
    }

    override fun createShader(type: Int): Any = gl.createShader(type) ?: error("Could not create a WebGL shader.")
    override fun shaderSource(shader: Any, source: String) { gl.shaderSource(shader, source) }
    override fun compileShader(shader: Any) { gl.compileShader(shader) }
    override fun getShaderParameter(shader: Any, pname: Int): Int = toInt(gl.getShaderParameter(shader, pname))
    override fun getShaderInfoLog(shader: Any): String = gl.getShaderInfoLog(shader)?.unsafeCast<String>() ?: ""
    override fun deleteShader(shader: Any?) { gl.deleteShader(shader) }
    override fun createProgram(): Any = gl.createProgram() ?: error("Could not create the WebGL program.")
    override fun attachShader(program: Any, shader: Any) { gl.attachShader(program, shader) }
    override fun linkProgram(program: Any) { gl.linkProgram(program) }
    override fun getProgramParameter(program: Any, pname: Int): Int = toInt(gl.getProgramParameter(program, pname))
    override fun getProgramInfoLog(program: Any): String = gl.getProgramInfoLog(program)?.unsafeCast<String>() ?: ""
    override fun useProgram(program: Any?) { gl.useProgram(program) }
    override fun deleteProgram(program: Any?) { gl.deleteProgram(program) }
    override fun getActiveAttrib(program: Any, index: Int): GLActiveInfo = activeInfo(gl.getActiveAttrib(program, index))
    override fun getActiveUniform(program: Any, index: Int): GLActiveInfo = activeInfo(gl.getActiveUniform(program, index))
    override fun getAttribLocation(program: Any, name: String): Int = gl.getAttribLocation(program, name).unsafeCast<Int>()
    override fun getUniformLocation(program: Any, name: String): Any? = gl.getUniformLocation(program, name)

    override fun uniform1f(location: Any?, x: Double) { gl.uniform1f(location, x) }
    override fun uniform1fv(location: Any?, value: FloatArray) { gl.uniform1fv(location, value) }
    override fun uniform1i(location: Any?, x: Int) { gl.uniform1i(location, x) }
    override fun uniform1iv(location: Any?, value: IntArray) { gl.uniform1iv(location, value) }
    override fun uniform1ui(location: Any?, x: Int) { gl.uniform1ui(location, x) }
    override fun uniform2f(location: Any?, x: Double, y: Double) { gl.uniform2f(location, x, y) }
    override fun uniform2i(location: Any?, x: Int, y: Int) { gl.uniform2i(location, x, y) }
    override fun uniform3f(location: Any?, x: Double, y: Double, z: Double) { gl.uniform3f(location, x, y, z) }
    override fun uniform4f(location: Any?, x: Double, y: Double, z: Double, w: Double) { gl.uniform4f(location, x, y, z, w) }
    override fun uniformMatrix3fv(location: Any?, transpose: Boolean, value: FloatArray) { gl.uniformMatrix3fv(location, transpose, value) }
    override fun uniformMatrix4fv(location: Any?, transpose: Boolean, value: FloatArray) { gl.uniformMatrix4fv(location, transpose, value) }

    override fun createTexture(): Any = gl.createTexture() ?: error("Could not create a WebGL texture.")
    override fun deleteTexture(texture: Any?) { gl.deleteTexture(texture) }
    override fun bindTexture(target: Int, texture: Any?) { gl.bindTexture(target, texture) }
    override fun activeTexture(texture: Int) { gl.activeTexture(texture) }
    override fun texParameteri(target: Int, pname: Int, param: Int) { gl.texParameteri(target, pname, param) }
    override fun texParameterf(target: Int, pname: Int, param: Double) { gl.texParameterf(target, pname, param) }
    override fun texStorage2D(target: Int, levels: Int, internalformat: Int, width: Int, height: Int) {
        gl.texStorage2D(target, levels, internalformat, width, height)
    }
    override fun texImage2D(target: Int, level: Int, internalformat: Int, format: Int, type: Int, source: Any, flipY: Boolean) {
        val previousFlip = gl.getParameter(gl.UNPACK_FLIP_Y_WEBGL)
        try {
            gl.pixelStorei(gl.UNPACK_FLIP_Y_WEBGL, flipY)
            gl.texImage2D(target, level, internalformat, format, type, source)
        } finally {
            gl.pixelStorei(gl.UNPACK_FLIP_Y_WEBGL, previousFlip)
        }
    }
    override fun texImage3D(
        target: Int, level: Int, internalformat: Int, width: Int, height: Int, depth: Int, border: Int,
        format: Int, type: Int, data: FloatArray,
    ) {
        gl.texImage3D(target, level, internalformat, width, height, depth, border, format, type, data)
    }
    override fun texImage2DBytes(internalformat: Int, width: Int, height: Int, data: ByteArray) {
        gl.texImage2D(GL.TEXTURE_2D, 0, internalformat, width, height, 0, GL.RGBA, GL.UNSIGNED_BYTE, unsignedBytes(data))
    }
    override fun texSubImage2DBytes(x: Int, y: Int, width: Int, height: Int, data: ByteArray) {
        gl.texSubImage2D(GL.TEXTURE_2D, 0, x, y, width, height, GL.RGBA, GL.UNSIGNED_BYTE, unsignedBytes(data))
    }
    private fun unsignedBytes(data: ByteArray): dynamic {
        val bytes = data.asDynamic()
        return js("new Uint8Array(bytes.buffer, bytes.byteOffset, bytes.byteLength)")
    }
    override fun copyTexSubImage2D(x: Int, y: Int, width: Int, height: Int) {
        gl.copyTexSubImage2D(GL.TEXTURE_2D, 0, 0, 0, x, y, width, height)
    }
    override fun readPixelsFloat(x: Int, y: Int, width: Int, height: Int, target: FloatArray) {
        gl.readPixels(x, y, width, height, GL.RGBA, GL.FLOAT, target)
    }
    override fun generateMipmap(target: Int) { gl.generateMipmap(target) }

    override fun createFramebuffer(): Any = gl.createFramebuffer() ?: error("Could not create a WebGL framebuffer.")
    override fun deleteFramebuffer(framebuffer: Any?) { gl.deleteFramebuffer(framebuffer) }
    override fun bindFramebuffer(target: Int, framebuffer: Any?) { gl.bindFramebuffer(target, framebuffer) }
    override fun framebufferTexture2D(target: Int, attachment: Int, textarget: Int, texture: Any?, level: Int) {
        gl.framebufferTexture2D(target, attachment, textarget, texture, level)
    }
    override fun framebufferRenderbuffer(target: Int, attachment: Int, renderbuffertarget: Int, renderbuffer: Any?) {
        gl.framebufferRenderbuffer(target, attachment, renderbuffertarget, renderbuffer)
    }
    override fun checkFramebufferStatus(target: Int): Int = gl.checkFramebufferStatus(target).unsafeCast<Int>()
    override fun createRenderbuffer(): Any = gl.createRenderbuffer() ?: error("Could not create a WebGL renderbuffer.")
    override fun deleteRenderbuffer(renderbuffer: Any?) { gl.deleteRenderbuffer(renderbuffer) }
    override fun bindRenderbuffer(target: Int, renderbuffer: Any?) { gl.bindRenderbuffer(target, renderbuffer) }
    override fun renderbufferStorage(target: Int, internalformat: Int, width: Int, height: Int) {
        gl.renderbufferStorage(target, internalformat, width, height)
    }

    private fun activeInfo(info: dynamic) =
        GLActiveInfo(info.name.unsafeCast<String>(), info.size.unsafeCast<Int>(), info.type.unsafeCast<Int>())

    /** Converts a WebGL query result, which may be a boolean, to an integer. */
    private fun toInt(value: dynamic): Int = when (value) {
        true -> 1
        false, null -> 0
        else -> value.unsafeCast<Int>()
    }
}
