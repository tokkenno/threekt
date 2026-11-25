package com.aitorgf.threekt.renderers.webgl

import java.awt.image.BufferedImage
import org.lwjgl.opengles.GLES20
import org.lwjgl.opengles.GLES30
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil

/**
 * [GLContext] backed by the OpenGL ES 3.0 context that is current on the calling thread, through LWJGL.
 * OpenGL ES 3.0 matches WebGL2, so shaders written for the browser compile unchanged. The application
 * creates the context, for example with GLFW, and calls `GLES.createCapabilities()` before constructing
 * this object. Every call must happen on the thread where the context is current.
 *
 * The drawing buffer belongs to the window, so [resizeDrawingBuffer] only records the size that the
 * application reports, normally the window's framebuffer size. Images are uploaded from
 * [BufferedImage]s. Handles are OpenGL object names.
 */
class GLES30Context(width: Int, height: Int) : GLContext {
    override var drawingBufferWidth = width
        private set
    override var drawingBufferHeight = height
        private set

    private val extensions: Set<String> by lazy {
        (0 until GLES20.glGetInteger(GLES30.GL_NUM_EXTENSIONS)).mapNotNull { GLES30.glGetStringi(GLES20.GL_EXTENSIONS, it) }.toSet()
    }

    override fun resizeDrawingBuffer(width: Int, height: Int) {
        drawingBufferWidth = width
        drawingBufferHeight = height
    }

