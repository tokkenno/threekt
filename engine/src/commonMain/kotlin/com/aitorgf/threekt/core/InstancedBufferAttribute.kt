package com.aitorgf.threekt.core

import kotlin.js.JsExport

/**
 * Float attribute that advances once per [meshPerAttribute] instances instead of once per vertex, as
 * Three.js `InstancedBufferAttribute`. Three.js accepts any typed array; Threekt stores 32-bit floats,
 * which covers instance transforms, colors and offsets.
 */
@JsExport
open class InstancedBufferAttribute(
    array: FloatArray,
    itemSize: Int,
    normalized: Boolean = false,
    /** Number of consecutive instances that read the same item. */
    var meshPerAttribute: Int = 1,
) : Float32BufferAttribute(array, itemSize, normalized) {
    override fun clone(): InstancedBufferAttribute =
        InstancedBufferAttribute(array.copyOf(), itemSize, normalized, meshPerAttribute).also { it.copySettings(this) }
}
