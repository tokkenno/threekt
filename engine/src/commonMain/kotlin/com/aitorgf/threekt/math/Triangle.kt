package com.aitorgf.threekt.math

import com.aitorgf.threekt.core.BufferAttribute
import kotlin.js.JsExport
import kotlin.js.JsName

/** A geometric triangle as defined by three vectors representing its three corners. */
@JsExport
class Triangle(val a: Vector3 = Vector3(), val b: Vector3 = Vector3(), val c: Vector3 = Vector3()) {

    /** Sets the triangle's vertices by copying the given values. Returns this instance. */
    fun set(a: Vector3, b: Vector3, c: Vector3): Triangle = apply {
        this.a.copy(a)
        this.b.copy(b)
        this.c.copy(c)
    }

    /** Sets the triangle's vertices by copying the given array values. Returns this instance. */
    @JsExport.Ignore
    fun setFromPointsAndIndices(points: List<Vector3>, i0: Int, i1: Int, i2: Int): Triangle =
        set(points[i0], points[i1], points[i2])

    /** Sets the triangle's vertices by copying the given array values. Returns this instance. */
    fun setFromPointsAndIndices(points: Array<out Vector3>, i0: Int, i1: Int, i2: Int): Triangle =
        set(points[i0], points[i1], points[i2])

    /** Sets the triangle's vertices by copying the given attribute values. Returns this instance. */
    fun setFromAttributeAndIndices(attribute: BufferAttribute, i0: Int, i1: Int, i2: Int): Triangle = apply {
        a.fromBufferAttribute(attribute, i0)
        b.fromBufferAttribute(attribute, i1)
        c.fromBufferAttribute(attribute, i2)
    }

    /** Returns a new instance with the same values. */
    fun copy(): Triangle = Triangle().copy(this)

    /** Copies the supplied values into this instance and returns it. */
    @JsName("copyFrom")
    fun copy(other: Triangle): Triangle = set(other.a, other.b, other.c)

    /** Computes the area of the triangle. */
    fun getArea(): Double = b.copy().sub(a).cross(c.copy().sub(a)).length() * 0.5

    /** Computes the midpoint of the triangle. */
    fun getMidpoint(target: Vector3): Vector3 = target.set(
        (a.x + b.x + c.x) / 3.0, (a.y + b.y + c.y) / 3.0, (a.z + b.z + c.z) / 3.0,
    )

    /** Computes the normal of the triangle. */
    fun getNormal(target: Vector3): Vector3 = getNormal(a, b, c, target)

    /** Computes a plane the triangle lies within. */
    fun getPlane(target: Plane): Plane = target.setFromCoplanarPoints(a, b, c)

    /**
     * Writes the barycentric coordinates of [point] projected onto the triangle plane into [target].
     * Returns null and zeroes [target] for a degenerate triangle.
     */
    fun getBarycoord(point: Vector3, target: Vector3): Vector3? = getBarycoord(point, a, b, c, target)

    /**
     * Interpolates [v1], [v2], and [v3] according to the barycentric coordinates of [point]. Returns
     * [target], or null and zeroes [target] for a degenerate triangle.
     */
    fun <V : MutableVector<V>> getInterpolation(point: Vector3, v1: V, v2: V, v3: V, target: V): V? =
        getInterpolation(point, a, b, c, v1, v2, v3, target)

    /**
     * Returns `true` if the given point, when projected onto the plane of the triangle, lies within the
     * triangle. triangle, lies within the triangle or not.
     */
    fun containsPoint(point: Vector3): Boolean = containsPoint(point, a, b, c)

    /** Returns `true` if the triangle is oriented towards the given direction. */
    fun isFrontFacing(direction: Vector3): Boolean = isFrontFacing(a, b, c, direction)

    /** Returns `true` if this triangle intersects with the given box. */
    fun intersectsBox(box: Box3): Boolean = box.intersectsTriangle(this)

    /** Returns `true` if this triangle is equal with the given one. */
    override fun equals(other: Any?): Boolean = other is Triangle && a == other.a && b == other.b && c == other.c

