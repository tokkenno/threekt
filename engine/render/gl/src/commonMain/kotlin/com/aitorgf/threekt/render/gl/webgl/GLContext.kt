package com.aitorgf.threekt.render.gl.webgl

import kotlin.js.JsExport

/**
 * The subset of the WebGL2 API that the renderer uses, implemented by each platform: `WebGL2Context` in
 * the browser and `GLES30Context` on the JVM. Methods keep their WebGL2 names and enumeration values
 * from [GL]; they differ only where WebGL2 is untyped. Object handles are opaque values created by the
 * same context, and null stands for "no object". Parameters that WebGL2 reports as booleans, such as
 * [GL.COMPILE_STATUS], are reported as `1` or `0`.
 */
@JsExport
interface GLContext {
    /** Width of the drawing buffer in pixels. */
    val drawingBufferWidth: Int
    /** Height of the drawing buffer in pixels. */
    val drawingBufferHeight: Int

    /** Requests a drawing buffer of [width] × [height] pixels, for example by resizing a canvas. */
    fun resizeDrawingBuffer(width: Int, height: Int)

    /** Enables extension [name], such as `EXT_texture_filter_anisotropic`. Returns whether it is available. */
    fun hasExtension(name: String): Boolean

    fun getError(): Int
    fun getParameterInt(pname: Int): Int
    fun getParameterFloat(pname: Int): Double
    /** Copies an integer-vector parameter, such as [GL.VIEWPORT], into [target] and returns it. */
    fun getParameterInts(pname: Int, target: IntArray): IntArray
    /** Returns the object bound for a binding parameter, such as [GL.DRAW_FRAMEBUFFER_BINDING]. */
    fun getParameterHandle(pname: Int): Any?

    fun enable(cap: Int)
    fun disable(cap: Int)
    fun viewport(x: Int, y: Int, width: Int, height: Int)
    fun scissor(x: Int, y: Int, width: Int, height: Int)
    fun clearColor(red: Double, green: Double, blue: Double, alpha: Double)
    fun clearDepth(depth: Double)
    fun clear(mask: Int)
    fun depthFunc(func: Int)
    fun depthMask(flag: Boolean)
    fun cullFace(mode: Int)
    fun frontFace(mode: Int)
    fun blendEquation(mode: Int)
    fun blendFunc(sfactor: Int, dfactor: Int)
    fun blendFuncSeparate(srcRGB: Int, dstRGB: Int, srcAlpha: Int, dstAlpha: Int)
    fun lineWidth(width: Double)

    fun createBuffer(): Any
    fun deleteBuffer(buffer: Any?)
    fun bindBuffer(target: Int, buffer: Any?)
    /** Uploads 32-bit floats, as `bufferData` with a `Float32Array`. */
    fun bufferFloatData(target: Int, data: FloatArray, usage: Int)
    /** Uploads bytes, as `bufferData` with a `Uint8Array` over the same bytes. */
    fun bufferByteData(target: Int, data: ByteArray, usage: Int)
    /** Uploads 16-bit integers, as `bufferData` with an `Int16Array`. */
    fun bufferShortData(target: Int, data: ShortArray, usage: Int)
    /** Uploads 32-bit integers, as `bufferData` with an `Int32Array`. */
    fun bufferIntData(target: Int, data: IntArray, usage: Int)
    /**
     * Replaces the contents of the bound buffer from byte [offset] with [length] 32-bit floats of [data]
     * starting at element [srcOffset], as the WebGL 2 `bufferSubData`.
     */
    fun bufferFloatSubData(target: Int, offset: Int, data: FloatArray, srcOffset: Int, length: Int)
    /** Replaces the contents of the bound buffer with 16-bit integers, as [bufferFloatSubData] does with floats. */
    fun bufferShortSubData(target: Int, offset: Int, data: ShortArray, srcOffset: Int, length: Int)
    /** Replaces the contents of the bound buffer with 32-bit integers, as [bufferFloatSubData] does with floats. */
    fun bufferIntSubData(target: Int, offset: Int, data: IntArray, srcOffset: Int, length: Int)
    /** Replaces the contents of the bound buffer with bytes, as [bufferFloatSubData] does with floats. */
    fun bufferByteSubData(target: Int, offset: Int, data: ByteArray, srcOffset: Int, length: Int)

