package com.aitorgf.threekt.core

import com.aitorgf.threekt.core.buffers.Float32BufferAttribute
import com.aitorgf.threekt.core.buffers.Float64BufferAttribute
import com.aitorgf.threekt.math.*
import com.aitorgf.threekt.events.EventEmitter
import com.aitorgf.threekt.events.Observable
import com.aitorgf.threekt.types.Buffer
import com.aitorgf.threekt.types.ComplexBuffer
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsName

@ExperimentalJsExport
abstract class BufferAttribute(
     itemSize: Int = 1,
     normalized: Boolean = true
): ComplexBuffer {
    class Usage(val name: String) {
        companion object {
            val StaticDrawUsage = Usage("StaticDrawUsage")
        }
    }

    var name: String = ""

    var version: Int = 0
        protected set

    var usage: Usage = Usage.StaticDrawUsage

    var itemSize: Int = itemSize
        protected set

    var normalized: Boolean = normalized
        protected set

    protected val onUploadEmitter = EventEmitter()

    val onUpload: Observable
        get() = onUploadEmitter

    fun scheduleUpdate() {
        this.version++
    }

    /**
     * Return TRUE if this buffer is in video memory
     */
    abstract val inGPU: Boolean

    abstract fun clone(): BufferAttribute

    @ExperimentalJsExport
    @JsName("applyMatrix3")
    abstract fun apply(m: Matrix3): BufferAttribute

    @ExperimentalJsExport
    @JsName("applyMatrix4")
    abstract fun apply(m: Matrix4): BufferAttribute

    @ExperimentalJsExport
    abstract fun applyNormalMatrix(m: Matrix3): BufferAttribute

    @ExperimentalJsExport
    abstract fun transformDirection(m: Matrix4): BufferAttribute
}
