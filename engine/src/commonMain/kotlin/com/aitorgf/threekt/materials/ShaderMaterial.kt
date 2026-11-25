package com.aitorgf.threekt.materials

import com.aitorgf.threekt.core.Uniforms
import kotlin.js.JsExport

/**
 * Material with user-defined GLSL and renderer-provided attributes and uniforms.
 * Threekt takes shader strings and uniforms as constructor arguments and supports direct GLSL sources.
 */
@JsExport
open class ShaderMaterial(
    var vertexShader: String,
    var fragmentShader: String,
    val uniforms: Uniforms = Uniforms(),
) : Material() {
    /**
     * GLSL dialect. The renderer compiles GLSL ES 3.00 and supplies compatibility aliases for GLSL ES
     * 1.00 syntax by default. Use [GLSLVersion.GLSL3] to declare your own fragment outputs.
     */
    var glslVersion: GLSLVersion? = null
}
