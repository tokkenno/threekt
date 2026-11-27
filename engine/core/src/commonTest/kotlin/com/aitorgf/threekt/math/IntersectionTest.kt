package com.aitorgf.threekt.math

import com.aitorgf.threekt.core.Float32BufferAttribute
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class IntersectionTest {
    @Test
    fun line3ProjectsOntoClampedUnclampedAndDegenerateSegments() {
        val line = Line3(Vector3(1.0, 2.0, 0.0), Vector3(5.0, 2.0, 0.0))
        val point = Vector3(7.0, 4.0, 0.0)
        assertEquals(Vector3(3.0, 2.0, 0.0), line.getCenter(Vector3()))
        assertEquals(Vector3(4.0, 0.0, 0.0), line.delta(Vector3()))
        assertEquals(16.0, line.distanceSq())
        assertEquals(4.0, line.distance())
        assertEquals(1.5, line.closestPointToPointParameter(point, false))
        assertEquals(Vector3(5.0, 2.0, 0.0), line.closestPointToPoint(point, true, Vector3()))
        assertEquals(Vector3(7.0, 2.0, 0.0), line.closestPointToPoint(point, false, Vector3()))
        assertEquals(0.0, Line3().closestPointToPointParameter(point, false))
        assertEquals(Vector3(1.0, 5.0, 0.0), line.copy().applyMatrix4(Matrix4().makeTranslation(0.0, 3.0, 0.0)).start)
    }

    @Test
    fun closestSegmentPointsHandleCrossingAndDegenerateSegments() {
        val horizontal = Line3(Vector3(), Vector3(4.0, 0.0, 0.0))
        val vertical = Line3(Vector3(2.0, -1.0, 0.0), Vector3(2.0, 3.0, 0.0))
        val first = Vector3()
        val second = Vector3()
        val crossing = Vector3(2.0, 0.0, 0.0)
        assertEquals(0.0, horizontal.distanceSqToLine3(vertical, first, second))
        assertEquals(crossing, first)
        assertEquals(crossing, second)

        val point = Vector3(5.0, 2.0, 0.0)
        assertEquals(5.0, Line3(point, point).distanceSqToLine3(horizontal, first, second))
        assertEquals(point, first)
        assertEquals(horizontal.end, second)
    }

    @Test
    fun planeLineIntersectionHandlesCrossingParallelCoplanarAndUnclampedCases() {
        val plane = Plane(Vector3(0.0, 1.0, 0.0), -1.0)
        val crossing = Line3(Vector3(), Vector3(0.0, 2.0, 0.0))
        val target = Vector3()
        assertTrue(plane.intersectsLine(crossing))
        assertSame(target, plane.intersectLine(crossing, target))
        assertEquals(Vector3(0.0, 1.0, 0.0), target)

        val parallel = Line3(Vector3(), Vector3(2.0, 0.0, 0.0))
        assertNull(plane.intersectLine(parallel, target))
        parallel.applyMatrix4(Matrix4().makeTranslation(0.0, 1.0, 0.0))
        assertFalse(plane.intersectsLine(parallel))
        assertEquals(parallel.start, plane.intersectLine(parallel, target))

        val short = Line3(Vector3(), Vector3(0.0, 0.5, 0.0))
        assertNull(plane.intersectLine(short, target))
        assertEquals(Vector3(0.0, 1.0, 0.0), plane.intersectLine(short, target, clampToLine = false))
    }

    @Test
    fun planeTransformationUsesItsNormalMatrixAndKeepsTransformedPointsCoplanar() {
        val plane = Plane(Vector3(2.0, 2.0, 0.0), -4.0).normalize()
        val snapshot = plane.snapshot()
        val matrix = Matrix4().makeScale(2.0, 3.0, 1.0).setPosition(4.0, 5.0, 6.0)
        val transformed = plane.copy().applyMatrix4(matrix)
        val explicit = plane.copy().applyMatrix4(matrix, Matrix3().getNormalMatrix(matrix))
        val point = Vector3(1.0, 1.0, 0.0).applyMatrix4(matrix)
        assertEquals(explicit, transformed)
        assertEquals(0.0, transformed.distanceToPoint(point), 1e-12)
        assertEquals(1.0, transformed.normal.length(), 1e-12)
        assertEquals(0.0, transformed.distanceToPoint(transformed.coplanarPoint(Vector3())), 1e-12)
        assertEquals(0.0, transformed.distanceToPoint(transformed.projectPoint(Vector3(), Vector3())), 1e-12)
        assertEquals(0.0, transformed.distanceToSphere(Sphere(point.copy().add(transformed.normal), 1.0)), 1e-12)
        assertEquals(plane, transformed.translate(Vector3(1.0, 2.0, 3.0)).negate().restore(snapshot))
    }

    @Test
    fun triangleComputesBarycentricCoordinatesAndInterpolatesTypedAttributes() {
        val triangle = Triangle(Vector3(), Vector3(2.0, 0.0, 0.0), Vector3(0.0, 2.0, 0.0))
        val point = Vector3(0.5, 0.5, 4.0)
        val weights = assertNotNull(triangle.getBarycoord(point, Vector3()))
        assertEquals(Vector3(0.5, 0.25, 0.25), weights)
        assertTrue(triangle.containsPoint(point))
        assertFalse(triangle.containsPoint(Vector3(2.0, 2.0, 0.0)))
        val target = Vector2()
        assertSame(target, triangle.getInterpolation(point, Vector2(), Vector2(8.0, 0.0), Vector2(0.0, 4.0), target))
        assertEquals(Vector2(2.0, 1.0), target)
        val attribute = Float32BufferAttribute(floatArrayOf(0f, 0f, 8f, 0f, 0f, 4f), 2)
        val out = Triangle.getInterpolatedAttribute(attribute, 0, 1, 2, weights, Vector4())
        assertEquals(Vector4(2.0, 1.0, 0.0, 0.0), out)
        assertFailsWith<IllegalArgumentException> {
            Triangle.getInterpolatedAttribute(attribute, 0, 1, 3, weights, out)
        }
        assertNull(Triangle().getBarycoord(point, weights))
        assertEquals(Vector3(), weights)
    }

    @Test
    fun closestTrianglePointHandlesFacesEdgesAndDegenerateVertices() {
        val attribute = Float32BufferAttribute(floatArrayOf(0f, 0f, 0f, 2f, 0f, 0f, 0f, 2f, 0f), 3)
        val triangle = Triangle().setFromAttributeAndIndices(attribute, 0, 1, 2)
        assertEquals(2.0, triangle.getArea())
        assertEquals(Vector3(2.0 / 3.0, 2.0 / 3.0, 0.0), triangle.getMidpoint(Vector3()))
        assertEquals(1.0, triangle.getNormal(Vector3()).z)
        assertEquals(Vector3(0.5, 0.5, 0.0), triangle.closestPointToPoint(Vector3(0.5, 0.5, 2.0), Vector3()))
        assertEquals(Vector3(1.0, 1.0, 0.0), triangle.closestPointToPoint(Vector3(2.0, 2.0, 2.0), Vector3()))
        assertEquals(Vector3(), Triangle().closestPointToPoint(Vector3(1.0, 1.0, 1.0), Vector3()))
        assertTrue(triangle.isFrontFacing(Vector3(0.0, 0.0, -1.0)))
        assertFalse(triangle.isFrontFacing(Vector3(0.0, 0.0, 1.0)))
    }

    @Test
    fun rayIntersectsPlanesAndTrianglesOnlyInItsForwardDirection() {
        val target = Vector3()
        val ray = Ray(Vector3(0.5, 0.5, 2.0), Vector3(0.0, 0.0, -1.0))
        val plane = Plane(Vector3(0.0, 0.0, 1.0))
        assertEquals(2.0, ray.distanceToPlane(plane))
        assertEquals(Vector3(0.5, 0.5, 0.0), ray.intersectPlane(plane, target))
        val a = Vector3()
        val b = Vector3(2.0, 0.0, 0.0)
        val c = Vector3(0.0, 2.0, 0.0)
        assertEquals(Vector3(0.5, 0.5, 0.0), ray.intersectTriangle(a, b, c, true, target))
        assertNull(ray.intersectTriangle(a, c, b, true, target))
        assertNotNull(ray.intersectTriangle(a, c, b, false, target))
        assertNull(ray.copy().recast(3.0).distanceToPlane(plane))
        val coplanar = Ray(Vector3(), Vector3(1.0, 0.0, 0.0))
        assertEquals(0.0, coplanar.distanceToPlane(plane))
        assertFalse(Ray(Vector3(0.0, 0.0, 1.0), coplanar.direction).intersectsPlane(plane))
    }

    @Test
    fun rayHandlesParallelSlabsSphereTangencyAndDirectionTransforms() {
        val target = Vector3()
        val ray = Ray(Vector3(-2.0, 1.0, 0.0), Vector3(1.0, 0.0, 0.0))
        val box = Box3(Vector3(-1.0, -1.0, -1.0), Vector3(1.0, 1.0, 1.0))
        assertEquals(Vector3(-1.0, 1.0, 0.0), ray.intersectBox(box, target))
        assertFalse(Ray(Vector3(-2.0, 1.01, 0.0), ray.direction).intersectsBox(box))
        val sphere = Sphere(Vector3(), 1.0)
        assertTrue(ray.intersectsSphere(sphere))
        assertEquals(Vector3(0.0, 1.0, 0.0), ray.intersectSphere(sphere, target))
        assertNull(ray.intersectSphere(Sphere(Vector3(-10.0, 1.0, 0.0), 1.0), target))
        assertEquals(ray.origin, ray.closestPointToPoint(Vector3(-3.0, 9.0, 0.0), target))
        val transform = Matrix4().makeScale(2.0, 3.0, 4.0).setPosition(1.0, 0.0, 0.0)
        val transformed = ray.copy().applyMatrix4(transform)
        assertEquals(Vector3(-3.0, 3.0, 0.0), transformed.origin)
        assertEquals(Vector3(1.0, 0.0, 0.0), transformed.direction)
    }
}
