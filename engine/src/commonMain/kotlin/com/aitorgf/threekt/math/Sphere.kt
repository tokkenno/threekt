package com.aitorgf.threekt.math

import kotlin.js.JsExport
import kotlin.js.JsName

/**
 * An analytical 3D sphere defined by a center and radius. This class is mainly used as a Bounding Sphere
 * for 3D objects.
 */
@JsExport
class Sphere(val center: Vector3 = Vector3(), var radius: Double = -1.0) {

    /** Sets the sphere's components by copying the given values. Returns this instance. */
    fun set(center: Vector3, radius: Double): Sphere = apply {
        this.center.copy(center)
        this.radius = radius
    }

    /**
     * Builds a sphere enclosing [points]. Uses [optionalCenter] when supplied, or the center of the
     * points' bounding box. Returns this sphere.
     */
    fun setFromPoints(points: Array<out Vector3>, optionalCenter: Vector3? = null): Sphere =
        setFromPoints(points.asIterable(), optionalCenter)

    /**
     * Builds a sphere enclosing [points]. Uses [optionalCenter] when supplied, or the center of the
     * points' bounding box. Returns this sphere.
     */
    @JsExport.Ignore
    fun setFromPoints(points: Iterable<Vector3>, optionalCenter: Vector3? = null): Sphere {
        val samples = if (points is Collection) points else points.toList()
        if (optionalCenter != null) center.copy(optionalCenter) else Box3().setFromPoints(samples).getCenter(center)
        radius = kotlin.math.sqrt(samples.maxOfOrNull { center.distanceToSquared(it) } ?: 0.0)
        return this
    }

    /** Copies the supplied values into this instance and returns it. */
    @JsName("copyFrom")
    fun copy(sphere: Sphere): Sphere = set(sphere.center, sphere.radius)

    /** Returns a new instance with the same values. */
    fun copy(): Sphere = Sphere().copy(this)

    /**
     * Returns `true` if the sphere is empty (the radius set to a negative number). Spheres with a radius
     * of `0` contain only their center point and are not considered to be empty.
     */
    fun isEmpty(): Boolean = radius < 0.0

    /** Makes this sphere empty which means in encloses a zero space in 3D. Returns this instance. */
    fun makeEmpty(): Sphere = apply {
        center.setScalar(0.0)
        radius = -1.0
    }

    /** Returns `true` if this sphere contains the given point inclusive of the surface of the sphere. */
    fun containsPoint(point: Vector3): Boolean = point.distanceToSquared(center) <= radius * radius

    /**
     * Returns the distance from [point] to the center minus [radius]. The result is negative inside the
     * sphere.
     */
    fun distanceToPoint(point: Vector3): Double = point.distanceTo(center) - radius

    /** Returns `true` if this sphere intersects with the given one. */
    fun intersectsSphere(sphere: Sphere): Boolean {
        val combined = radius + sphere.radius
        return sphere.center.distanceToSquared(center) <= combined * combined
    }

    /** Returns `true` if this sphere intersects with the given box. */
    fun intersectsBox(box: Box3): Boolean = box.intersectsSphere(this)

    /** Returns `true` if this sphere intersects with the given plane. */
    fun intersectsPlane(plane: Plane): Boolean = kotlin.math.abs(plane.distanceToPoint(center)) <= radius

    /**
     * Clamps a point within the sphere. If the point is outside the sphere, it will clamp it to the
     * closest point on the edge of the sphere. Points already inside the sphere will not be affected.
     */
    fun clampPoint(point: Vector3, target: Vector3): Vector3 {
        val distanceSq = center.distanceToSquared(point)
        target.copy(point)
        if (distanceSq > radius * radius) target.sub(center).normalize().multiplyScalar(radius).add(center)
        return target
    }

    /** Returns a bounding box that encloses this sphere. */
    fun getBoundingBox(target: Box3): Box3 =
        if (isEmpty()) target.makeEmpty() else target.set(center, center).expandByScalar(radius)

    /** Transforms this sphere with the given 4x4 transformation matrix. Returns this instance. */
    fun applyMatrix4(matrix: Matrix4): Sphere = apply {
        center.applyMatrix4(matrix)
        radius *= matrix.getMaxScaleOnAxis()
    }

    /** Translates the sphere's center by the given offset. Returns this instance. */
    fun translate(offset: Vector3): Sphere = apply { center.add(offset) }

    /** Expands the boundaries of this sphere to include the given point. Returns this instance. */
    fun expandByPoint(point: Vector3): Sphere {
        if (isEmpty()) return set(point, 0.0)
        val delta = point.copy().sub(center)
        val distance = delta.length()
        if (distance > radius) {
            val increase = (distance - radius) * 0.5
            center.addScaledVector(delta, increase / distance)
            radius += increase
        }
        return this
    }

    /**
     * Expands this sphere to enclose both the original sphere and the given sphere. Returns this instance.
     */
    fun union(sphere: Sphere): Sphere {
        if (sphere.isEmpty()) return this
        if (isEmpty()) return copy(sphere)
        if (center == sphere.center) {
            radius = maxOf(radius, sphere.radius)
            return this
        }
        val offset = sphere.center.copy().sub(center).setLength(sphere.radius)
        expandByPoint(sphere.center.copy().add(offset))
        return expandByPoint(sphere.center.copy().sub(offset))
    }

    /** Returns `true` if this sphere is equal with the given one. */
    override fun equals(other: Any?): Boolean = other is Sphere && center == other.center && radius.toBits() == other.radius.toBits()

    /** Returns a hash based on the current component values. */
    override fun hashCode(): Int = 31 * center.hashCode() + radius.hashCode()

    /** Captures the sphere's center and radius as immutable values. */
    fun snapshot(): Snapshot = Snapshot(center.x, center.y, center.z, radius)

    /** Copies the center and radius from [snapshot] into this sphere and returns it. */
    fun restore(snapshot: Snapshot): Sphere = apply {
        center.set(snapshot.x, snapshot.y, snapshot.z)
        radius = snapshot.radius
    }

    /** Immutable coordinates and radius of a sphere. */
    data class Snapshot(val x: Double, val y: Double, val z: Double, val radius: Double)
}
