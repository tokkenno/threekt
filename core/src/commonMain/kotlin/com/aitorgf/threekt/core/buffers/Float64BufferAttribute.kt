package com.aitorgf.threekt.core.buffers

import com.aitorgf.threekt.core.BufferAttribute
import com.aitorgf.threekt.math.*
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsName

@ExperimentalJsExport
expect class Float64BufferAttribute(
    itemSize: Int = 1,
    normalized: Boolean = true
): BufferAttribute {
    constructor(
        data: DoubleArray,
        itemSize: Int = 1,
        normalized: Boolean = true
    )

    fun set(source: Float64BufferAttribute): Float64BufferAttribute

    fun copyAt(index1: Int, attribute: Float64BufferAttribute, index2: Int): Float64BufferAttribute

    @ExperimentalJsExport
    @JsName("setColorsArray")
    fun set(colors: Array<Color>): Float64BufferAttribute

    @ExperimentalJsExport
    @JsName("setVector2Array")
    fun set(vectors: Array<Vector2>): Float64BufferAttribute

    @ExperimentalJsExport
    @JsName("setVector3Array")
    fun set(vectors: Array<Vector3>): Float64BufferAttribute

    @ExperimentalJsExport
    @JsName("setVector4Array")
    fun set(vectors: Array<Vector4>): Float64BufferAttribute

    @ExperimentalJsExport
    override fun apply(m: Matrix3): Float64BufferAttribute

    @ExperimentalJsExport
    override fun apply(m: Matrix4): Float64BufferAttribute

    @ExperimentalJsExport
    override fun applyNormalMatrix(m: Matrix3): Float64BufferAttribute

    @ExperimentalJsExport
    override fun transformDirection(m: Matrix4): Float64BufferAttribute

    fun getX(index: Int): Double

    fun setX(index: Int, x: Double): Float64BufferAttribute

    fun getY(index: Int): Double

    fun setY(index: Int, y: Double): Float64BufferAttribute

    fun getZ(index: Int): Double

    fun setZ(index: Int, z: Double): Float64BufferAttribute

    fun getW(index: Int): Double

    fun setW(index: Int, w: Double): Float64BufferAttribute

    fun setXY(index: Int, x: Double, y: Double): Float64BufferAttribute

    fun setXYZ(index: Int, x: Double, y: Double, z: Double): Float64BufferAttribute

    fun setXYZW(index: Int, x: Double, y: Double, z: Double, w: Double): Float64BufferAttribute

    override fun clone(): Float64BufferAttribute
}

