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
    /** Copies [meshPerAttribute] as well when [source] is instanced. Returns this attribute. */
    override fun copy(source: BufferAttribute): InstancedBufferAttribute {
        super.copy(source)
        if (source is InstancedBufferAttribute) meshPerAttribute = source.meshPerAttribute
        return this
    }

    override fun clone(): InstancedBufferAttribute =
        InstancedBufferAttribute(array, itemSize, normalized, meshPerAttribute).copy(this)
}
