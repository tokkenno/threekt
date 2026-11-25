package com.aitorgf.threekt.math

import com.aitorgf.threekt.core.Object3D
import com.aitorgf.threekt.objects.Mesh
import com.aitorgf.threekt.renderers.CoordinateSystem
import kotlin.math.sqrt

/**
 * Frustums are used to determine what is inside the camera's field of view. They help speed up the
 * rendering process - objects which lie outside a camera's frustum can safely be excluded from rendering.
 * This class is mainly intended for use internally by a renderer.
 */
class Frustum(
    p0: Plane = Plane(), p1: Plane = Plane(), p2: Plane = Plane(),
    p3: Plane = Plane(), p4: Plane = Plane(), p5: Plane = Plane(),
) {
    /** The six inward-facing clipping planes. */
    val planes: List<Plane> = listOf(p0, p1, p2, p3, p4, p5)

    /** Sets the frustum planes by copying the given planes. Returns this instance. */
    fun set(p0: Plane, p1: Plane, p2: Plane, p3: Plane, p4: Plane, p5: Plane): Frustum {
        val source = listOf(p0, p1, p2, p3, p4, p5)
        for (index in planes.indices) planes[index].copy(source[index])
        return this
    }

    /** Copies the supplied values into this instance and returns it. */
    fun copy(frustum: Frustum): Frustum {
        for (index in planes.indices) planes[index].copy(frustum.planes[index])
        return this
    }

    /** Returns a new instance with the same values. */
    fun copy(): Frustum = Frustum().copy(this)

    /**
     * Extracts six clipping planes from [matrix], using [coordinateSystem] and [reversedDepth] to
     * interpret the depth range. Pass a view-projection matrix for world-space planes.
     */
    fun setFromProjectionMatrix(
        matrix: Matrix4,
        coordinateSystem: CoordinateSystem = CoordinateSystem.WebGL,
        reversedDepth: Boolean = false,
    ): Frustum {
        val e = matrix.elements
        fun extract(index: Int, row: Int, sign: Double, includeFourthRow: Boolean = true) {
            val base = if (includeFourthRow) 1.0 else 0.0
            planes[index].setComponents(
                base * e[3] + sign * e[row], base * e[7] + sign * e[4 + row],
                base * e[11] + sign * e[8 + row], base * e[15] + sign * e[12 + row],
            ).normalize()
        }

        extract(0, 0, -1.0)
        extract(1, 0, 1.0)
        extract(2, 1, 1.0)
        extract(3, 1, -1.0)
        if (reversedDepth) {
            extract(4, 2, 1.0, false)
            extract(5, 2, -1.0)
        } else {
            extract(4, 2, -1.0)
            extract(5, 2, 1.0, coordinateSystem == CoordinateSystem.WebGL)
        }
        return this
    }

    /**
     * Tests whether the world-space bounding sphere of a mesh intersects this frustum. Reads the mesh's
     * current world transform.
     */
    fun intersectsObject(object3D: Object3D): Boolean {
        val positions = (object3D as? Mesh)?.geometry?.getAttribute("position") ?: return false
        val sphere = Box3().setFromBufferAttribute(positions).getBoundingSphere(Sphere())
        if (sphere.isEmpty()) return false
        val point = Vector3()
        var radiusSq = 0.0
        repeat(positions.count) {
            point.fromBufferAttribute(positions, it)
            radiusSq = maxOf(radiusSq, point.distanceToSquared(sphere.center))
        }
        sphere.radius = sqrt(radiusSq)
        return intersectsSphere(sphere.applyMatrix4(object3D.matrixWorld))
    }

    /** Tests the sprite's bounding sphere using its current world transform. */
    fun intersectsSprite(sprite: Object3D): Boolean =
        intersectsSphere(Sphere(Vector3(), sqrt(0.5)).applyMatrix4(sprite.matrixWorld))

    /**
     * Returns `true` if the given bounding sphere is intersecting this frustum. This is a fast,
     * conservative test that favors performance over precision. It can report false positives for spheres
     * that lie outside the frustum but are not separated by a single frustum plane. It never reports false
     * negatives, so it is safe for culling.
     */
    fun intersectsSphere(sphere: Sphere): Boolean = planes.all { it.distanceToPoint(sphere.center) >= -sphere.radius }

    /** Returns `true` if the given point lies within the frustum. */
    fun containsPoint(point: Vector3): Boolean = planes.all { it.distanceToPoint(point) >= 0.0 }

    /**
     * Returns `true` if the given bounding box is intersecting this frustum. This is a fast, conservative
     * test that favors performance over precision. It can report false positives for large boxes that lie
     * outside the frustum but are not separated by a single frustum plane. It never reports false
     * negatives, so it is safe for culling.
     */
    fun intersectsBox(box: Box3): Boolean {
        val corner = Vector3()
        return planes.all { plane ->
            corner.set(
                if (plane.normal.x >= 0.0) box.max.x else box.min.x,
                if (plane.normal.y >= 0.0) box.max.y else box.min.y,
                if (plane.normal.z >= 0.0) box.max.z else box.min.z,
            )
            plane.distanceToPoint(corner) >= 0.0
        }
    }
}
