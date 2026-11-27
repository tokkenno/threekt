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
 * Threekt takes the shaders as constructor arguments instead of a Three.js parameters object.
 */
@JsExport
class RawShaderMaterial(
    vertexShader: String,
    fragmentShader: String,
    uniforms: Uniforms = Uniforms(),
) : ShaderMaterial(vertexShader, fragmentShader, uniforms)
