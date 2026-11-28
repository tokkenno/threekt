package com.aitorgf.threekt.render.gl.webgl

import com.aitorgf.threekt.render.gl.materials.GLSLVersion
import com.aitorgf.threekt.render.gl.materials.ShaderMaterial

/**
 * Linked shader program with the locations of its active attributes and uniforms. Array uniforms are
 * listed under their name without the `[0]` suffix.
 */
internal class WebGLProgram(private val gl: GLContext, vertexSource: String, fragmentSource: String) {
    /** Active uniform with its GLSL type and an array length when it refers to a whole array. */
    class UniformInfo(val name: String, val location: Any?, val type: Int, val arraySize: Int?)

    val handle: Any = link(vertexSource, fragmentSource)

    /** Locations of the active vertex attributes by name. */
    val attributes: Map<String, Int>

    /**
     * Number of consecutive locations that each active attribute occupies: 4 for a `mat4`, 3 for a `mat3`,
     * 2 for a `mat2` and 1 otherwise, as Three.js `locationSize`.
     */
    val attributeLocationSizes: Map<String, Int>

    /** Active uniforms by name. */
    val uniforms: Map<String, UniformInfo>

    init {
        val attributeCount = gl.getProgramParameter(handle, GL.ACTIVE_ATTRIBUTES)
        val activeAttributes = (0 until attributeCount).map { gl.getActiveAttrib(handle, it) }
        attributes = activeAttributes.associate { info -> info.name to gl.getAttribLocation(handle, info.name) }
            .filterValues { it >= 0 }
        attributeLocationSizes = activeAttributes.associate { info ->
            info.name to when (info.type) {
                GL.FLOAT_MAT4 -> 4
                GL.FLOAT_MAT3 -> 3
                GL.FLOAT_MAT2 -> 2
                else -> 1
            }
        }
        val uniformCount = gl.getProgramParameter(handle, GL.ACTIVE_UNIFORMS)
        uniforms = (0 until uniformCount).associate { index ->
            val info = gl.getActiveUniform(handle, index)
            val name = info.name.removeSuffix("[0]")
            val arraySize = info.size.takeIf { name != info.name }
            name to UniformInfo(name, gl.getUniformLocation(handle, info.name), info.type, arraySize)
        }
    }

    /** Deletes the program. */
    fun dispose() {
        gl.deleteProgram(handle)
    }

    companion object {
        /** Prepends the GLSL 3 compatibility macros and built-in declarations for ShaderMaterial. */
        fun shaderMaterialSource(
            material: ShaderMaterial,
            instancing: Boolean = false,
            instancingColor: Boolean = false,
        ): Pair<String, String> {
            val vertexPrefix = """
                #version 300 es
                #define attribute in
                #define varying out
                #define texture2D texture
                precision highp float;
                precision highp int;
                uniform mat4 modelMatrix;
                uniform mat4 modelViewMatrix;
                uniform mat4 projectionMatrix;
                uniform mat4 viewMatrix;
                uniform mat3 normalMatrix;
                uniform vec3 cameraPosition;
                uniform bool isOrthographic;
                attribute vec3 position;
                attribute vec3 normal;
                attribute vec2 uv;
            """.trimIndent()
            val color = buildString {
                if (instancing) append("#define USE_INSTANCING\nattribute mat4 instanceMatrix;\n")
                if (instancingColor) append("#define USE_INSTANCING_COLOR\nattribute vec3 instanceColor;\n")
                if (material.vertexColors) append("#define USE_COLOR\nattribute vec3 color;\n")
            }
            val fragmentPrefix = """
                #version 300 es
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
                precision highp float;
                precision highp int;
                uniform mat4 viewMatrix;
                uniform vec3 cameraPosition;
                uniform bool isOrthographic;
            """.trimIndent()
            val output = if (material.glslVersion == GLSLVersion.GLSL3) "" else
                "layout(location = 0) out highp vec4 pc_fragColor;\n#define gl_FragColor pc_fragColor\n"
            val useColor = if (material.vertexColors) "#define USE_COLOR\n" else ""
            return "$vertexPrefix\n$color${material.vertexShader}" to
                "$fragmentPrefix\n$output$useColor${material.fragmentShader}"
        }
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
