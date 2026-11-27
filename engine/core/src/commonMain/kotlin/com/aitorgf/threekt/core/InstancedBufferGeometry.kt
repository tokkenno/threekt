package com.aitorgf.threekt.core

import kotlin.js.JsExport

/**
 * Geometry drawn [instanceCount] times in one call, with per-instance data supplied by
 * [InstancedBufferAttribute]s or attributes of an [InstancedInterleavedBuffer], as Three.js
 * `InstancedBufferGeometry`.
 */
@JsExport
open class InstancedBufferGeometry : BufferGeometry() {
    /**
     * Number of instances to draw. The renderer also limits it to the instances that the instanced
     * attributes can supply. Defaults to [Int.MAX_VALUE], which stands for the Three.js `Infinity`.
     */
    var instanceCount = Int.MAX_VALUE

    /** Copies [instanceCount] as well when [source] is instanced. Returns this geometry. */
    override fun copy(source: BufferGeometry): InstancedBufferGeometry {
        super.copy(source)
        if (source is InstancedBufferGeometry) instanceCount = source.instanceCount
        return this
    }

    override fun clone(): InstancedBufferGeometry = InstancedBufferGeometry().copy(this)
}
