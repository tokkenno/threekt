package com.aitorgf.threekt.math

import kotlin.math.sqrt

/** A ray whose direction is expected to have unit length. */
class Ray(val origin: Vector3 = Vector3(), val direction: Vector3 = Vector3(0.0, 0.0, -1.0)) {

    /** Sets the ray's components by copying the given values. Returns this instance. */
    fun set(origin: Vector3, direction: Vector3): Ray = apply {
        this.origin.copy(origin)
        this.direction.copy(direction)
    }

    /** Copies the supplied values into this instance and returns it. */
    fun copy(ray: Ray): Ray = set(ray.origin, ray.direction)

    /** Returns a new instance with the same values. */
    fun copy(): Ray = Ray().copy(this)

    /** Returns a vector that is located at a given distance along this ray. */
    fun at(t: Double, target: Vector3): Vector3 =
        target.set(origin.x + direction.x * t, origin.y + direction.y * t, origin.z + direction.z * t)

    /**
     * Adjusts the direction of the ray to point at the given vector in world space. Returns this instance.
     */
    fun lookAt(point: Vector3): Ray = apply { direction.subVectors(point, origin).normalize() }

    /** Shift the origin of this ray along its direction by the given distance. Returns this instance. */
    fun recast(t: Double): Ray = apply { at(t, origin) }

    /** Returns the point along this ray that is closest to the given point. */
    fun closestPointToPoint(point: Vector3, target: Vector3): Vector3 =
        at(maxOf(0.0, point.copy().sub(origin).dot(direction)), target)

    /** Returns the squared distance of the closest approach between this ray and the given point. */
    fun distanceSqToPoint(point: Vector3): Double = closestPointToPoint(point, Vector3()).distanceToSquared(point)

    /** Returns the distance of the closest approach between this ray and the given point. */
    fun distanceToPoint(point: Vector3): Double = sqrt(distanceSqToPoint(point))

    /** Returns the squared distance between this ray and the given line segment. */
    fun distanceSqToSegment(
        v0: Vector3, v1: Vector3, optionalPointOnRay: Vector3? = null, optionalPointOnSegment: Vector3? = null,
    ): Double {
        val segment = Line3(v0, v1)
        val segmentDirection = segment.delta(Vector3())
        val lengthSq = segmentDirection.lengthSq()
        val offset = origin.copy().sub(v0)
        val cross = direction.dot(segmentDirection)
        val determinant = lengthSq - cross * cross
        val closestToOrigin = segment.closestPointToPointParameter(origin, true)
        val candidates = mutableListOf(0.0, 1.0, closestToOrigin)
        if (determinant > 0.0) {
            val s = (offset.dot(segmentDirection) - cross * direction.dot(offset)) / determinant
            candidates.add(MathUtils.clamp(s, 0.0, 1.0))
        }
        val onSegment = Vector3()
        val onRay = Vector3()
        var distance = Double.POSITIVE_INFINITY
        for (s in candidates) {
            segment.at(s, onSegment)
            closestPointToPoint(onSegment, onRay)
            val candidate = onSegment.distanceToSquared(onRay)
            if (candidate < distance) {
                distance = candidate
                optionalPointOnSegment?.copy(onSegment)
                optionalPointOnRay?.copy(onRay)
            }
        }
        return distance
    }

    /**
     * Intersects this ray with the given sphere, returning the intersection point or `null` if there is no
     * intersection.
     */
    fun intersectSphere(sphere: Sphere, target: Vector3): Vector3? {
        val delta = sphere.center.copy().sub(origin)
        val projection = delta.dot(direction)
        val distanceSq = delta.lengthSq() - projection * projection
        val radiusSq = sphere.radius * sphere.radius
        if (distanceSq > radiusSq) return null
        val halfChord = sqrt(radiusSq - distanceSq)
        val near = projection - halfChord
        val far = projection + halfChord
        if (far < 0.0) return null
        return at(if (near < 0.0) far else near, target)
    }

    /** Returns `true` if this ray intersects with the given sphere. */
    fun intersectsSphere(sphere: Sphere): Boolean = distanceSqToPoint(sphere.center) <= sphere.radius * sphere.radius

    /**
     * Computes the distance from the ray's origin to the given plane. Returns `null` if the ray does not
     * intersect with the plane.
     */
    fun distanceToPlane(plane: Plane): Double? {
        val denominator = plane.normal.dot(direction)
        val distance = plane.distanceToPoint(origin)
        if (denominator == 0.0) return if (distance == 0.0) 0.0 else null
        return (-distance / denominator).takeIf { it >= 0.0 }
    }

    /**
     * Intersects this ray with the given plane, returning the intersection point or `null` if there is no
     * intersection.
     */
    fun intersectPlane(plane: Plane, target: Vector3): Vector3? = distanceToPlane(plane)?.let { at(it, target) }

    /** Returns `true` if this ray intersects with the given plane. */
    fun intersectsPlane(plane: Plane): Boolean = distanceToPlane(plane) != null

    /**
     * Intersects this ray with the given bounding box, returning the intersection point or `null` if there
     * is no intersection.
     */
    fun intersectBox(box: Box3, target: Vector3): Vector3? {
        if (box.isEmpty()) return null
        var near = Double.NEGATIVE_INFINITY
        var far = Double.POSITIVE_INFINITY
        for (axis in 0..2) {
            val o = origin[axis]
            val d = direction[axis]
            val min = box.min[axis]
            val max = box.max[axis]
            if (d == 0.0) {
                if (o < min || o > max) return null
                continue
            }
            val entry = (min - o) / d
            val exit = (max - o) / d
            near = maxOf(near, minOf(entry, exit))
            far = minOf(far, maxOf(entry, exit))
            if (near > far || far < 0.0) return null
        }
        return at(if (near >= 0.0) near else far, target)
    }

    /** Returns `true` if this ray intersects with the given box. */
    fun intersectsBox(box: Box3): Boolean = intersectBox(box, Vector3()) != null

    /**
     * Intersects this ray with the given triangle, returning the intersection point or `null` if there is
     * no intersection.
     */
    fun intersectTriangle(a: Vector3, b: Vector3, c: Vector3, backfaceCulling: Boolean, target: Vector3): Vector3? {
        val edge1 = b.copy().sub(a)
        val edge2 = c.copy().sub(a)
        val p = direction.copy().cross(edge2)
        val determinant = edge1.dot(p)
        if (determinant == 0.0 || (backfaceCulling && determinant < 0.0)) return null
        val offset = origin.copy().sub(a)
        val u = offset.dot(p) / determinant
        if (u < 0.0 || u > 1.0) return null
        val q = offset.cross(edge1)
        val v = direction.dot(q) / determinant
        if (v < 0.0 || u + v > 1.0) return null
        val t = edge2.dot(q) / determinant
        return if (t >= 0.0) at(t, target) else null
    }

    /** Transforms this ray with the given 4x4 transformation matrix. Returns this instance. */
    fun applyMatrix4(matrix: Matrix4): Ray = apply {
        origin.applyMatrix4(matrix)
        direction.transformDirection(matrix)
    }

    /** Returns `true` if this ray is equal with the given one. */
    override fun equals(other: Any?): Boolean = other is Ray && origin == other.origin && direction == other.direction

    /** Returns a hash based on the current component values. */
    override fun hashCode(): Int = 31 * origin.hashCode() + direction.hashCode()
}
