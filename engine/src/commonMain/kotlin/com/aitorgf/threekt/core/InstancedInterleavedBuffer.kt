package com.aitorgf.threekt.core

import kotlin.js.JsExport

/**
 * Interleaved 32-bit float buffer whose strides advance once per [meshPerAttribute] instances, as
 * Three.js `InstancedInterleavedBuffer`. Threekt supports float storage only.
 */
@JsExport
class InstancedInterleavedBuffer(
    array: FloatArray,
    stride: Int,
    /** Number of consecutive instances that read the same stride. */
    var meshPerAttribute: Int = 1,
) : InterleavedBuffer(array, stride)
