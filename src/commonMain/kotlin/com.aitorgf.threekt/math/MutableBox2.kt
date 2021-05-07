package com.aitorgf.threekt.math

import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport
import kotlin.js.JsName

@ExperimentalJsExport
@JsExport
class MutableBox2(
    override val min: MutableVector2 = MutableVector2(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY),
    override val max: MutableVector2 = MutableVector2(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY)
) : Box2(min, max), MutableBox {
    override fun clone(): MutableBox2 {
        return MutableBox2(this.min.clone(), this.max.clone())
    }

    fun set(min: Vector2, max: Vector2): MutableBox2 {
        this.min.set(min)
        this.max.set(max)
        return this
    }

    @JsName("setBox2")
    fun set(other: MutableBox2): MutableBox2 {
        this.min.set(other.min)
        this.max.set(other.max)
        return this
    }

    fun setFromCenterAndSize(center: Vector2, size: Vector2): MutableBox2 {
        val halfSize = MutableVector2().set(size).multiply(0.5)
        this.min.set(center).sub(halfSize)
        this.max.set(center).add(halfSize)
        return this
    }

    fun expandByPoint(point: Vector2): MutableBox2 {
        this.min.min(point)
        this.max.max(point)
        return this
    }

    fun expandByVector(vector: Vector2): MutableBox2 {
        this.min.sub(vector)
        this.max.add(vector)
        return this
    }

    fun expandByScalar(scalar: Double): MutableBox2 {
        this.min.add(-scalar)
        this.max.add(scalar)
        return this
    }

    fun intersect(box: MutableBox2): MutableBox2 {
        this.min.max(box.min)
        this.max.min(box.max)
        return this
    }

    fun union(box: MutableBox2): MutableBox2 {
        this.min.min(box.min)
        this.max.max(box.max)
        return this
    }

    fun translate(offset: Vector2): MutableBox2 {
        this.min.add(offset)
        this.max.add(offset)
        return this
    }
}
