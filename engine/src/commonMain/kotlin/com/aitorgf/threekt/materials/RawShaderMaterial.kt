package com.aitorgf.threekt.materials

import com.aitorgf.threekt.core.Uniforms
import kotlin.js.JsExport

/**
 * Material drawn with user-supplied shaders that are compiled exactly as given, without prepended
 * definitions. The shaders read geometry attributes by name, the [uniforms], and the built-in uniforms
 * `modelMatrix`, `modelViewMatrix`, `projectionMatrix`, `viewMatrix`, `normalMatrix`, and
 * `cameraPosition` when they declare them. The fragment color is written to the target without color
 * space conversion.
 *
 * Three.js derives this class from `ShaderMaterial` and takes a parameters object. Threekt does not
 * provide `ShaderMaterial` yet and takes the shaders as constructor arguments.
 */
@JsExport
class RawShaderMaterial(
    var vertexShader: String,
    var fragmentShader: String,
    val uniforms: Uniforms = Uniforms(),
) : Material() {
    /**
     * GLSL dialect of the shaders, or null when the shaders declare their own `#version` or use the
     * default GLSL ES 1.00. When set, `#version` followed by [GLSLVersion.version] is prepended to both
     * shaders.
     */
    var glslVersion: GLSLVersion? = null
}
