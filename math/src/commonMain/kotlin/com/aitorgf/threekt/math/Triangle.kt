package com.aitorgf.threekt.math

import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport
import kotlin.js.JsName

@ExperimentalJsExport
@JsExport
class Triangle(
    val a: MutableVector3 = MutableVector3(),
    val b: MutableVector3 = MutableVector3(),
    val c: MutableVector3 = MutableVector3()
) {
    companion object {
        fun getNormal(a: Vector3, b: Vector3, c: Vector3): Vector3 {
            val target = MutableVector3().sub(c, b)
            val v0 = MutableVector3().sub(a, b)
            target.cross(v0)

            val targetLengthSq = target.lengthSq()
            if (targetLengthSq > 0.0) {
                return target.multiply(1 / kotlin.math.sqrt(targetLengthSq))
            }

            return target.set(0.0, 0.0, 0.0)
        }

        // static/instance method to calculate barycentric coordinates
        // based on: http://www.blackpawn.com/texts/pointinpoly/default.html
        fun getBarycoord(point: Vector3, a: Vector3, b: Vector3, c: Vector3): Vector3 {
            val v0 = MutableVector3().sub(c, a)
            val v1 = MutableVector3().sub(b, a)
            val v2 = MutableVector3().sub(point, a)

            val dot00 = v0.dot(v0)
            val dot01 = v0.dot(v1)
            val dot02 = v0.dot(v2)
            val dot11 = v1.dot(v1)
            val dot12 = v1.dot(v2)

            val denom = (dot00 * dot11 - dot01 * dot01)

            val target = MutableVector3()

            // collinear or singular triangle
            if (denom == 0.0) {
                // arbitrary location outside of triangle?
                // not sure if this is the best idea, maybe should be returning undefined
                return target.set(-2.0, -1.0, -1.0)
            }

            val invDenom = 1.0 / denom
            val u = (dot11 * dot02 - dot01 * dot12) * invDenom
            val v = (dot00 * dot12 - dot01 * dot02) * invDenom

            // barycentric coordinates must always sum to 1
            return target.set(1 - u - v, v, u)
        }

        fun containsPoint(point: Vector3, a: Vector3, b: Vector3, c: Vector3): Boolean {
            val v = Triangle.getBarycoord(point, a, b, c)
            return (v.x >= 0) && (v.y >= 0) && ((v.x + v.y) <= 1)
        }

        fun getUV(
            point: Vector3,
            p1: Vector3,
            p2: Vector3,
            p3: Vector3,
            uv1: Vector2,
            uv2: Vector2,
            uv3: Vector2
        ): Vector2 {
            val v = this.getBarycoord(point, p1, p2, p3)

            val target = MutableVector2(0.0, 0.0)
            target.addScaledVector(uv1, v.x)
            target.addScaledVector(uv2, v.y)
            target.addScaledVector(uv3, v.z)

            return target
        }

        fun isFrontFacing(a: Vector3, b: Vector3, c: Vector3, direction: Vector3): Boolean {
            val v0 = MutableVector3().sub(c, b)
            val v1 = MutableVector3().sub(a, b)

            // strictly front facing
            return v0.cross(v1).dot(direction) < 0
        }
    }

    fun set(a: Vector3, b: Vector3, c: Vector3): Triangle {
        this.a.set(a)
        this.b.set(b)
        this.c.set(c)

        return this
    }

    @JsName("setTriangle")
    fun set(triangle: Triangle): Triangle {
        this.a.set(triangle.a)
        this.b.set(triangle.b)
        this.c.set(triangle.c)

        return this
    }

    fun setFromPointsAndIndices(points: Array<Vector3>, i0: Int, i1: Int, i2: Int): Triangle {
        this.a.set(points[i0])
        this.b.set(points[i1])
        this.c.set(points[i2])

        return this
    }

    fun clone(): Triangle {
        return Triangle().set(this)
    }

    fun getArea(): Double {
        val v0 = MutableVector3().sub(this.c, this.b)
        val v1 = MutableVector3().sub(this.a, this.b)

        return v0.cross(v1).length() * 0.5
    }

    fun getMidpoint(): Vector3 {
        val target = MutableVector3()
        return target.add(this.a, this.b).add(this.c).multiply(1.0 / 3.0)
    }

    fun getNormal(): Vector3 {
        return Triangle.getNormal(this.a, this.b, this.c)
    }

    fun getPlane(): Plane {
        val target = Plane()
        return target.setFromCoplanarPoints(this.a, this.b, this.c)
    }

    fun getBarycoord(point: Vector3): Vector3 {
        return Triangle.getBarycoord(point, this.a, this.b, this.c)
    }

    fun getUV(point: Vector3, uv1: Vector2, uv2: Vector2, uv3: Vector2): Vector2 {
        return Triangle.getUV(point, this.a, this.b, this.c, uv1, uv2, uv3)
    }

    fun containsPoint(point: Vector3): Boolean {
        return Triangle.containsPoint(point, this.a, this.b, this.c)
    }

    fun isFrontFacing(direction: Vector3): Boolean {
        return Triangle.isFrontFacing(this.a, this.b, this.c, direction)
    }

    @JsName("intersectsBox")
    fun intersects(box: Box3): Boolean {
        return box.intersects(this)
    }

    fun closestPointToPoint(p: Vector3): Vector3 {
        val target = MutableVector3()

        val a = this.a
        val b = this.b
        val c = this.c
        var v: Double
        var w: Double

        // algorithm thanks to Real-Time Collision Detection by Christer Ericson,
        // published by Morgan Kaufmann Publishers, (c) 2005 Elsevier Inc.,
        // under the accompanying license; see chapter 5.1.5 for detailed explanation.
        // basically, we're distinguishing which of the voronoi regions of the triangle
        // the point lies in with the minimum amount of redundant computation.

        val vab = MutableVector3().sub(b, a)
        val vac = MutableVector3().sub(c, a)
        val vap = MutableVector3().sub(p, a)
        val d1 = vab.dot(vap)
        val d2 = vac.dot(vap)

        if (d1 <= 0 && d2 <= 0) {
            // vertex region of A; barycentric coords (1, 0, 0)
            return target.set(a)
        }

        val vbp = MutableVector3().sub(p, b)
        val d3 = vab.dot(vbp)
        val d4 = vac.dot(vbp)

        if (d3 >= 0 && d4 <= d3) {
            // vertex region of B; barycentric coords (0, 1, 0)
            return target.set(b)
        }

        val vc = d1 * d4 - d3 * d2
        if (vc <= 0 && d1 >= 0 && d3 <= 0) {
            v = d1 / (d1 - d3)
            // edge region of AB; barycentric coords (1-v, v, 0)
            return target.set(a).addScaledVector(vab, v)
        }

        val vcp = MutableVector3().sub(p, c)
        val d5 = vab.dot(vcp)
        val d6 = vac.dot(vcp)
        if (d6 >= 0 && d5 <= d6) {
            // vertex region of C; barycentric coords (0, 0, 1)
            return target.set(c)
        }

        val vb = d5 * d2 - d1 * d6
        if (vb <= 0 && d2 >= 0 && d6 <= 0) {
            w = d2 / (d2 - d6)
            // edge region of AC; barycentric coords (1-w, 0, w)
            return target.set(a).addScaledVector(vac, w)
        }

        val va = d3 * d6 - d5 * d4
        if (va <= 0 && (d4 - d3) >= 0 && (d5 - d6) >= 0) {
            val vbc = MutableVector3().sub(c, b)
            w = (d4 - d3) / ((d4 - d3) + (d5 - d6))
            // edge region of BC; barycentric coords (0, 1-w, w)
            return target.set(b).addScaledVector(vbc, w) // edge region of BC
        }

        // face region
        val denom = 1.0 / (va + vb + vc)
        // u = va * denom
        v = vb * denom
        w = vc * denom

        return target.set(a).addScaledVector(vab, v).addScaledVector(vac, w)
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other === this) return true
        if (other !is Triangle) return false
        return this.a == other.a && this.b == other.b && this.c == other.c
    }
}
