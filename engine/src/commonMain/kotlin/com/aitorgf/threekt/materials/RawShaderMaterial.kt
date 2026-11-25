package com.aitorgf.threekt.materials

import com.aitorgf.threekt.core.Uniforms
import kotlin.js.JsExport

/**
 * Compiles user shaders without renderer declarations. Declare any built-in uniforms the shaders use.
 * The renderer prepends a version directive only when [glslVersion] is set. Threekt takes the shader
 * strings and uniforms as constructor arguments.
 */
@JsExport
class RawShaderMaterial(
    vertexShader: String,
    fragmentShader: String,
    uniforms: Uniforms = Uniforms(),
) : ShaderMaterial(vertexShader, fragmentShader, uniforms)
