@file:OptIn(ThreektRendererApi::class)

package com.aitorgf.threekt.core

import com.aitorgf.threekt.ThreektRendererApi
import kotlin.js.JsExport
import kotlin.js.JsName

/**
 * Interleaved buffer whose strides advance once per [meshPerAttribute] instances instead of once per
 * vertex, as Three.js `InstancedInterleavedBuffer`. Like [InterleavedBuffer], it has one constructor per
 * array type, exported to JavaScript as named factories such as
 * `InstancedInterleavedBuffer.fromFloat32Array`.
 */
@JsExport
class InstancedInterleavedBuffer : InterleavedBuffer {
    /** Number of consecutive instances that read the same stride. */
    var meshPerAttribute: Int

    internal constructor(storage: BufferAttribute, stride: Int, meshPerAttribute: Int = 1) : super(storage, stride) {
        this.meshPerAttribute = meshPerAttribute
    }

    /** Retains a 32-bit float array. */
    @JsName("fromFloat32Array")
    constructor(array: FloatArray, stride: Int, meshPerAttribute: Int = 1) :
        this(Float32BufferAttribute(array, 1), stride, meshPerAttribute)

    /** Retains an unsigned byte array, represented by an `Int8Array` in JavaScript. */
    @JsName("fromUint8Array")
    constructor(array: ByteArray, stride: Int, meshPerAttribute: Int = 1) :
        this(Uint8BufferAttribute(array, 1), stride, meshPerAttribute)

    /** Retains a signed 16-bit array. */
    @JsName("fromInt16Array")
    constructor(array: ShortArray, stride: Int, meshPerAttribute: Int = 1) :
        this(Int16BufferAttribute(array, 1), stride, meshPerAttribute)

    /** Retains a signed 32-bit array. */
    @JsName("fromInt32Array")
    constructor(array: IntArray, stride: Int, meshPerAttribute: Int = 1) :
        this(Int32BufferAttribute(array, 1), stride, meshPerAttribute)

    /** Copies [meshPerAttribute] as well when [source] is instanced. Returns this buffer. */
    override fun copy(source: InterleavedBuffer): InstancedInterleavedBuffer {
        super.copy(source)
        if (source is InstancedInterleavedBuffer) meshPerAttribute = source.meshPerAttribute
        return this
    }

    override fun clone(): InstancedInterleavedBuffer =
        InstancedInterleavedBuffer(storage.clone(), stride, meshPerAttribute).also { it.setUsage(usage) }
}
