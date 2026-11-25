package com.aitorgf.threekt.renderers.webgl

import com.aitorgf.threekt.materials.GLSLVersion
import com.aitorgf.threekt.materials.ShaderMaterial

/**
 * Linked shader program with the locations of its active attributes and uniforms. Array uniforms are
 * listed under their name without the `[0]` suffix.
 */
internal class WebGLProgram(private val gl: GLContext, vertexSource: String, fragmentSource: String) {
    /** Active uniform with its location and GLSL type constant. */
    class UniformInfo(val name: String, val location: Any?, val type: Int, val size: Int)

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
            name to UniformInfo(name, gl.getUniformLocation(handle, info.name), info.type, info.size)
        }
    }

    companion object {
        /** Adds the declarations supplied by Three.js for custom shaders. */
        fun shaderMaterialSources(material: ShaderMaterial): Pair<String, String> {
            val prefix = """
                #version 300 es
                precision highp float;
                precision highp int;
                uniform mat4 viewMatrix;
                uniform vec3 cameraPosition;
                uniform bool isOrthographic;
            """.trimIndent() + "\n"
            val vertexPrefix = """
                #define attribute in
                #define varying out
                #define texture2D texture
                uniform mat4 modelMatrix, modelViewMatrix, projectionMatrix;
                uniform mat3 normalMatrix;
                in vec3 position, normal;
                in vec2 uv;
            """.trimIndent() + "\n"
            val color = if (material.vertexColors) "#define USE_COLOR\nin vec3 color;\n" else ""
            val fragmentPrefix = """
                #define varying in
                #define gl_FragDepthEXT gl_FragDepth
                #define texture2D texture
                #define textureCube texture
                #define texture2DProj textureProj
                #define texture2DLodEXT textureLod
                #define texture2DProjLodEXT textureProjLod
                #define textureCubeLodEXT textureLod
                #define texture2DGradEXT textureGrad
                #define texture2DProjGradEXT textureProjGrad
                #define textureCubeGradEXT textureGrad
            """.trimIndent() + "\n"
            val fragmentOutput = if (material.glslVersion == GLSLVersion.GLSL3) "" else """
                layout(location = 0) out highp vec4 pc_fragColor;
                #define gl_FragColor pc_fragColor
            """.trimIndent() + "\n"
            return (prefix + vertexPrefix + color + material.vertexShader) to
                (prefix + fragmentPrefix + fragmentOutput + material.fragmentShader)
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
