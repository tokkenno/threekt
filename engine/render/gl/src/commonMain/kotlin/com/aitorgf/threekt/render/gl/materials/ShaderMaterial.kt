package com.aitorgf.threekt.render.gl.materials

import com.aitorgf.threekt.core.Uniforms
import com.aitorgf.threekt.materials.Material
import kotlin.js.JsExport

/**
 * Draws with custom shaders and named [uniforms]. The renderer provides standard matrices, camera
 * uniforms, and the position, normal, UV and enabled vertex color attributes. Shader source is
 * supplied as constructor arguments in Threekt instead of a Three.js parameters object.
 */
@JsExport
open class ShaderMaterial(
    var vertexShader: String = "void main() { gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0); }",
    var fragmentShader: String = "void main() { gl_FragColor = vec4(1.0, 0.0, 0.0, 1.0); }",
    val uniforms: Uniforms = Uniforms(),
) : Material() {
    /**
     * The renderer compiles with GLSL ES 3.00. When the value is null or [GLSLVersion.GLSL1], it also
     * provides the legacy fragment output. [RawShaderMaterial] uses this as its exact version prefix.
     */
    var glslVersion: GLSLVersion? = null
}