    /** Returns a hash based on the current component values. */
    override fun hashCode(): Int = 31 * (31 * a.hashCode() + b.hashCode()) + c.hashCode()

    /** Returns the closest point on the triangle to the given point. */
    fun closestPointToPoint(point: Vector3, target: Vector3): Vector3 {
        val projected = getPlane(Plane()).projectPoint(point, Vector3())
        if (containsPoint(projected)) return target.copy(projected)
        val closest = listOf(Line3(a, b), Line3(b, c), Line3(c, a))
            .map { it.closestPointToPoint(point, true, Vector3()) }
            .minBy { it.distanceToSquared(point) }
        return target.copy(closest)
    }

    companion object {

        /** Computes the normal of the triangle. */
        fun getNormal(a: Vector3, b: Vector3, c: Vector3, target: Vector3): Vector3 {
            val edge1 = c.copy().sub(b)
            val edge2 = a.copy().sub(b)
            return target.crossVectors(edge1, edge2).normalize()
        }

        /**
         * Writes the barycentric coordinates of [point] projected onto the triangle plane into [target].
         * Returns null and zeroes [target] for a degenerate triangle.
         */
        fun getBarycoord(point: Vector3, a: Vector3, b: Vector3, c: Vector3, target: Vector3): Vector3? {
            val v0 = c.copy().sub(a)
            val v1 = b.copy().sub(a)
            val v2 = point.copy().sub(a)
            val dot00 = v0.dot(v0)
            val dot01 = v0.dot(v1)
            val dot02 = v0.dot(v2)
            val dot11 = v1.dot(v1)
            val dot12 = v1.dot(v2)
            val denominator = dot00 * dot11 - dot01 * dot01
            if (denominator == 0.0) {
                target.setScalar(0.0)
                return null
            }
            val u = (dot11 * dot02 - dot01 * dot12) / denominator
            val v = (dot00 * dot12 - dot01 * dot02) / denominator
            return target.set(1.0 - u - v, v, u)
        }

        /**
         * Returns `true` if the given point, when projected onto the plane of the triangle, lies within
         * the triangle. triangle, lies within the triangle or not.
         */
        fun containsPoint(point: Vector3, a: Vector3, b: Vector3, c: Vector3): Boolean {
            val weight = getBarycoord(point, a, b, c, Vector3()) ?: return false
            return weight.x >= 0.0 && weight.y >= 0.0 && weight.z >= 0.0
        }

        /**
         * Interpolates [v1], [v2], and [v3] according to the barycentric coordinates of [point]. Returns
         * [target], or null and zeroes [target] for a degenerate triangle.
         */
        fun <V : MutableVector<V>> getInterpolation(
            point: Vector3, a: Vector3, b: Vector3, c: Vector3,
            v1: V, v2: V, v3: V, target: V,
        ): V? {
            val weights = getBarycoord(point, a, b, c, Vector3()) ?: run {
                target.setScalar(0.0)
                return null
            }
            for (component in 0 until target.dimension) {
                target[component] = v1[component] * weights.x + v2[component] * weights.y + v3[component] * weights.z
            }
            return target
        }

        /**
         * Blends the three indexed attribute items with [weights] and writes the result into [target].
         * Components absent from the source attribute are set to zero.
         */
        fun <V : MutableVector<V>> getInterpolatedAttribute(
            attribute: BufferAttribute, i1: Int, i2: Int, i3: Int, weights: Vector3, target: V,
        ): V {
            require(i1 in 0 until attribute.count && i2 in 0 until attribute.count && i3 in 0 until attribute.count)
            for (component in 0 until target.dimension) {
                target[component] = if (component < attribute.itemSize) {
                    attribute.getComponent(i1, component) * weights.x +
                        attribute.getComponent(i2, component) * weights.y +
                        attribute.getComponent(i3, component) * weights.z
                } else 0.0
            }
            return target
        }

        /** Returns `true` if the triangle is oriented towards the given direction. */
        fun isFrontFacing(a: Vector3, b: Vector3, c: Vector3, direction: Vector3): Boolean =
            c.copy().sub(b).cross(a.copy().sub(b)).dot(direction) < 0.0
    }
}
