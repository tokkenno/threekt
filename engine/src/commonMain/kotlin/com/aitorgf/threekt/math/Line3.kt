package com.aitorgf.threekt.math

import kotlin.js.JsExport
import kotlin.js.JsName

/** An analytical line segment in 3D space represented by a start and end point. */
@JsExport
class Line3(val start: Vector3 = Vector3(), val end: Vector3 = Vector3()) {

    /** Sets the start and end values by copying the given vectors. Returns this instance. */
    fun set(start: Vector3, end: Vector3): Line3 = apply {
        this.start.copy(start)
        this.end.copy(end)
    }

    /** Copies the supplied values into this instance and returns it. */
    @JsName("copyFrom")
    fun copy(line: Line3): Line3 = set(line.start, line.end)

    /** Returns a new instance with the same values. */
    fun copy(): Line3 = Line3().copy(this)

    /** Returns the center of the line segment. */
    fun getCenter(target: Vector3): Vector3 = target.addVectors(start, end).multiplyScalar(0.5)

    /** Returns the delta vector of the line segment's start and end point. */
    fun delta(target: Vector3): Vector3 = target.subVectors(end, start)

    /** Returns the squared Euclidean distance between the line' start and end point. */
    fun distanceSq(): Double = start.distanceToSquared(end)

    /** Returns the Euclidean distance between the line' start and end point. */
    fun distance(): Double = start.distanceTo(end)

    /** Returns a vector at a certain position along the line segment. */
    fun at(t: Double, target: Vector3): Vector3 =
        target.set(start.x + (end.x - start.x) * t, start.y + (end.y - start.y) * t, start.z + (end.z - start.z) * t)

    /** Returns a point parameter based on the closest point as projected on the line segment. */
    fun closestPointToPointParameter(point: Vector3, clampToLine: Boolean): Double {
        val direction = delta(Vector3())
        val length = direction.lengthSq()
        if (length == 0.0) return 0.0
        val t = point.copy().sub(start).dot(direction) / length
        return if (clampToLine) MathUtils.clamp(t, 0.0, 1.0) else t
    }

    /** Returns the closest point on the line for a given point. */
    fun closestPointToPoint(point: Vector3, clampToLine: Boolean, target: Vector3): Vector3 =
        at(closestPointToPointParameter(point, clampToLine), target)

    /** Returns the closest squared distance between this line segment and the given one. */
    fun distanceSqToLine3(line: Line3, c1: Vector3 = Vector3(), c2: Vector3 = Vector3()): Double {
        val d1 = delta(Vector3())
        val d2 = line.delta(Vector3())
        val delta = start.copy().sub(line.start)
        val length1 = d1.lengthSq()
        val length2 = d2.lengthSq()
        val cross = d1.dot(d2)
        val projection1 = d1.dot(delta)
        val projection2 = d2.dot(delta)
        var s: Double
        var t: Double
        val tolerance = 1e-16
        if (length1 <= tolerance) {
            s = 0.0
            t = if (length2 <= tolerance) 0.0 else MathUtils.clamp(projection2 / length2, 0.0, 1.0)
        } else if (length2 <= tolerance) {
            t = 0.0
            s = MathUtils.clamp(-projection1 / length1, 0.0, 1.0)
        } else {
            val determinant = length1 * length2 - cross * cross
            s = if (determinant <= 0.0) 0.0 else MathUtils.clamp((cross * projection2 - projection1 * length2) / determinant, 0.0, 1.0)
            t = (cross * s + projection2) / length2
            if (t < 0.0 || t > 1.0) {
                t = MathUtils.clamp(t, 0.0, 1.0)
                s = MathUtils.clamp((cross * t - projection1) / length1, 0.0, 1.0)
            }
        }
        val point1 = at(s, Vector3())
        val point2 = line.at(t, Vector3())
        c1.copy(point1)
        c2.copy(point2)
        return point1.distanceToSquared(point2)
    }

    /** Applies a 4x4 transformation matrix to this line segment. Returns this instance. */
    fun applyMatrix4(matrix: Matrix4): Line3 = apply {
        start.applyMatrix4(matrix)
        end.applyMatrix4(matrix)
    }

    /** Returns `true` if this line segment is equal with the given one. */
    override fun equals(other: Any?): Boolean = other is Line3 && start == other.start && end == other.end

    /** Returns a hash based on the current component values. */
    override fun hashCode(): Int = 31 * start.hashCode() + end.hashCode()
}
