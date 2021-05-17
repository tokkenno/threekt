package com.aitorgf.threekt.math

import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport
import kotlin.js.JsName

@ExperimentalJsExport
@JsExport
open class Box3(
    open val min: Vector3 = Vector3(
        Double.POSITIVE_INFINITY,
        Double.POSITIVE_INFINITY,
        Double.POSITIVE_INFINITY
    ),
    open val max: Vector3 = Vector3(
        Double.NEGATIVE_INFINITY,
        Double.NEGATIVE_INFINITY,
        Double.NEGATIVE_INFINITY
    )
) : Box {
    override val center: Vector3
        get() {
            return if (this.empty) {
                Vector3(0.0, 0.0, 0.0)
            } else MutableVector3().add(this.min, this.max).multiply(0.5)
        }

    override val size: Vector3
        get() {
            return if (this.empty) {
                Vector3(0.0, 0.0, 0.0)
            } else MutableVector3().sub(this.max, this.min)
        }

    override val empty: Boolean
        get() {
            // this is a more robust check for empty than ( volume <= 0 ) because volume can get positive with two negative axes
            return (this.max.x < this.min.x) || (this.max.y < this.min.y) || (this.max.z < this.min.z)
        }

    open fun clone(): Box3 {
        return Box3(this.min.clone(), this.max.clone())
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other === this) return true
        if (other !is Box3) return false
        return this.min.equals(this.min) && this.max.equals(this.max)
    }

    fun containsPoint(point: Vector3): Boolean {
        return !(point.x < this.min.x || point.x > this.max.x ||
                point.y < this.min.y || point.y > this.max.y ||
                point.z < this.min.z || point.z > this.max.z)
    }

    fun containsBox(box: MutableBox3): Boolean {
        return this.min.x <= box.min.x && box.max.x <= this.max.x &&
                this.min.y <= box.min.y && box.max.y <= this.max.y &&
                this.min.z <= box.min.z && box.max.z <= this.max.z
    }

    fun clampPoint(point: Vector3): Vector3 {
        return MutableVector3().set(point).clamp(this.min, this.max)
    }

    fun distanceToPoint(point: Vector3): Double {
        val clampedPoint = MutableVector3().set(point).clamp(this.min, this.max)

        return clampedPoint.sub(point).length()
    }

    fun getParameter(point: Vector3): Vector3 {
        return Vector3(
            (point.x - this.min.x) / (this.max.x - this.min.x),
            (point.y - this.min.y) / (this.max.y - this.min.y),
            (point.z - this.min.z) / (this.max.z - this.min.z)
        )
    }

    fun getBoundingSphere(): Sphere {
        return Sphere(
            this.center.toMutable(),
            this.size.length() * 0.5
        )
    }

    @JsName("intersectsBox")
    fun intersects(box: MutableBox3): Boolean {
        // using 6 splitting planes to rule out intersections.
        return !(box.max.x < this.min.x || box.min.x > this.max.x ||
                box.max.y < this.min.y || box.min.y > this.max.y ||
                box.max.z < this.min.z || box.min.z > this.max.z)
    }

    @JsName("intersectsSphere")
    fun intersects(sphere: Sphere): Boolean {
        // Find the point on the AABB closest to the sphere center.
        val vector = this.clampPoint(sphere.center)

        // If that point is inside the sphere, the AABB and sphere intersect.
        return vector.distanceToSquared(sphere.center) <= (sphere.radius * sphere.radius)
    }

    @JsName("intersectsPlane")
    fun intersects(plane: Plane): Boolean {
        // We compute the minimum and maximum dot product values. If those values
        // are on the same side (back or front) of the plane, then there is no intersection.

        var min: Double
        var max: Double

        if (plane.normal.x > 0) {
            min = plane.normal.x * this.min.x
            max = plane.normal.x * this.max.x
        } else {
            min = plane.normal.x * this.max.x
            max = plane.normal.x * this.min.x
        }

        if (plane.normal.y > 0) {
            min += plane.normal.y * this.min.y
            max += plane.normal.y * this.max.y
        } else {
            min += plane.normal.y * this.max.y
            max += plane.normal.y * this.min.y
        }

        if (plane.normal.z > 0) {
            min += plane.normal.z * this.min.z
            max += plane.normal.z * this.max.z
        } else {
            min += plane.normal.z * this.max.z
            max += plane.normal.z * this.min.z
        }

        return (min <= -plane.constant && max >= -plane.constant)
    }

    @JsName("intersectsTriangle")
    fun intersects(triangle: Triangle): Boolean {
        if (this.empty) {
            return false
        }

        // compute box center and extents
        val extents = MutableVector3().sub(this.max, this.center)

        // translate triangle to aabb origin
        val v0 = MutableVector3().sub(triangle.a, this.center)
        val v1 = MutableVector3().sub(triangle.b, this.center)
        val v2 = MutableVector3().sub(triangle.c, this.center)

        // compute edge vectors for triangle
        val f0 = MutableVector3().sub(v1, v0)
        val f1 = MutableVector3().sub(v2, v1)
        val f2 = MutableVector3().sub(v0, v2)

        // test against axes that are given by cross product combinations of the edges of the triangle and the edges of the aabb
        // make an axis testing of each of the 3 sides of the aabb against each of the 3 sides of the triangle = 9 axis of separation
        // axis_ij = u_i x f_j (u0, u1, u2 = face normals of aabb = x,y,z axes vectors since aabb is axis aligned)
        var axes: DoubleArray = doubleArrayOf(
            0.0, -f0.z, f0.y, 0.0, -f1.z, f1.y, 0.0, -f2.z, f2.y,
            f0.z, 0.0, -f0.x, f1.z, 0.0, -f1.x, f2.z, 0.0, -f2.x,
            -f0.y, f0.x, 0.0, -f1.y, f1.x, 0.0, -f2.y, f2.x, 0.0
        )

        if (!Box3.satForAxes(axes, v0, v1, v2, extents)) {
            return false
        }

        // test 3 face normals from the aabb
        axes = doubleArrayOf(1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 1.0)
        if (!Box3.satForAxes(axes, v0, v1, v2, extents)) {

            return false

        }

        // finally testing the face normal of the triangle
        // use already existing triangle edge vectors here
        val triangleNormal = MutableVector3().cross(f0, f1)
        return Box3.satForAxes(triangleNormal.toArray(), v0, v1, v2, extents)
    }

    companion object {
        private fun satForAxes(axes: DoubleArray, v0: Vector3, v1: Vector3, v2: Vector3, extents: Vector3): Boolean {
            for (i in 0..(axes.size - 3) step 3) {
                val testAxis = MutableVector3().fromArray(axes, i)
                // project the aabb onto the seperating axis
                val r =
                    extents.x * kotlin.math.abs(testAxis.x) + extents.y * kotlin.math.abs(testAxis.y) + extents.z * kotlin.math.abs(
                        testAxis.z
                    )
                // project all 3 vertices of the triangle onto the seperating axis
                val p0 = v0.dot(testAxis)
                val p1 = v1.dot(testAxis)
                val p2 = v2.dot(testAxis)
                // actual test, basically see if either of the most extreme of the triangle points intersects r
                if (kotlin.math.max(
                        -kotlin.math.max(p0, kotlin.math.max(p1, p2)),
                        kotlin.math.min(p0, kotlin.math.min(p1, p2))
                    ) > r
                ) {
                    // points of the projected triangle are outside the projected half-length of the aabb
                    // the axis is seperating and we can exit
                    return false
                }
            }

            return true
        }
    }
}
