package com.aitorgf.threekt.math

import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

@ExperimentalJsExport
@JsExport
open class Box2(
    open val min: Vector2 = Vector2(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY),
    open val max: Vector2 = Vector2(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY)
) : Box {
    override val center: Vector2
        get() {
            return if (this.empty) Vector2() else this.min.toMutable().add(this.max).multiply(0.5)
        }

    override val size: Vector2
        get() {
            return if (this.empty) Vector2() else this.max.toMutable().sub(this.min)
        }

    override val empty: Boolean
        get() {
            return (this.max.x < this.min.x) || (this.max.y < this.min.y)
        }

    open fun clone(): Box2 {
        return Box2(this.min.clone(), this.max.clone())
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other === this) return true
        if (other !is Box2) return false
        return this.min == other.min && this.max == other.max
    }

    fun containsPoint(point: Vector2): Boolean {
        return !(point.x < this.min.x || point.x > this.max.x ||
                point.y < this.min.y || point.y > this.max.y)
    }

    fun containsBox(box: Box2): Boolean {
        return this.min.x <= box.min.x && box.max.x <= this.max.x &&
                this.min.y <= box.min.y && box.max.y <= this.max.y
    }

    fun intersectsBox(box: Box2): Boolean {
        // using 4 splitting planes to rule out intersections
        return !(box.max.x < this.min.x || box.min.x > this.max.x ||
                box.max.y < this.min.y || box.min.y > this.max.y)
    }

    fun clampPoint(point: Vector2): Vector2 {
        return MutableVector2().set(point).clamp(this.min, this.max)
    }

    fun distanceToPoint(point: Vector2): Double {
        val clampedPoint = MutableVector2().set(point).clamp(this.min, this.max)
        return clampedPoint.sub(point).length()
    }
}
