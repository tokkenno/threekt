package com.aitorgf.threekt.math

import com.aitorgf.threekt.core.Object3D
import com.aitorgf.threekt.objects.InstancedMesh
import com.aitorgf.threekt.objects.Line
import com.aitorgf.threekt.objects.Mesh
import com.aitorgf.threekt.objects.Points
import com.aitorgf.threekt.objects.Sprite
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
     * Tests whether the world-space bounding sphere of [object3D] intersects this frustum, using its current
     * world transform. Instanced meshes use their own bounding sphere, which covers every instance; meshes,
     * lines and points use the bounding sphere of their geometry. Missing spheres are computed first.
     * Returns false for other objects, which Three.js cannot test either.
     */
    fun intersectsObject(object3D: Object3D): Boolean {
        if (object3D is InstancedMesh) {
            if (object3D.boundingSphere == null) object3D.computeBoundingSphere()
            sphere.copy(object3D.boundingSphere!!).applyMatrix4(object3D.matrixWorld)
        } else {
            val geometry = when (object3D) {
                is Mesh -> object3D.geometry
                is Line -> object3D.geometry
                is Points -> object3D.geometry
                is Sprite -> object3D.geometry
                else -> return false
            }
            if (geometry.boundingSphere == null) geometry.computeBoundingSphere()
            sphere.copy(geometry.boundingSphere!!).applyMatrix4(object3D.matrixWorld)
        }
        return intersectsSphere(sphere)
    }

    /**
     * Tests the bounding sphere of [sprite] using its current world transform. The sphere grows with the
     * distance of the sprite's center from the middle of the quad.
     */
    fun intersectsSprite(sprite: Sprite): Boolean {
        sphere.center.set(0.0, 0.0, 0.0)
        val offset = defaultSpriteCenter.distanceTo(sprite.center)
        sphere.radius = 0.7071067811865476 + offset
        sphere.applyMatrix4(sprite.matrixWorld)
        return intersectsSphere(sphere)
    }

    private val sphere = Sphere()

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

    private companion object {
        val defaultSpriteCenter = Vector2(0.5, 0.5)
    }
}
