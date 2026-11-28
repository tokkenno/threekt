package com.aitorgf.threekt.render.gl.materials

import kotlin.js.JsExport

/**
 * GLSL dialect of a shader material, as the Three.js `GLSL1` and `GLSL3` constants. Renderers prepend
 * `#version [version]` to raw shaders when one is set.
 */
@JsExport
enum class GLSLVersion(val version: String) {
    /** GLSL ES 1.00, with `attribute`, `varying`, and `gl_FragColor`. */
    GLSL1("100"),
    /** GLSL ES 3.00, with `in`, `out`, integer types, and `gl_VertexID`. */
    GLSL3("300 es"),
}
