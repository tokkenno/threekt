package com.aitorgf.threekt.renderers.webgl

/**
 * Linked shader program with the locations of its active attributes and uniforms. Array uniforms are
 * listed under their name without the `[0]` suffix.
 */
internal class WebGLProgram(private val gl: GLContext, vertexSource: String, fragmentSource: String) {
    /** Active uniform with its location and GLSL type constant. */
    class UniformInfo(val name: String, val location: Any?, val type: Int)

    val handle: Any = link(vertexSource, fragmentSource)

    /** Locations of the active vertex attributes by name. */
    val attributes: Map<String, Int>

    /** Active uniforms by name. */
    val uniforms: Map<String, UniformInfo>

    init {
        val attributeCount = gl.getProgramParameter(handle, GL.ACTIVE_ATTRIBUTES)
        attributes = (0 until attributeCount).associate { index ->
            val name = gl.getActiveAttrib(handle, index).name
            name to gl.getAttribLocation(handle, name)
        }.filterValues { it >= 0 }
        val uniformCount = gl.getProgramParameter(handle, GL.ACTIVE_UNIFORMS)
        uniforms = (0 until uniformCount).associate { index ->
            val info = gl.getActiveUniform(handle, index)
            val name = info.name.removeSuffix("[0]")
            name to UniformInfo(name, gl.getUniformLocation(handle, info.name), info.type)
        }
    }

    /** Deletes the program. */
    fun dispose() {
        gl.deleteProgram(handle)
    }

    private fun link(vertexSource: String, fragmentSource: String): Any {
        val vertex = compile(GL.VERTEX_SHADER, vertexSource)
        try {
            val fragment = compile(GL.FRAGMENT_SHADER, fragmentSource)
            try {
                val program = gl.createProgram()
                gl.attachShader(program, vertex)
                gl.attachShader(program, fragment)
                gl.linkProgram(program)
                if (gl.getProgramParameter(program, GL.LINK_STATUS) == 0) {
                    val log = gl.getProgramInfoLog(program)
                    gl.deleteProgram(program)
                    error("Could not link the WebGL program: $log")
                }
                return program
            } finally {
                gl.deleteShader(fragment)
            }
        } finally {
            gl.deleteShader(vertex)
        }
    }

    private fun compile(type: Int, source: String): Any {
        val shader = gl.createShader(type)
        gl.shaderSource(shader, source)
        gl.compileShader(shader)
        if (gl.getShaderParameter(shader, GL.COMPILE_STATUS) == 0) {
            val log = gl.getShaderInfoLog(shader)
            gl.deleteShader(shader)
            error("Could not compile a WebGL shader: $log")
        }
        return shader
    }
}
