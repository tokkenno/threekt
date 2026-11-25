package com.aitorgf.threekt.core

import kotlin.js.JsExport

/**
 * Vertex data stored as consecutive items of [itemSize] components in a typed array owned by a subclass.
 * Renderers upload the data once and again after [markUpdated] is called, which replaces the Three.js
 * `needsUpdate = true` setter.
 */
@JsExport
abstract class BufferAttribute(
    /** Number of components per item, for example `3` for positions. */
    val itemSize: Int,
    /**
     * Whether integer components are mapped to `[0, 1]` (unsigned) or `[-1, 1]` (signed) when read by
     * shaders and by [getComponent]. Has no effect on floating-point attributes.
     */
    var normalized: Boolean,
) {
    init {
        require(itemSize > 0) { "Items must have at least one component." }
    }

    /** Expected update pattern, which renderers pass to the GPU as a hint. Change it before the first draw. */
    private var usageHint = Usage.StaticDraw
    open val usage: Usage get() = usageHint

    /** Sets [usage] and returns this attribute, as Three.js `setUsage()` does. */
    open fun setUsage(value: Usage): BufferAttribute {
        usageHint = value
        return this
    }

    /** Optional name, used for example as the key of a morph target. */
    var name = ""

    /** Number of components in the underlying array. */
    abstract val size: Int

    /** Number of complete items. */
    val count: Int get() = size / itemSize

    /** Incremented by [markUpdated] so that renderers know to upload the data again. */
    private var revision = 0
    open val version: Int get() = revision

    /** Marks the data as changed so that it is uploaded before the next draw. */
    open fun markUpdated() {
        revision++
    }

    /** Returns component [component] of item [index], normalized when [normalized] is set. */
    fun getComponent(index: Int, component: Int): Double = read(index * itemSize + component)

    /** Returns the first component of item [index]. */
    fun getX(index: Int): Double = getComponent(index, 0)

    /** Returns the second component of item [index]. */
    fun getY(index: Int): Double = getComponent(index, 1)

    /** Returns the third component of item [index]. */
    fun getZ(index: Int): Double = getComponent(index, 2)

    /** Returns the fourth component of item [index]. */
    fun getW(index: Int): Double = getComponent(index, 3)

    /** Reads the array element at [offset], applying normalization when it applies. */
    protected abstract fun read(offset: Int): Double

    /** Validates that [size] holds a whole number of items. Call it from subclass initializers. */
    protected fun requireWholeItems() {
        require(size % itemSize == 0) { "The buffer must contain a whole number of items." }
    }
}
