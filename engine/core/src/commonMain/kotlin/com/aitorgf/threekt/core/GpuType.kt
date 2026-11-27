package com.aitorgf.threekt.core

import kotlin.js.JsExport

/**
 * Shader input type used when binding integer-backed vertex data. Replaces the numeric Three.js
 * `FloatType` and `IntType` values on [BufferAttribute.gpuType].
 */
@JsExport
enum class GpuType {
    /** Converts the stored components to floats and applies normalization if requested. */
    Float,
    /** Passes the stored integer components directly to an integer shader input. */
    Int,
}
