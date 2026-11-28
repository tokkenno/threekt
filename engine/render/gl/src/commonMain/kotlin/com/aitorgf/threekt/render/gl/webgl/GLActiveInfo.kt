package com.aitorgf.threekt.render.gl.webgl

import kotlin.js.JsExport

/** Name, array size, and GLSL type of an active attribute or uniform, as in `WebGLActiveInfo`. */
@JsExport
class GLActiveInfo(val name: String, val size: Int, val type: Int)
