package com.aitorgf.threekt.core.buffers

import com.aitorgf.threekt.core.BufferAttribute
import com.aitorgf.threekt.math.*
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsName

@ExperimentalJsExport
expect class Float32BufferAttribute(
    itemSize: Int = 1,
    normalized: Boolean = true
): BufferAttribute {
    constructor(
        data: FloatArray,
        itemSize: Int = 1,
        normalized: Boolean = true
    )

    constructor(
        data: DoubleArray,
        itemSize: Int = 1,
        normalized: Boolean = true
    )

    fun set(source: Float32BufferAttribute): Float32BufferAttribute

    fun copyAt(index1: Int, attribute: Float32BufferAttribute, index2: Int): Float32BufferAttribute

    @ExperimentalJsExport
    @JsName("setColorsArray")
    fun set(colors: Array<Color>): Float32BufferAttribute

    @ExperimentalJsExport
    @JsName("setVector2Array")
    fun set(vectors: Array<Vector2>): Float32BufferAttribute

    @ExperimentalJsExport
    @JsName("setVector3Array")
    fun set(vectors: Array<Vector3>): Float32BufferAttribute

    @ExperimentalJsExport
    @JsName("setVector4Array")
    fun set(vectors: Array<Vector4>): Float32BufferAttribute

    @ExperimentalJsExport
    override fun apply(m: Matrix3): Float32BufferAttribute

    @ExperimentalJsExport
    override fun apply(m: Matrix4): Float32BufferAttribute

    @ExperimentalJsExport
    override fun applyNormalMatrix(m: Matrix3): Float32BufferAttribute

    @ExperimentalJsExport
    override fun transformDirection(m: Matrix4): Float32BufferAttribute

    fun getX(index: Int): Double

    fun setX(index: Int, x: Double): Float32BufferAttribute

    fun getY(index: Int): Double

    fun setY(index: Int, y: Double): Float32BufferAttribute

    fun getZ(index: Int): Double

    fun setZ(index: Int, z: Double): Float32BufferAttribute

    fun getW(index: Int): Double

    fun setW(index: Int, w: Double): Float32BufferAttribute

    fun setXY(index: Int, x: Double, y: Double): Float32BufferAttribute

    fun setXYZ(index: Int, x: Double, y: Double, z: Double): Float32BufferAttribute

    fun setXYZW(index: Int, x: Double, y: Double, z: Double, w: Double): Float32BufferAttribute

    override fun clone(): Float32BufferAttribute
}

