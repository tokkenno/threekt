package com.aitorgf.threekt.math

import com.aitorgf.threekt.cameras.PerspectiveCamera
import com.aitorgf.threekt.geometries.Box
import com.aitorgf.threekt.math.color.ColorSpace
import com.aitorgf.threekt.math.color.CssColorNames
import com.aitorgf.threekt.math.color.HSL
import com.aitorgf.threekt.math.color.RGB
import com.aitorgf.threekt.math.interpolants.BezierInterpolant
import com.aitorgf.threekt.math.interpolants.CubicInterpolant
import com.aitorgf.threekt.math.interpolants.DiscreteInterpolant
import com.aitorgf.threekt.math.interpolants.LinearInterpolant
import com.aitorgf.threekt.math.interpolants.QuaternionLinearInterpolant
import com.aitorgf.threekt.objects.Mesh
import com.aitorgf.threekt.renderers.CoordinateSystem
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ThreeMathParityTest {
    @Test
    fun inverseOfAsymmetricProjectionMatchesThreeJs() {
        val matrix = Matrix4().makePerspective(-2.0, 3.0, 4.0, -1.0, 1.0, 41.0, CoordinateSystem.WebGPU)
        val inverse = matrix.copy().invert()
        assertComponents(
            doubleArrayOf(2.5, 0.0, 0.0, 0.0, 0.0, 2.5, 0.0, 0.0, 0.0, 0.0, 0.0, -40.0 / 41, 0.5, 1.5, -1.0, 1.0),
            inverse.elements,
        )
        assertComponents(Matrix4().elements, matrix.multiply(inverse).elements)

        val matrix3 = Matrix3(1.0, 2.0, 3.0, 0.0, 1.0, 4.0, 5.0, 6.0, 0.0)
        assertComponents(Matrix3().elements, matrix3.copy().invert().multiply(matrix3).elements)
        assertComponents(doubleArrayOf(1.0, 3.0, 2.0, 4.0), Matrix2(1.0, 2.0, 3.0, 4.0).elements)
    }

    @Test
    fun everyEulerOrderRoundTripsItsRotation() {
        for (order in Euler.Order.entries) {
            val rotation = Quaternion().setFromEuler(Euler(0.2, -0.7, 1.4, order))
            val euler = Euler().setFromQuaternion(rotation, order)
            assertEquals(1.0, kotlin.math.abs(rotation.dot(Quaternion().setFromEuler(euler))), 1e-12)
        }
        val q = Quaternion().setFromEuler(Euler(0.2, -0.7, 1.4, Euler.Order.YZX))
        assertComponents(doubleArrayOf(-0.1480697458468565, -0.20053724593661193, 0.6283198169093515, 0.7369357984492306), q.toArray())
        val axis = Vector4().setAxisAngleFromRotationMatrix(Matrix4().makeRotationY(PI))
        assertComponents(doubleArrayOf(0.0, 1.0, 0.0, PI), axis.toArray())
    }

    @Test
    fun properEulerOrdersMatchComposedAxisRotations() {
        val axes = mapOf(
            'X' to Vector3(1.0, 0.0, 0.0),
            'Y' to Vector3(0.0, 1.0, 0.0),
            'Z' to Vector3(0.0, 0.0, 1.0),
        )
        for (order in MathUtils.ProperEulerOrder.entries) {
            val axis = axes.getValue(order.name[0])
            val middleAxis = axes.getValue(order.name[1])
            for (middle in listOf(-0.7, 0.0, PI)) {
                val expected = Quaternion().setFromAxisAngle(axis, 0.2)
                    .multiply(Quaternion().setFromAxisAngle(middleAxis, middle))
                    .multiply(Quaternion().setFromAxisAngle(axis, 1.4))
                val actual = Quaternion()
                MathUtils.setQuaternionFromProperEuler(actual, 0.2, middle, 1.4, order)

                assertEquals(1.0, kotlin.math.abs(expected.dot(actual)), 1e-12, "$order at $middle")
            }
        }
    }

    @Test
    fun decomposeSupportsReflectedScaleAndCoordinateConversionsRoundTrip() {
        val pos = Vector3(1.0, 2.0, 3.0)
        val orientation = Quaternion().setFromEuler(Euler(0.2, 0.4, 0.9))
        val scale = Vector3(-1.0, 2.0, 3.0)
        val matrix = Matrix4().compose(pos, orientation, scale)
        val decomposedPosition = Vector3()
        val decomposedRotation = Quaternion()
        val decomposedScale = Vector3()
        matrix.decompose(decomposedPosition, decomposedRotation, decomposedScale)
        assertComponents(matrix.elements, Matrix4().compose(decomposedPosition, decomposedRotation, decomposedScale).elements)

        assertComponents(pos.toArray(), Vector3().setFromSpherical(Spherical().setFromVector3(pos)).toArray())
        assertComponents(pos.toArray(), Vector3().setFromCylindrical(Cylindrical().setFromVector3(pos)).toArray())
        assertEquals(PI / 2, Vector2().angleTo(Vector2(1.0, 0.0)), 1e-12)
        assertEquals(0.0, Vector2(-0.5, 0.0).round().x, 0.0)
    }

    @Test
    fun intersectionsHandleInsideOriginsAndDegenerateTriangles() {
        val box = Box3(Vector3(-1.0, -1.0, -1.0), Vector3(1.0, 1.0, 1.0))
        val ray = Ray()
        assertComponents(doubleArrayOf(0.0, 0.0, -1.0), assertNotNull(ray.intersectBox(box, Vector3())).toArray())
        assertComponents(doubleArrayOf(0.0, 0.0, -1.0), assertNotNull(ray.intersectSphere(Sphere(Vector3(), 1.0), Vector3())).toArray())
        val triangle = Triangle(Vector3(), Vector3(1.0, 0.0, 0.0), Vector3(0.0, 1.0, 0.0))
        assertTrue(box.intersectsTriangle(triangle))
        assertTrue(Box2(Vector2(-1.0, -1.0), Vector2()).containsPoint(Vector2()))
        assertFalse(box.intersectsTriangle(triangle.copy().apply { a.x += 5; b.x += 5; c.x += 5 }))
        assertNull(Triangle().getInterpolation(Vector3(), Vector2(), Vector2(), Vector2(), Vector2()))
        assertEquals(0.5, triangle.getArea())
    }

    @Test
    fun rayToSegmentMatchesThreeJs() {
        val ray = Ray(Vector3(1.0, 2.0, 3.0), Vector3(1.0, -2.0, 4.0).normalize())
        val onRay = Vector3()
        val onSegment = Vector3()
        val distance = ray.distanceSqToSegment(Vector3(-10.0, 5.0, 8.0), Vector3(8.0, -6.0, 8.0), onRay, onSegment)
        assertEquals(2.640284054228516, distance, 1e-12)
        assertComponents(doubleArrayOf(2.3653970303421565, -0.7307940606843131, 8.461588121368626), onRay.toArray())
        assertComponents(doubleArrayOf(1.5530019367333772, -2.0601678502259526, 8.0), onSegment.toArray())
        val parallel = Line3(Vector3(0.0, 2.0, 0.0), Vector3(4.0, 2.0, 0.0))
        assertEquals(4.0, Line3(Vector3(), Vector3(1.0, 0.0, 0.0)).distanceSqToLine3(parallel))
    }

    @Test
    fun frustumsUseTheSelectedClipDepth() {
        for (system in CoordinateSystem.entries) for (reversed in listOf(false, true)) {
            val p = Matrix4().makePerspective(-1.0, 1.0, 1.0, -1.0, 1.0, 10.0, system, reversed)
            val frustum = Frustum().setFromProjectionMatrix(p, system, reversed)
            assertTrue(frustum.containsPoint(Vector3(0.0, 0.0, -5.0)))
            assertFalse(frustum.containsPoint(Vector3(0.0, 0.0, -0.5)))
            assertFalse(frustum.containsPoint(Vector3(0.0, 0.0, -11.0)))
        }
    }

    @Test
    fun arrayFrustumUsesActiveCameraProjectionsAndMeshWorldBounds() {
        val left = PerspectiveCamera(fov = 60.0, near = 1.0, far = 10.0)
        val right = PerspectiveCamera(fov = 60.0, near = 1.0, far = 10.0)
        right.coordinateSystem = CoordinateSystem.WebGPU
        right.reversedDepth = true
        right.updateProjectionMatrix()
        right.position.x = 10.0
        right.updateMatrixWorld()
        val mesh = Mesh(Box())
        mesh.position.set(10.0, 0.0, -5.0)
        mesh.updateMatrixWorld()

        val frustums = FrustumArray().setFromArrayCamera(listOf(left, right))
        assertTrue(frustums.intersectsObject(mesh))
        assertTrue(frustums.copy().intersectsObject(mesh))
        assertComponents(doubleArrayOf(9.5, -0.5, -5.5), Box3().setFromObject(mesh, true).min.toArray())
        frustums.setFromArrayCamera(listOf(left))
        assertFalse(frustums.intersectsObject(mesh))
    }

    @Test
    fun cssColorAndSphericalIrradianceMatchThreeJs() {
        val color = Color("hsl(215, 75%, 55%)")
        assertComponents(doubleArrayOf(0.03713395305732496, 0.2083017778510861, 0.7629030623387469), color.toArray())
        assertEquals(0x663399, Color("rebeccapurple").getHex())
        assertEquals(0xff8800, Color("#f80").getHex())
        assertEquals("color(srgb-linear 0.250 0.500 0.750)", Color(0.25, 0.5, 0.75).getStyle(ColorSpace.LinearSRGB))
        val sh = SphericalHarmonics3().fromArray(DoubleArray(27) { it / 16.0 })
        val irradiance = sh.getIrradianceAt(Vector3(2.0, 3.0, 6.0).normalize(), Vector3())
        assertComponents(doubleArrayOf(1.4956276989795918, 1.7068293163265305, 1.9180309336734693), irradiance.toArray())
    }

    @Test
    fun namedAndComponentColorsConvertToWorkingColors() {
        assertEquals(0x663399, CssColorNames.REBECCA_PURPLE.toColor().getHex())
        assertEquals(0xff0000, HSL(h = 0.0, s = 1.0, l = 0.5).toColor().getHex())
        assertEquals(Color(0.5, 0.25, 0.0), RGB(0.5, 0.25, 0.0).toColor())
        assertEquals(0x804000, RGB(0.5, 0.25, 0.0).toColor(ColorSpace.SRGB).getHex())
    }

    @Test
    fun vectorRoundingUsesKotlinSemantics() {
        assertEquals(2.0, Vector2(2.5, 0.0).round().x)
    }

    @Test
    fun interpolantsMatchThreeJsWhileSeekingAcrossIntervals() {
        val times = doubleArrayOf(0.0, 1.0, 3.0, 7.0)
        val samples = doubleArrayOf(0.0, 4.0, -2.0, 9.0)
        val cubic = CubicInterpolant(times, samples, 1)
        val queries = doubleArrayOf(0.2, 1.0, 2.0, 5.0, 7.0, 2.0, -4.0)
        assertComponents(doubleArrayOf(0.912, 4.0, 1.15625, 2.0625, 9.0, 1.15625, 0.0),
            DoubleArray(queries.size) { cubic.evaluate(queries[it])[0] })
        val result = DoubleArray(1)
        assertSame(result, LinearInterpolant(times, samples, 1, result).evaluate(2.0))
        assertEquals(1.0, result[0])
        assertEquals(4.0, DiscreteInterpolant(times, samples, 1).evaluate(2.0)[0])

        val bezier = BezierInterpolant(doubleArrayOf(0.0, 1.0), doubleArrayOf(0.0, 2.0), 1)
        bezier.inTangents = doubleArrayOf(0.0, 0.0, 0.7, 2.2)
        bezier.outTangents = doubleArrayOf(0.2, 0.0, 1.0, 2.0)
        assertComponents(doubleArrayOf(0.1162240052401535, 0.898301385038279, 1.9893471506925127),
            doubleArrayOf(0.1, 0.4, 0.9).map { bezier.evaluate(it)[0] }.toDoubleArray())
    }

    @Test
    fun quaternionInterpolationWritesEachRotationToItsOwnOffset() {
        val start = Quaternion().toArray()
        val endA = Quaternion().setFromAxisAngle(Vector3(1.0, 0.0, 0.0), PI).toArray()
        val endB = Quaternion().setFromAxisAngle(Vector3(0.0, 1.0, 0.0), PI).toArray()
        val interpolant = QuaternionLinearInterpolant(doubleArrayOf(0.0, 1.0), start + start + endA + endB, 8)
        val sine = kotlin.math.sqrt(0.5)
        assertComponents(doubleArrayOf(sine, 0.0, 0.0, sine, 0.0, sine, 0.0, sine), interpolant.evaluate(0.5))
    }

    private fun assertComponents(expected: DoubleArray, actual: DoubleArray) {
        assertEquals(expected.size, actual.size)
        for (index in expected.indices) assertEquals(expected[index], actual[index], 1e-12, "component $index")
    }
}
