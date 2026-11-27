package com.aitorgf.threekt.math

import kotlin.js.JsExport
import kotlin.js.JsName

/**
 * A two dimensional surface that extends infinitely in 3D space, represented in [Hessian normal
 * form](https://mathworld.wolfram.com/HessianNormalForm.html) by a unit length normal vector and a
 * constant.
 */
@JsExport
class Plane(val normal: Vector3 = Vector3(1.0, 0.0, 0.0), var constant: Double = 0.0) {

    /** Sets the plane components by copying the given values. Returns this instance. */
    fun set(normal: Vector3, constant: Double): Plane = apply {
        this.normal.copy(normal)
        this.constant = constant
    }

    /**
     * Sets the plane components by defining `x`, `y`, `z` as the plane normal and `w` as the constant.
     * Returns this instance.
     */
    fun setComponents(x: Double, y: Double, z: Double, w: Double): Plane = apply {
        normal.set(x, y, z)
        constant = w
    }

    /**
     * Sets the plane from the given normal and coplanar point (that is a point that lies onto the plane).
     * Returns this instance.
     */
    fun setFromNormalAndCoplanarPoint(normal: Vector3, point: Vector3): Plane = set(normal, -point.dot(normal))

    /**
     * Sets the plane from three coplanar points. The winding order is assumed to be counter-clockwise, and
     * determines the direction of the plane normal. Returns this instance.
     */
    fun setFromCoplanarPoints(a: Vector3, b: Vector3, c: Vector3): Plane =
        setFromNormalAndCoplanarPoint(c.copy().sub(b).cross(a.copy().sub(b)).normalize(), a)

    /** Copies the supplied values into this instance and returns it. */
    @JsName("copyFrom")
    fun copy(plane: Plane): Plane = set(plane.normal, plane.constant)

    /** Returns a new instance with the same values. */
    fun copy(): Plane = Plane().copy(this)

    /** Normalizes the plane normal and adjusts the constant accordingly. Returns this instance. */
    fun normalize(): Plane {
        val inverse = 1.0 / normal.length()
        normal.multiplyScalar(inverse)
        constant *= inverse
        return this
    }

    /** Negates both the plane normal and the constant. Returns this instance. */
    fun negate(): Plane = apply {
        normal.negate()
        constant = -constant
    }

    /** Returns the signed distance from the given point to this plane. */
    fun distanceToPoint(point: Vector3): Double = normal.dot(point) + constant

    /** Returns the signed distance from the given sphere to this plane. */
    fun distanceToSphere(sphere: Sphere): Double = distanceToPoint(sphere.center) - sphere.radius

    /** Projects a the given point onto the plane. */
    fun projectPoint(point: Vector3, target: Vector3): Vector3 {
        val distance = distanceToPoint(point)
        return target.copy(point).addScaledVector(normal, -distance)
    }

    /**
     * Writes the intersection with [line] into [target] and returns it, or returns null for a miss. With
     * [clampToLine], intersections must lie on the segment. Returns its starting point if the line is
     * coplanar.
     */
    fun intersectLine(line: Line3, target: Vector3, clampToLine: Boolean = true): Vector3? {
        val direction = line.delta(Vector3())
        val denominator = normal.dot(direction)
        val distance = distanceToPoint(line.start)
        if (denominator == 0.0) return if (distance == 0.0) target.copy(line.start) else null
        val t = -distance / denominator
        if (clampToLine && (t < 0.0 || t > 1.0)) return null
        return target.copy(line.start).addScaledVector(direction, t)
    }

    /** Returns `true` if the given line segment intersects with (passes through) the plane. */
    fun intersectsLine(line: Line3): Boolean {
        val start = distanceToPoint(line.start)
        val end = distanceToPoint(line.end)
        return (start < 0.0 && end > 0.0) || (start > 0.0 && end < 0.0)
    }

    /** Returns `true` if the given bounding box intersects with the plane. */
    fun intersectsBox(box: Box3): Boolean = box.intersectsPlane(this)

    /** Returns `true` if the given bounding sphere intersects with the plane. */
    fun intersectsSphere(sphere: Sphere): Boolean = sphere.intersectsPlane(this)

    /**
     * Returns a coplanar vector to the plane, by calculating the projection of the normal at the origin
     * onto the plane.
     */
    fun coplanarPoint(target: Vector3): Vector3 = target.copy(normal).multiplyScalar(-constant)

    /**
     * Apply a 4x4 matrix to the plane. The matrix must be an affine, homogeneous transform. The optional
     * normal matrix can be pre-computed like so: Returns this instance.
     */
    fun applyMatrix4(matrix: Matrix4, optionalNormalMatrix: Matrix3? = null): Plane {
        val point = coplanarPoint(Vector3()).applyMatrix4(matrix)
        normal.applyNormalMatrix(optionalNormalMatrix ?: Matrix3().getNormalMatrix(matrix))
        constant = -point.dot(normal)
        return this
    }

    /**
     * Translates the plane by the distance defined by the given offset vector. Note that this only affects
     * the plane constant and will not affect the normal vector. Returns this instance.
     */
    fun translate(offset: Vector3): Plane = apply { constant -= offset.dot(normal) }

    /** Returns `true` if this plane is equal with the given one. */
    override fun equals(other: Any?): Boolean = other is Plane && normal == other.normal && constant.toBits() == other.constant.toBits()

    /** Returns a hash based on the current component values. */
    override fun hashCode(): Int = 31 * normal.hashCode() + constant.hashCode()

    /** Captures the normal components and constant as immutable values. */
    fun snapshot(): Snapshot = Snapshot(normal.x, normal.y, normal.z, constant)

    /** Copies the normal and constant from [snapshot] into this plane and returns it. */
    fun restore(snapshot: Snapshot): Plane = setComponents(snapshot.x, snapshot.y, snapshot.z, snapshot.constant)

    /** Immutable coefficients of a plane equation. */
    data class Snapshot(val x: Double, val y: Double, val z: Double, val constant: Double)
}
