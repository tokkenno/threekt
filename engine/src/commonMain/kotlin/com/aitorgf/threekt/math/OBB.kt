package com.aitorgf.threekt.math

import kotlin.js.JsExport
import kotlin.math.abs

/**
 * An oriented bounding box, as the Three.js addon `OBB`: a [center], the [halfSize] along each local
 * axis and a [rotation] whose columns are those axes. The constructor and [set] retain the supplied
 * objects. Three.js keeps the scratch objects at module level; Threekt keeps them per instance.
 */
@JsExport
class OBB(
    var center: Vector3 = Vector3(),
    var halfSize: Vector3 = Vector3(),
    var rotation: Matrix3 = Matrix3(),
) {
    private val xAxis = Vector3()
    private val yAxis = Vector3()
    private val zAxis = Vector3()
    private val v1 = Vector3()

    /** Retains [center], [halfSize] and [rotation] and returns this box. */
    fun set(center: Vector3, halfSize: Vector3, rotation: Matrix3): OBB {
        this.center = center
        this.halfSize = halfSize
        this.rotation = rotation
        return this
    }

    /** Copies the values of [obb] into this box's objects and returns this box. */
    fun copy(obb: OBB): OBB {
        center.copy(obb.center)
        halfSize.copy(obb.halfSize)
        rotation.copy(obb.rotation)
        return this
    }

    /** Returns an independent copy. */
    fun clone(): OBB = OBB().copy(this)

    /** Writes the full size into [target] and returns it. */
    fun getSize(target: Vector3): Vector3 = target.copy(halfSize).multiplyScalar(2.0)

    /** Writes the point of the box closest to [point] into [target] and returns it. */
    fun clampPoint(point: Vector3, target: Vector3): Vector3 {
        v1.subVectors(point, center)
        rotation.extractBasis(xAxis, yAxis, zAxis)
        target.copy(center)
        target.add(xAxis.multiplyScalar(v1.dot(xAxis).coerceIn(-halfSize.x, halfSize.x)))
        target.add(yAxis.multiplyScalar(v1.dot(yAxis).coerceIn(-halfSize.y, halfSize.y)))
        target.add(zAxis.multiplyScalar(v1.dot(zAxis).coerceIn(-halfSize.z, halfSize.z)))
        return target
    }

    /** Returns whether [point] lies inside or on the box. */
    fun containsPoint(point: Vector3): Boolean {
        v1.subVectors(point, center)
        rotation.extractBasis(xAxis, yAxis, zAxis)
        return abs(v1.dot(xAxis)) <= halfSize.x && abs(v1.dot(yAxis)) <= halfSize.y && abs(v1.dot(zAxis)) <= halfSize.z
    }

    /** Returns whether this box intersects the axis-aligned [box3]. */
    fun intersectsBox3(box3: Box3): Boolean = intersectsOBB(OBB().fromBox3(box3))

    /** Returns whether this box intersects [sphere]. */
    fun intersectsSphere(sphere: Sphere): Boolean {
        val closestPoint = clampPoint(sphere.center, Vector3())
        return closestPoint.distanceToSquared(sphere.center) <= sphere.radius * sphere.radius
    }

    /**
     * Returns whether this box intersects [obb], using the separating axis theorem from Ericson's
     * *Real-Time Collision Detection*. [epsilon] guards against nearly parallel edges.
     */
    fun intersectsOBB(obb: OBB, epsilon: Double = 2.220446049250313e-16): Boolean {
        val ae = doubleArrayOf(halfSize.x, halfSize.y, halfSize.z)
        val au = arrayOf(Vector3(), Vector3(), Vector3())
        rotation.extractBasis(au[0], au[1], au[2])
        val be = doubleArrayOf(obb.halfSize.x, obb.halfSize.y, obb.halfSize.z)
        val bu = arrayOf(Vector3(), Vector3(), Vector3())
        obb.rotation.extractBasis(bu[0], bu[1], bu[2])
        val r = Array(3) { i -> DoubleArray(3) { j -> au[i].dot(bu[j]) } }
        v1.subVectors(obb.center, center)
        val t = doubleArrayOf(v1.dot(au[0]), v1.dot(au[1]), v1.dot(au[2]))
        val absR = Array(3) { i -> DoubleArray(3) { j -> abs(r[i][j]) + epsilon } }
        var ra: Double
        var rb: Double
        // Axes L = A0, A1, A2.
        for (i in 0 until 3) {
            ra = ae[i]
            rb = be[0] * absR[i][0] + be[1] * absR[i][1] + be[2] * absR[i][2]
            if (abs(t[i]) > ra + rb) return false
        }
        // Axes L = B0, B1, B2.
        for (i in 0 until 3) {
            ra = ae[0] * absR[0][i] + ae[1] * absR[1][i] + ae[2] * absR[2][i]
            rb = be[i]
            if (abs(t[0] * r[0][i] + t[1] * r[1][i] + t[2] * r[2][i]) > ra + rb) return false
        }
        // Axis L = A0 x B0.
        ra = ae[1] * absR[2][0] + ae[2] * absR[1][0]
        rb = be[1] * absR[0][2] + be[2] * absR[0][1]
        if (abs(t[2] * r[1][0] - t[1] * r[2][0]) > ra + rb) return false
        // Axis L = A0 x B1.
        ra = ae[1] * absR[2][1] + ae[2] * absR[1][1]
        rb = be[0] * absR[0][2] + be[2] * absR[0][0]
        if (abs(t[2] * r[1][1] - t[1] * r[2][1]) > ra + rb) return false
        // Axis L = A0 x B2.
        ra = ae[1] * absR[2][2] + ae[2] * absR[1][2]
        rb = be[0] * absR[0][1] + be[1] * absR[0][0]
        if (abs(t[2] * r[1][2] - t[1] * r[2][2]) > ra + rb) return false
        // Axis L = A1 x B0.
        ra = ae[0] * absR[2][0] + ae[2] * absR[0][0]
        rb = be[1] * absR[1][2] + be[2] * absR[1][1]
        if (abs(t[0] * r[2][0] - t[2] * r[0][0]) > ra + rb) return false
        // Axis L = A1 x B1.
        ra = ae[0] * absR[2][1] + ae[2] * absR[0][1]
        rb = be[0] * absR[1][2] + be[2] * absR[1][0]
        if (abs(t[0] * r[2][1] - t[2] * r[0][1]) > ra + rb) return false
        // Axis L = A1 x B2.
        ra = ae[0] * absR[2][2] + ae[2] * absR[0][2]
        rb = be[0] * absR[1][1] + be[1] * absR[1][0]
        if (abs(t[0] * r[2][2] - t[2] * r[0][2]) > ra + rb) return false
        // Axis L = A2 x B0.
        ra = ae[0] * absR[1][0] + ae[1] * absR[0][0]
        rb = be[1] * absR[2][2] + be[2] * absR[2][1]
        if (abs(t[1] * r[0][0] - t[0] * r[1][0]) > ra + rb) return false
        // Axis L = A2 x B1.
        ra = ae[0] * absR[1][1] + ae[1] * absR[0][1]
        rb = be[0] * absR[2][2] + be[2] * absR[2][0]
        if (abs(t[1] * r[0][1] - t[0] * r[1][1]) > ra + rb) return false
        // Axis L = A2 x B2.
        ra = ae[0] * absR[1][2] + ae[1] * absR[0][2]
        rb = be[0] * absR[2][1] + be[1] * absR[2][0]
        if (abs(t[1] * r[0][2] - t[0] * r[1][2]) > ra + rb) return false
        return true
    }

    /** Returns whether this box intersects [plane]. */
    fun intersectsPlane(plane: Plane): Boolean {
        rotation.extractBasis(xAxis, yAxis, zAxis)
        val r = halfSize.x * abs(plane.normal.dot(xAxis)) + halfSize.y * abs(plane.normal.dot(yAxis)) +
            halfSize.z * abs(plane.normal.dot(zAxis))
        return abs(plane.distanceToPoint(center)) <= r
    }

    /** Writes the nearest intersection of [ray] with this box into [target] and returns it, or null if none. */
    fun intersectRay(ray: Ray, target: Vector3): Vector3? {
        val aabb = Box3().setFromCenterAndSize(Vector3(), getSize(Vector3()))
        val matrix = Matrix4().setFromMatrix3(rotation).setPosition(center)
        val localRay = Ray().copy(ray).applyMatrix4(Matrix4().copy(matrix).invert())
        return if (localRay.intersectBox(aabb, target) != null) target.applyMatrix4(matrix) else null
    }

    /** Returns whether [ray] intersects this box. */
    fun intersectsRay(ray: Ray): Boolean = intersectRay(ray, Vector3()) != null

    /** Sets this box to the axis-aligned [box3] and returns it. */
    fun fromBox3(box3: Box3): OBB {
        box3.getCenter(center)
        box3.getSize(halfSize).multiplyScalar(0.5)
        rotation.identity()
        return this
    }

    /** Returns whether [other] is an [OBB] with the same center, half size and rotation. */
    override fun equals(other: Any?): Boolean =
        other is OBB && other.center == center && other.halfSize == halfSize && other.rotation == rotation

    override fun hashCode(): Int = (center.hashCode() * 31 + halfSize.hashCode()) * 31 + rotation.hashCode()

    /**
     * Transforms this box by [matrix], which may scale but not shear. A negative determinant flips the X
     * axis. Returns this box.
     */
    fun applyMatrix4(matrix: Matrix4): OBB {
        val e = matrix.elements
        var sx = v1.set(e[0], e[1], e[2]).length()
        val sy = v1.set(e[4], e[5], e[6]).length()
        val sz = v1.set(e[8], e[9], e[10]).length()
        if (matrix.determinant() < 0) sx = -sx
        val rotationMatrix = Matrix3().setFromMatrix4(matrix)
        val elements = rotationMatrix.elements
        for (i in 0 until 3) elements[i] /= sx
        for (i in 3 until 6) elements[i] /= sy
        for (i in 6 until 9) elements[i] /= sz
        rotation.premultiply(rotationMatrix)
        halfSize.x *= abs(sx)
        halfSize.y *= sy
        halfSize.z *= sz
        center.add(v1.setFromMatrixPosition(matrix))
        return this
    }
}
