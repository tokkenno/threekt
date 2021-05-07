package com.aitorgf.threekt.math

import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport
import kotlin.js.JsName

@ExperimentalJsExport
@JsExport
class Ray(
    origin: Vector3 = Vector3(),
    direction: Vector3 = Vector3(0.0, 0.0, -1.0)
) {
    val origin: MutableVector3 = origin.toMutable()
    val direction: MutableVector3 = direction.toMutable()

    fun set(origin: Vector3, direction: Vector3): Ray {
        this.origin.set(origin)
        this.direction.set(direction)
        return this
    }

    @JsName("setRay")
    fun set(ray: Ray): Ray {
        this.origin.set(ray.origin)
        this.direction.set(ray.direction)
        return this
    }

    fun at(t: Double): Vector3 {
        val target = MutableVector3()
        return target.set(this.direction).multiply(t).add(this.origin)
    }

    fun lookAt(v: Vector3): Ray {
        this.direction.set(v).sub(this.origin).normalize()
        return this
    }

    fun recast(t: Double): Ray {
        this.origin.set(this.at(t))
        return this
    }

    fun closestPointToPoint(point: Vector3): Vector3 {
        val target = MutableVector3().sub(point, this.origin)
        val directionDistance = target.dot(this.direction)

        return if (directionDistance < 0.0) {
            target.set(this.origin)
        } else {
            target.set(this.direction).multiply(directionDistance).add(this.origin)
        }
    }

    fun distanceToPoint(point: Vector3): Double {
        return kotlin.math.sqrt(this.distanceSqToPoint(point))
    }

    fun distanceSqToPoint(point: Vector3): Double {
        val vector = MutableVector3()
        val directionDistance = vector.sub(point, this.origin).dot(this.direction)

        // point behind the ray
        if (directionDistance < 0.0) {
            return this.origin.distanceToSquared(point)
        }

        vector.set(this.direction).multiply(directionDistance).add(this.origin)

        return vector.distanceToSquared(point)
    }

    fun distanceSqToSegment(
        v0: Vector3,
        v1: Vector3,
        optionalPointOnRay: MutableVector3? = null,
        optionalPointOnSegment: MutableVector3? = null
    ): Double {
        // from http://www.geometrictools.com/GTEngine/Include/kotlin.mathematics/GteDistRaySegment.h
        // It returns the min distance between the ray and the segment
        // defined by v0 and v1
        // It can also set two optional targets :
        // - The closest point on the ray
        // - The closest point on the segment

        val segCenter = MutableVector3().set(v0).add(v1).multiply(0.5)
        val segDir = MutableVector3().set(v1).sub(v0).normalize()
        val diff = MutableVector3().set(this.origin).sub(segCenter)

        val segExtent = v0.distanceTo(v1) * 0.5
        val a01 = -this.direction.dot(segDir)
        val b0 = diff.dot(this.direction)
        val b1 = -diff.dot(segDir)
        val c = diff.lengthSq()
        val det = kotlin.math.abs(1 - a01 * a01)

        var s0: Double
        var s1: Double
        var sqrDist: Double
        var extDet: Double

        if (det > 0.0) {
            // The ray and segment are not parallel.
            s0 = a01 * b1 - b0
            s1 = a01 * b0 - b1
            extDet = segExtent * det

            if (s0 >= 0) {
                if (s1 >= -extDet) {
                    if (s1 <= extDet) {
                        // region 0
                        // Minimum at interior points of ray and segment.
                        val invDet = 1 / det
                        s0 *= invDet
                        s1 *= invDet
                        sqrDist = s0 * (s0 + a01 * s1 + 2 * b0) + s1 * (a01 * s0 + s1 + 2 * b1) + c

                    } else {
                        // region 1
                        s1 = segExtent
                        s0 = kotlin.math.max(0.0, -(a01 * s1 + b0))
                        sqrDist = -s0 * s0 + s1 * (s1 + 2 * b1) + c
                    }

                } else {
                    // region 5
                    s1 = -segExtent
                    s0 = kotlin.math.max(0.0, -(a01 * s1 + b0))
                    sqrDist = -s0 * s0 + s1 * (s1 + 2 * b1) + c
                }
            } else {
                if (s1 <= -extDet) {
                    // region 4
                    s0 = kotlin.math.max(0.0, -(-a01 * segExtent + b0))
                    s1 = if (s0 > 0.0) {
                        -segExtent
                    } else kotlin.math.min(kotlin.math.max(-segExtent, -b1), segExtent)
                    sqrDist = -s0 * s0 + s1 * (s1 + 2 * b1) + c
                } else if (s1 <= extDet) {
                    // region 3
                    s0 = 0.0
                    s1 = kotlin.math.min(kotlin.math.max(-segExtent, -b1), segExtent)
                    sqrDist = s1 * (s1 + 2 * b1) + c
                } else {
                    // region 2
                    s0 = kotlin.math.max(0.0, -(a01 * segExtent + b0))
                    s1 = if (s0 > 0.0) {
                        segExtent
                    } else kotlin.math.min(kotlin.math.max(-segExtent, -b1), segExtent)
                    sqrDist = -s0 * s0 + s1 * (s1 + 2 * b1) + c
                }
            }
        } else {
            // Ray and segment are parallel.
            s1 = if (a01 > 0.0) {
                -segExtent
            } else segExtent
            s0 = kotlin.math.max(0.0, -(a01 * s1 + b0))
            sqrDist = -s0 * s0 + s1 * (s1 + 2 * b1) + c
        }

        if (optionalPointOnRay != null) {
            optionalPointOnRay.set(this.direction).multiply(s0).add(this.origin)
        }

        if (optionalPointOnSegment != null) {
            optionalPointOnSegment.set(segDir).multiply(s1).add(segCenter)
        }

        return sqrDist
    }

    @JsName("intersectSphere")
    fun intersect(sphere: Sphere): Vector3? {
        val vector = MutableVector3().sub(sphere.center, this.origin)
        val tca = vector.dot(this.direction)
        val d2 = vector.dot(vector) - tca * tca
        val radius2 = sphere.radius * sphere.radius

        if (d2 > radius2) return null

        val thc = kotlin.math.sqrt(radius2 - d2)

        // t0 = first intersect point - entrance on front of sphere
        val t0 = tca - thc

        // t1 = second intersect point - exit point on back of sphere
        val t1 = tca + thc

        // test to see if both t0 and t1 are behind the ray - if so, return null
        if (t0 < 0 && t1 < 0) return null

        // test to see if t0 is behind the ray:
        // if it is, the ray is inside the sphere, so return the second exit point scaled by t1,
        // in order to always return an intersect point that is in front of the ray.
        if (t0 < 0) return this.at(t1)

        // else t0 is in front of the ray, so return the first collision point scaled by t0
        return this.at(t0)
    }

    @JsName("intersectsSphere")
    fun intersects(sphere: Sphere): Boolean {
        return this.distanceSqToPoint(sphere.center) <= (sphere.radius * sphere.radius)
    }

    @JsName("distanceToPlane")
    fun distanceTo(plane: Plane): Double? {
        val denominator = plane.normal.dot(this.direction)

        if (denominator == 0.0) {
            // line is coplanar, return origin
            if (plane.distanceToPoint(this.origin) == 0.0) {
                return 0.0
            }
            // Null is preferable to undefined since undefined means.... it is undefined
            return null
        }

        val t = -(this.origin.dot(plane.normal) + plane.constant) / denominator

        // Return if the ray never intersects the plane
        return if (t >= 0) {
            t
        } else null
    }

    @JsName("intersectPlane")
    fun intersect(plane: Plane): Vector3? {
        val t = this.distanceTo(plane)
        if (t == null) {
            return null
        }

        return this.at(t)
    }

    @JsName("intersectsPlane")
    fun intersects(plane: Plane): Boolean {
        // check if the ray lies on the plane first
        val distToPoint = plane.distanceToPoint(this.origin)

        if (distToPoint == 0.0) {
            return true
        }

        val denominator = plane.normal.dot(this.direction)

        if (denominator * distToPoint < 0.0) {
            return true
        }

        // ray origin is behind the plane (and is pointing behind it)
        return false
    }

    @JsName("intersectBox3")
    fun intersect(box: Box3): Vector3? {
        var tmin: Double
        var tmax: Double
        var tymin: Double
        var tymax: Double
        var tzmin: Double
        var tzmax: Double

        val invdirx = 1 / this.direction.x
        val invdiry = 1 / this.direction.y
        val invdirz = 1 / this.direction.z

        val origin = this.origin

        if (invdirx >= 0) {
            tmin = (box.min.x - origin.x) * invdirx
            tmax = (box.max.x - origin.x) * invdirx
        } else {
            tmin = (box.max.x - origin.x) * invdirx
            tmax = (box.min.x - origin.x) * invdirx
        }

        if (invdiry >= 0) {
            tymin = (box.min.y - origin.y) * invdiry
            tymax = (box.max.y - origin.y) * invdiry
        } else {
            tymin = (box.max.y - origin.y) * invdiry
            tymax = (box.min.y - origin.y) * invdiry
        }

        if ((tmin > tymax) || (tymin > tmax)) return null

        // These lines also handle the case where tmin or tmax is NaN
        // (result of 0 * Infinity). x !== x returns true if x is NaN

        if (tymin > tmin || tmin != tmin) tmin = tymin

        if (tymax < tmax || tmax != tmax) tmax = tymax

        if (invdirz >= 0) {
            tzmin = (box.min.z - origin.z) * invdirz
            tzmax = (box.max.z - origin.z) * invdirz
        } else {
            tzmin = (box.max.z - origin.z) * invdirz
            tzmax = (box.min.z - origin.z) * invdirz
        }

        if ((tmin > tzmax) || (tzmin > tmax)) return null

        if (tzmin > tmin || tmin != tmin) tmin = tzmin

        if (tzmax < tmax || tmax != tmax) tmax = tzmax

        //return point closest to the ray (positive side)

        if (tmax < 0) return null

        return this.at(
            if (tmin >= 0) {
                tmin
            } else tmax
        )
    }

    @JsName("intersectsBox3")
    fun intersects(box: Box3): Boolean {
        return this.intersect(box) != null
    }

    @JsName("intersectTriangle")
    fun intersect(triangle: Triangle, backfaceCulling: Boolean): Vector3? {
        // Compute the offset origin, edges, and normal.
        // from http://www.geometrictools.com/GTEngine/Include/kotlin.mathematics/GteIntrRay3Triangle3.h

        val edge1 = MutableVector3().sub(triangle.b, triangle.a)
        val edge2 = MutableVector3().sub(triangle.c, triangle.a)
        val normal = MutableVector3().cross(edge1, edge2)

        // Solve Q + t*D = b1*E1 + b2*E2 (Q = kDiff, D = ray direction,
        // E1 = kEdge1, E2 = kEdge2, N = Cross(E1,E2)) by
        //   |Dot(D,N)|*b1 = sign(Dot(D,N))*Dot(D,Cross(Q,E2))
        //   |Dot(D,N)|*b2 = sign(Dot(D,N))*Dot(D,Cross(E1,Q))
        //   |Dot(D,N)|*t = -sign(Dot(D,N))*Dot(Q,N)
        var DdN = this.direction.dot(normal)
        var sign: Int

        if (DdN > 0) {
            if (backfaceCulling) return null
            sign = 1
        } else if (DdN < 0) {
            sign = -1
            DdN = -DdN
        } else {
            return null
        }

        val diff = MutableVector3().sub(this.origin, triangle.a)
        val DdQxE2 = sign * this.direction.dot(edge2.cross(diff, edge2))

        // b1 < 0, no intersection
        if (DdQxE2 < 0.0) {
            return null
        }

        val DdE1xQ = sign * this.direction.dot(edge1.cross(diff))

        // b2 < 0, no intersection
        if (DdE1xQ < 0) {
            return null
        }

        // b1+b2 > 1, no intersection
        if (DdQxE2 + DdE1xQ > DdN) {
            return null
        }

        // Line intersects triangle, check if ray does.
        val QdN = -sign * diff.dot(normal)

        // t < 0, no intersection
        if (QdN < 0) {
            return null
        }

        // Ray intersects triangle.
        return this.at(QdN / DdN)
    }

    @JsName("applyMatrix4")
    fun apply(matrix4: Matrix4): Ray {
        this.origin.apply(matrix4)
        this.direction.transformDirection(matrix4)
        return this
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other === this) return true
        if (other !is Ray) return false
        return this.direction.equals(other.direction) && this.origin.equals(other.origin)
    }

    fun clone(): Ray {
        return Ray().set(this)
    }
}