    /** Reads the currently bound read framebuffer into an independent top-down image. */
    fun readImage(includeAlpha: Boolean = true): BufferedImage {
        val width = drawingBufferWidth
        val height = drawingBufferHeight
        require(width > 0 && height > 0 && width.toLong() * height <= Int.MAX_VALUE / 4)
        val pixels = MemoryUtil.memAlloc(width * height * 4)
        try {
            GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, pixels)
            val image = BufferedImage(width, height, if (includeAlpha) BufferedImage.TYPE_INT_ARGB else BufferedImage.TYPE_INT_RGB)
            val row = IntArray(width)
            for (y in 0 until height) {
                val start = (height - 1 - y) * width * 4
                for (x in 0 until width) {
                    val offset = start + x * 4
                    val red = pixels[offset].toInt() and 255
                    val green = pixels[offset + 1].toInt() and 255
                    val blue = pixels[offset + 2].toInt() and 255
                    val alpha = pixels[offset + 3].toInt() and 255
                    row[x] = (alpha shl 24) or (red shl 16) or (green shl 8) or blue
                }
                image.setRGB(0, y, width, 1, row, 0, width)
            }
            return image
        } finally {
            MemoryUtil.memFree(pixels)
        }
    }

    override fun hasExtension(name: String): Boolean = "GL_$name" in extensions

    override fun getError(): Int = GLES20.glGetError()
    override fun getParameterInt(pname: Int): Int = GLES20.glGetInteger(pname)
    override fun getParameterFloat(pname: Int): Double = GLES20.glGetFloat(pname).toDouble()
    override fun getParameterInts(pname: Int, target: IntArray): IntArray {
        GLES20.glGetIntegerv(pname, target)
        return target
    }
    override fun getParameterHandle(pname: Int): Any? = GLES20.glGetInteger(pname).takeIf { it != 0 }

    override fun enable(cap: Int) = GLES20.glEnable(cap)
    override fun disable(cap: Int) = GLES20.glDisable(cap)
    override fun viewport(x: Int, y: Int, width: Int, height: Int) = GLES20.glViewport(x, y, width, height)
    override fun scissor(x: Int, y: Int, width: Int, height: Int) = GLES20.glScissor(x, y, width, height)
    override fun clearColor(red: Double, green: Double, blue: Double, alpha: Double) =
        GLES20.glClearColor(red.toFloat(), green.toFloat(), blue.toFloat(), alpha.toFloat())
    override fun clearDepth(depth: Double) = GLES20.glClearDepthf(depth.toFloat())
    override fun clear(mask: Int) = GLES20.glClear(mask)
    override fun depthFunc(func: Int) = GLES20.glDepthFunc(func)
    override fun depthMask(flag: Boolean) = GLES20.glDepthMask(flag)
    override fun cullFace(mode: Int) = GLES20.glCullFace(mode)
    override fun frontFace(mode: Int) = GLES20.glFrontFace(mode)
    override fun blendEquation(mode: Int) = GLES20.glBlendEquation(mode)
    override fun blendFunc(sfactor: Int, dfactor: Int) = GLES20.glBlendFunc(sfactor, dfactor)
    override fun blendFuncSeparate(srcRGB: Int, dstRGB: Int, srcAlpha: Int, dstAlpha: Int) =
        GLES20.glBlendFuncSeparate(srcRGB, dstRGB, srcAlpha, dstAlpha)

    override fun lineWidth(width: Double) = GLES20.glLineWidth(width.toFloat())

    override fun createBuffer(): Any = GLES20.glGenBuffers()
    override fun deleteBuffer(buffer: Any?) { name(buffer)?.let(GLES20::glDeleteBuffers) }
    override fun bindBuffer(target: Int, buffer: Any?) = GLES20.glBindBuffer(target, name(buffer) ?: 0)
    override fun bufferFloatData(target: Int, data: FloatArray, usage: Int) = GLES20.glBufferData(target, data, usage)
    override fun bufferByteData(target: Int, data: ByteArray, usage: Int) {
        val buffer = MemoryUtil.memAlloc(data.size)
        try {
            buffer.put(data).flip()
            GLES20.glBufferData(target, buffer, usage)
        } finally {
            MemoryUtil.memFree(buffer)
        }
    }
    override fun bufferShortData(target: Int, data: ShortArray, usage: Int) = GLES20.glBufferData(target, data, usage)
    override fun bufferShortSubData(target: Int, offset: Int, data: ShortArray) =
        GLES20.glBufferSubData(target, offset.toLong(), data)
    override fun bufferIntData(target: Int, data: IntArray, usage: Int) = GLES20.glBufferData(target, data, usage)
    override fun bufferFloatSubData(target: Int, offset: Int, data: FloatArray) =
        GLES20.glBufferSubData(target, offset.toLong(), data)
    override fun bufferByteSubData(target: Int, offset: Int, data: ByteArray) {
        val buffer = MemoryUtil.memAlloc(data.size)
        try {
            buffer.put(data).flip()
            GLES20.glBufferSubData(target, offset.toLong(), buffer)
        } finally {
            MemoryUtil.memFree(buffer)
        }
    }

    override fun enableVertexAttribArray(index: Int) = GLES20.glEnableVertexAttribArray(index)
    override fun disableVertexAttribArray(index: Int) = GLES20.glDisableVertexAttribArray(index)
    override fun vertexAttribPointer(index: Int, size: Int, type: Int, normalized: Boolean, stride: Int, offset: Int) =
        GLES20.glVertexAttribPointer(index, size, type, normalized, stride, offset.toLong())
    override fun vertexAttribIPointer(index: Int, size: Int, type: Int, stride: Int, offset: Int) =
        GLES30.glVertexAttribIPointer(index, size, type, stride, offset.toLong())
    override fun drawArrays(mode: Int, first: Int, count: Int) = GLES20.glDrawArrays(mode, first, count)
    override fun drawElements(mode: Int, count: Int, type: Int, offset: Int) =
        GLES20.glDrawElements(mode, count, type, offset.toLong())

    override fun createShader(type: Int): Any = GLES20.glCreateShader(type).also { check(it != 0) { "Could not create a shader." } }
    override fun shaderSource(shader: Any, source: String) = GLES20.glShaderSource(shader as Int, source)
    override fun compileShader(shader: Any) = GLES20.glCompileShader(shader as Int)
    override fun getShaderParameter(shader: Any, pname: Int): Int = GLES20.glGetShaderi(shader as Int, pname)
    override fun getShaderInfoLog(shader: Any): String = GLES20.glGetShaderInfoLog(shader as Int)
    override fun deleteShader(shader: Any?) { name(shader)?.let(GLES20::glDeleteShader) }
    override fun createProgram(): Any = GLES20.glCreateProgram().also { check(it != 0) { "Could not create a program." } }
    override fun attachShader(program: Any, shader: Any) = GLES20.glAttachShader(program as Int, shader as Int)
    override fun linkProgram(program: Any) = GLES20.glLinkProgram(program as Int)
    override fun getProgramParameter(program: Any, pname: Int): Int = GLES20.glGetProgrami(program as Int, pname)
    override fun getProgramInfoLog(program: Any): String = GLES20.glGetProgramInfoLog(program as Int)
    override fun useProgram(program: Any?) = GLES20.glUseProgram(name(program) ?: 0)
    override fun deleteProgram(program: Any?) { name(program)?.let(GLES20::glDeleteProgram) }
    override fun getActiveAttrib(program: Any, index: Int): GLActiveInfo = MemoryStack.stackPush().use { stack ->
        val size = stack.mallocInt(1)
        val type = stack.mallocInt(1)
        val name = GLES20.glGetActiveAttrib(program as Int, index, size, type)
        GLActiveInfo(name, size[0], type[0])
    }
    override fun getActiveUniform(program: Any, index: Int): GLActiveInfo = MemoryStack.stackPush().use { stack ->
        val size = stack.mallocInt(1)
        val type = stack.mallocInt(1)
        val name = GLES20.glGetActiveUniform(program as Int, index, size, type)
        GLActiveInfo(name, size[0], type[0])
    }
    override fun getAttribLocation(program: Any, name: String): Int = GLES20.glGetAttribLocation(program as Int, name)
    override fun getUniformLocation(program: Any, name: String): Any? =
        GLES20.glGetUniformLocation(program as Int, name).takeIf { it >= 0 }

    // As in WebGL, a null location is ignored.
    override fun uniform1f(location: Any?, x: Double) { name(location)?.let { GLES20.glUniform1f(it, x.toFloat()) } }
    override fun uniform1fv(location: Any?, value: FloatArray) { name(location)?.let { GLES20.glUniform1fv(it, value) } }
    override fun uniform1i(location: Any?, x: Int) { name(location)?.let { GLES20.glUniform1i(it, x) } }
    override fun uniform1iv(location: Any?, value: IntArray) { name(location)?.let { GLES20.glUniform1iv(it, value) } }
    override fun uniform1ui(location: Any?, x: Int) { name(location)?.let { GLES30.glUniform1ui(it, x) } }
    override fun uniform2f(location: Any?, x: Double, y: Double) {
        name(location)?.let { GLES20.glUniform2f(it, x.toFloat(), y.toFloat()) }
    }
    override fun uniform2i(location: Any?, x: Int, y: Int) { name(location)?.let { GLES20.glUniform2i(it, x, y) } }
    override fun uniform3f(location: Any?, x: Double, y: Double, z: Double) {
        name(location)?.let { GLES20.glUniform3f(it, x.toFloat(), y.toFloat(), z.toFloat()) }
    }
    override fun uniform4f(location: Any?, x: Double, y: Double, z: Double, w: Double) {
        name(location)?.let { GLES20.glUniform4f(it, x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat()) }
    }
    override fun uniformMatrix3fv(location: Any?, transpose: Boolean, value: FloatArray) {
        name(location)?.let { GLES20.glUniformMatrix3fv(it, transpose, value) }
    }
    override fun uniformMatrix4fv(location: Any?, transpose: Boolean, value: FloatArray) {
        name(location)?.let { GLES20.glUniformMatrix4fv(it, transpose, value) }
    }

    override fun createTexture(): Any = GLES20.glGenTextures()
    override fun deleteTexture(texture: Any?) { name(texture)?.let(GLES20::glDeleteTextures) }
    override fun bindTexture(target: Int, texture: Any?) = GLES20.glBindTexture(target, name(texture) ?: 0)
    override fun activeTexture(texture: Int) = GLES20.glActiveTexture(texture)
    override fun texParameteri(target: Int, pname: Int, param: Int) = GLES20.glTexParameteri(target, pname, param)
    override fun texParameterf(target: Int, pname: Int, param: Double) = GLES20.glTexParameterf(target, pname, param.toFloat())
    override fun texStorage2D(target: Int, levels: Int, internalformat: Int, width: Int, height: Int) =
        GLES30.glTexStorage2D(target, levels, internalformat, width, height)
    override fun texImage2D(target: Int, level: Int, internalformat: Int, format: Int, type: Int, source: Any, flipY: Boolean) {
        if (source !is BufferedImage || format != GL.RGBA || type != GL.UNSIGNED_BYTE) {
            throw UnsupportedOperationException("GLES30Context uploads only BufferedImage sources as RGBA bytes.")
        }
        val width = source.width
        val height = source.height
        val pixels = MemoryUtil.memAlloc(width * height * 4)
        try {
            for (row in 0 until height) {
                val sourceRow = if (flipY) height - 1 - row else row
                for (column in 0 until width) {
                    val argb = source.getRGB(column, sourceRow)
                    pixels.put((argb shr 16).toByte()).put((argb shr 8).toByte()).put(argb.toByte()).put((argb ushr 24).toByte())
                }
            }
            pixels.flip()
            GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1)
            GLES20.glTexImage2D(target, level, internalformat, width, height, 0, format, type, pixels)
        } finally {
            MemoryUtil.memFree(pixels)
        }
    }
    override fun texImage3D(
        target: Int, level: Int, internalformat: Int, width: Int, height: Int, depth: Int, border: Int,
        format: Int, type: Int, data: FloatArray,
    ) {
        val buffer = MemoryUtil.memAllocFloat(data.size)
        try {
            buffer.put(data).flip()
            GLES30.glTexImage3D(target, level, internalformat, width, height, depth, border, format, type, buffer)
        } finally {
            MemoryUtil.memFree(buffer)
        }
    }
    override fun generateMipmap(target: Int) = GLES20.glGenerateMipmap(target)

    override fun createFramebuffer(): Any = GLES20.glGenFramebuffers()
    override fun deleteFramebuffer(framebuffer: Any?) { name(framebuffer)?.let(GLES20::glDeleteFramebuffers) }
    override fun bindFramebuffer(target: Int, framebuffer: Any?) = GLES20.glBindFramebuffer(target, name(framebuffer) ?: 0)
    override fun framebufferTexture2D(target: Int, attachment: Int, textarget: Int, texture: Any?, level: Int) =
        GLES20.glFramebufferTexture2D(target, attachment, textarget, name(texture) ?: 0, level)
    override fun framebufferRenderbuffer(target: Int, attachment: Int, renderbuffertarget: Int, renderbuffer: Any?) =
        GLES20.glFramebufferRenderbuffer(target, attachment, renderbuffertarget, name(renderbuffer) ?: 0)
    override fun checkFramebufferStatus(target: Int): Int = GLES20.glCheckFramebufferStatus(target)
    override fun createRenderbuffer(): Any = GLES20.glGenRenderbuffers()
    override fun deleteRenderbuffer(renderbuffer: Any?) { name(renderbuffer)?.let(GLES20::glDeleteRenderbuffers) }
    override fun bindRenderbuffer(target: Int, renderbuffer: Any?) = GLES20.glBindRenderbuffer(target, name(renderbuffer) ?: 0)
    override fun renderbufferStorage(target: Int, internalformat: Int, width: Int, height: Int) =
        GLES20.glRenderbufferStorage(target, internalformat, width, height)

    /** Returns the OpenGL name of a handle created by this context, or null for no object. */
    private fun name(handle: Any?): Int? = handle as Int?
}
