package com.aitorgf.threekt.core

import kotlin.js.JsExport

/**
 * Mutable shader input. Renderers read [value] on every draw, so changing it takes effect on the next
 * frame. Supported values are numbers, booleans, `Vector2`, `Vector3`, `Vector4`, `Color`, `Matrix3`,
 * `Matrix4`, and textures, matching the uniform type declared in the shader.
 */
@JsExport
class Uniform(var value: Any)