    fun enableVertexAttribArray(index: Int)
    fun disableVertexAttribArray(index: Int)
    fun vertexAttribPointer(index: Int, size: Int, type: Int, normalized: Boolean, stride: Int, offset: Int)
    fun vertexAttribIPointer(index: Int, size: Int, type: Int, stride: Int, offset: Int)
    fun drawArrays(mode: Int, first: Int, count: Int)
    fun drawElements(mode: Int, count: Int, type: Int, offset: Int)
    /** Sets how many instances share each value of the attribute at [index]; zero advances it per vertex. */
    fun vertexAttribDivisor(index: Int, divisor: Int)
    fun drawArraysInstanced(mode: Int, first: Int, count: Int, instanceCount: Int)
    fun drawElementsInstanced(mode: Int, count: Int, type: Int, offset: Int, instanceCount: Int)

    fun createShader(type: Int): Any
    fun shaderSource(shader: Any, source: String)
    fun compileShader(shader: Any)
    fun getShaderParameter(shader: Any, pname: Int): Int
    fun getShaderInfoLog(shader: Any): String
    fun deleteShader(shader: Any?)
    fun createProgram(): Any
    fun attachShader(program: Any, shader: Any)
    fun linkProgram(program: Any)
    fun getProgramParameter(program: Any, pname: Int): Int
    fun getProgramInfoLog(program: Any): String
    fun useProgram(program: Any?)
    fun deleteProgram(program: Any?)
    fun getActiveAttrib(program: Any, index: Int): GLActiveInfo
    fun getActiveUniform(program: Any, index: Int): GLActiveInfo
    /** Returns the attribute location, or `-1` when the program has no such active attribute. */
    fun getAttribLocation(program: Any, name: String): Int
    /** Returns the uniform location, or null when the program has no such active uniform. */
    fun getUniformLocation(program: Any, name: String): Any?

    fun uniform1f(location: Any?, x: Double)
    fun uniform1fv(location: Any?, value: FloatArray)
    fun uniform1i(location: Any?, x: Int)
    fun uniform1iv(location: Any?, value: IntArray)
    fun uniform1ui(location: Any?, x: Int)
    fun uniform2f(location: Any?, x: Double, y: Double)
    fun uniform2i(location: Any?, x: Int, y: Int)
    fun uniform3f(location: Any?, x: Double, y: Double, z: Double)
    fun uniform4f(location: Any?, x: Double, y: Double, z: Double, w: Double)
    fun uniform4fv(location: Any?, value: FloatArray)
    fun uniformMatrix3fv(location: Any?, transpose: Boolean, value: FloatArray)
    fun uniformMatrix4fv(location: Any?, transpose: Boolean, value: FloatArray)

    fun createTexture(): Any
    fun deleteTexture(texture: Any?)
    fun bindTexture(target: Int, texture: Any?)
    fun activeTexture(texture: Int)
    fun texParameteri(target: Int, pname: Int, param: Int)
    fun texParameterf(target: Int, pname: Int, param: Double)
    fun texStorage2D(target: Int, levels: Int, internalformat: Int, width: Int, height: Int)
    /**
     * Uploads a platform image, such as an `HTMLImageElement` or canvas in the browser. [flipY] replaces
     * WebGL's `UNPACK_FLIP_Y_WEBGL` state and flips only this upload. Throws
     * [UnsupportedOperationException] if the platform cannot read [source].
     */
    fun texImage2D(target: Int, level: Int, internalformat: Int, format: Int, type: Int, source: Any, flipY: Boolean)
    /** Uploads 32-bit float texels to a 3D or array texture, as `texImage3D` with a `Float32Array`. */
    fun texImage3D(
        target: Int, level: Int, internalformat: Int, width: Int, height: Int, depth: Int, border: Int,
        format: Int, type: Int, data: FloatArray,
    )
    /** Uploads or replaces tightly packed RGBA8 pixels. */
    fun texImage2DBytes(internalformat: Int, width: Int, height: Int, data: ByteArray)
    fun texSubImage2DBytes(x: Int, y: Int, width: Int, height: Int, data: ByteArray)
    fun copyTexSubImage2D(x: Int, y: Int, width: Int, height: Int)
    fun readPixelsFloat(x: Int, y: Int, width: Int, height: Int, target: FloatArray)
    fun generateMipmap(target: Int)

    fun createFramebuffer(): Any
    fun deleteFramebuffer(framebuffer: Any?)
    fun bindFramebuffer(target: Int, framebuffer: Any?)
    fun framebufferTexture2D(target: Int, attachment: Int, textarget: Int, texture: Any?, level: Int)
    fun framebufferRenderbuffer(target: Int, attachment: Int, renderbuffertarget: Int, renderbuffer: Any?)
    fun checkFramebufferStatus(target: Int): Int
    fun createRenderbuffer(): Any
    fun deleteRenderbuffer(renderbuffer: Any?)
    fun bindRenderbuffer(target: Int, renderbuffer: Any?)
    fun renderbufferStorage(target: Int, internalformat: Int, width: Int, height: Int)
}
