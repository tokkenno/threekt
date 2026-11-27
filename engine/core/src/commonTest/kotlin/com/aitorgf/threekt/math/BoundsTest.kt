package com.aitorgf.threekt.math

import com.aitorgf.threekt.core.BufferGeometry
import com.aitorgf.threekt.core.Float32BufferAttribute
import com.aitorgf.threekt.core.Object3D
import com.aitorgf.threekt.objects.Mesh
import kotlin.math.PI
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class BoundsTest {
    @Test
    fun box2HandlesEmptyDegenerateTouchingAndDisjointBounds() {
        val box = Box2()
        assertTrue(box.isEmpty())
        assertEquals(Vector2(), box.getCenter(Vector2(4.0, 5.0)))
        assertEquals(Vector2(), box.getSize(Vector2(4.0, 5.0)))
        box.setFromPoints(arrayOf(Vector2(2.0, 3.0)))
        assertFalse(box.isEmpty())
        assertTrue(box.containsPoint(Vector2(2.0, 3.0)))
        box.expandByVector(Vector2(2.0, 1.0))
        val adjacent = Box2(Vector2(4.0, 2.0), Vector2(6.0, 4.0))
        assertTrue(box.intersectsBox(adjacent))
        assertEquals(Box2(Vector2(4.0, 2.0), Vector2(4.0, 4.0)), box.copy().intersect(adjacent))
        assertTrue(box.copy().intersect(adjacent.copy().translate(Vector2(1.0, 0.0))).isEmpty())
        assertTrue(box.copy().union(adjacent).containsBox(adjacent))
    }

    @Test
    fun box2NormalizesParametersAndClampsPoints() {
        val box = Box2().setFromCenterAndSize(Vector2(2.0, 3.0), Vector2(4.0, 2.0))
        assertEquals(Vector2(1.5, -0.5), box.getParameter(Vector2(6.0, 1.0), Vector2()))
        val target = Vector2()
        assertSame(target, box.clampPoint(Vector2(7.0, -2.0), target))
        assertEquals(Vector2(4.0, 2.0), target)
        assertEquals(5.0, box.distanceToPoint(Vector2(7.0, -2.0)))
        assertEquals(Box2(Vector2(1.0, 3.0), Vector2(3.0, 3.0)), box.expandByScalar(-1.0))
    }

    @Test
    fun box3BuildsBoundsFromStridedAttributesAndTransformsEveryCorner() {
        val attribute = Float32BufferAttribute(floatArrayOf(-1f, -2f, -3f, 99f, 1f, 2f, 3f, 99f), 4)
        val box = Box3().setFromBufferAttribute(attribute)
        assertEquals(Box3().setFromArray(doubleArrayOf(-1.0, -2.0, -3.0, 1.0, 2.0, 3.0)), box)
        val snapshot = box.snapshot()
        val transform = Matrix4().makeRotationZ(PI / 2).setPosition(4.0, 0.0, 2.0)
        box.applyMatrix4(transform)
        assertEquals(0.0, box.min.distanceTo(Vector3(2.0, -1.0, -1.0)), 1e-12)
        assertEquals(0.0, box.max.distanceTo(Vector3(6.0, 1.0, 5.0)), 1e-12)
        assertEquals(Vector3(4.0, 0.0, 2.0), box.getCenter(Vector3()))
        assertEquals(sqrt(14.0), box.getBoundingSphere(Sphere()).radius, 1e-12)
        assertEquals(Box3().setFromBufferAttribute(attribute), box.restore(snapshot))
        assertTrue(box.makeEmpty().applyMatrix4(transform).isEmpty())
        assertTrue(box.getBoundingSphere(Sphere(Vector3(), 2.0)).isEmpty())
    }

    @Test
    fun preciseObjectBoundsUseWorldSpaceVerticesInsteadOfLocalBoundingCorners() {
        val geometry = BufferGeometry().setAttribute(
            "position", Float32BufferAttribute(floatArrayOf(0f, 0f, 0f, 2f, 0f, 0f, 0f, 2f, 0f), 3),
        )
        val group = Object3D()
        group.position.y = 5.0
        val mesh = Mesh(geometry)
        mesh.quaternion.setFromAxisAngle(Vector3(0.0, 0.0, 1.0), PI / 4)
        group.add(mesh)

        val precise = Box3().setFromObject(group, precise = true)
        val conservative = Box3().setFromObject(group)
        assertEquals(5.0 + sqrt(2.0), precise.max.y, 1e-12)
        assertEquals(5.0 + 2.0 * sqrt(2.0), conservative.max.y, 1e-12)
        assertTrue(conservative.containsBox(precise))
    }

    @Test
    fun box3IntersectionsDistinguishTangencyAndSeparatingTriangleAxes() {
        val box = Box3(Vector3(), Vector3(1.0, 1.0, 1.0))
        assertTrue(box.intersectsSphere(Sphere(Vector3(2.0, 0.5, 0.5), 1.0)))
        assertFalse(box.intersectsSphere(Sphere(Vector3(2.0, 2.0, 0.5), 1.0)))
        assertTrue(box.intersectsPlane(Plane(Vector3(1.0, 0.0, 0.0), -1.0)))
        assertFalse(box.intersectsPlane(Plane(Vector3(1.0, 0.0, 0.0), -1.1)))
        val outside = Triangle(Vector3(0.6, 1.6, 0.5), Vector3(1.6, 0.6, 0.5), Vector3(1.6, 1.6, 0.5))
        assertTrue(box.intersectsBox(Box3().setFromPoints(listOf(outside.a, outside.b, outside.c))))
        assertFalse(box.intersectsTriangle(outside))
        val crossing = Triangle(Vector3(-1.0, 0.5, 0.5), Vector3(2.0, 0.5, 0.5), Vector3(0.5, 2.0, 0.5))
        assertTrue(crossing.intersectsBox(box))
        assertEquals(Vector3(0.5, 0.5, 0.5), box.getParameter(Vector3(0.5, 0.5, 0.5), Vector3()))
    }

    @Test
    fun sphereFitsPointsAroundAnExplicitCenterAndSnapshotsAreIndependent() {
        val points = arrayOf(Vector3(-2.0, 0.0, 0.0), Vector3(4.0, 0.0, 0.0))
        val sphere = Sphere().setFromPoints(points)
        assertEquals(Sphere(Vector3(1.0, 0.0, 0.0), 3.0), sphere)
        sphere.setFromPoints(points, Vector3())
        assertEquals(4.0, sphere.radius)
        assertEquals(-4.0, sphere.distanceToPoint(Vector3()))
        assertTrue(sphere.containsPoint(points[1]))
        val snapshot = sphere.snapshot()
        assertEquals(Box3(Vector3(-4.0, -4.0, -4.0), Vector3(4.0, 4.0, 4.0)), sphere.getBoundingBox(Box3()))
        assertEquals(Vector3(4.0, 0.0, 0.0), sphere.clampPoint(Vector3(8.0, 0.0, 0.0), Vector3()))
        assertTrue(sphere.makeEmpty().getBoundingBox(Box3()).isEmpty())
        assertEquals(Sphere(Vector3(), 4.0), sphere.restore(snapshot))
    }

    @Test
    fun sphereExpansionUnionAndTransformEncloseTheirInputs() {
        val sphere = Sphere()
        sphere.expandByPoint(Vector3(-1.0, 0.0, 0.0)).expandByPoint(Vector3(1.0, 0.0, 0.0))
        assertEquals(Sphere(Vector3(), 1.0), sphere)
        sphere.union(Sphere(Vector3(4.0, 0.0, 0.0), 1.0))
        assertEquals(Sphere(Vector3(2.0, 0.0, 0.0), 3.0), sphere)
        assertTrue(sphere.intersectsSphere(Sphere(Vector3(6.0, 0.0, 0.0), 1.0)))
        assertFalse(sphere.intersectsSphere(Sphere(Vector3(6.1, 0.0, 0.0), 1.0)))
        sphere.applyMatrix4(Matrix4().makeScale(2.0, 3.0, 4.0)).translate(Vector3(1.0, 0.0, 0.0))
        assertEquals(Sphere(Vector3(5.0, 0.0, 0.0), 12.0), sphere)
    }
}
