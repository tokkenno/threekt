package com.aitorgf.threekt.math

import com.aitorgf.threekt.core.Float32BufferAttribute
import kotlin.math.PI
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertSame

class QuaternionAndCoordinatesTest {
    @Test
    fun rotationBetweenOppositeUnitVectorsRemainsNormalized() {
        val x = Vector3(1.0, 0.0, 0.0)
        val other = Vector3(-1.0, 0.0, 0.0)
        val rotation = Quaternion().setFromUnitVectors(x, other)
        assertEquals(1.0, rotation.lengthSq(), 1e-12)
        assertEquals(0.0, x.copy().applyQuaternion(rotation).distanceTo(other), 1e-12)
        assertEquals(0.0, other.copy().applyQuaternion(rotation.invert()).distanceTo(x), 1e-12)
        assertEquals(Quaternion(), Quaternion(0.0, 0.0, 0.0, 0.0).normalize())
    }

    @Test
    fun quaternionInterpolationTakesTheShortestArcAndClampsRotationSteps() {
        val target = Quaternion().setFromAxisAngle(Vector3(0.0, 0.0, 1.0), PI / 2)
        val opposite = Quaternion(-target.x, -target.y, -target.z, -target.w)
        val current = Quaternion()
        assertSame(current, current.slerp(opposite, 0.5))
        assertEquals(PI / 4, current.angleTo(target), 1e-12)
        assertEquals(0.0, target.angleTo(opposite), 1e-12)
        current.rotateTowards(target, PI / 8)
        assertEquals(PI / 8, current.angleTo(target), 1e-12)
        current.rotateTowards(target, PI)
        assertEquals(0.0, current.angleTo(target), 1e-12)
    }

    @Test
    fun quaternionBufferOperationsRespectOffsetsAndAllowAliasing() {
        val a = Quaternion(1.0, 0.0, 0.0, 0.0)
        val b = Quaternion(0.0, 1.0, 0.0, 0.0)
        val array = doubleArrayOf(99.0) + a.toArray() + b.toArray()
        assertSame(array, Quaternion.multiplyQuaternionsFlat(array, 1, array, 1, array, 5))
        assertContentEquals(doubleArrayOf(99.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0), array)
        val attribute = Float32BufferAttribute(floatArrayOf(0f, 0f, 0f, 1f, 99f, 0f, 0f, 1f, 0f, 99f), 5)
        assertEquals(Quaternion(0.0, 0.0, 1.0, 0.0), Quaternion().fromBufferAttribute(attribute, 1))
        assertEquals(a.copy().multiply(b), b.copy().premultiply(a))
    }

    @Test
    fun eulerReorderingPreservesRotationAndTheArrayOffset() {
        val euler = Euler(0.2, -0.3, 0.4, Euler.Order.XYZ)
        val rotation = Quaternion().setFromEuler(euler)
        euler.reorder(Euler.Order.ZYX)
        assertEquals(Euler.Order.ZYX, euler.order)
        assertEquals(1.0, abs(rotation.dot(Quaternion().setFromEuler(euler))), 1e-12)
        val array = DoubleArray(5) { -1.0 }
        assertSame(array, euler.toArray(array, 1))
        assertEquals(euler, Euler().fromArray(array, euler.order, 1))
        assertEquals(-1.0, array[0])
        assertEquals(-1.0, array[4])
    }

    @Test
    fun sphericalCoordinatesHandleTheOriginAndAvoidBothPoles() {
        val spherical = Spherical().setFromCartesianCoords(0.0, 0.0, 0.0)
        assertEquals(Spherical(0.0, 0.0, 0.0), spherical)
        assertEquals(1e-6, spherical.makeSafe().phi)
        spherical.setFromVector3(Vector3(0.0, -2.0, 0.0))
        assertEquals(2.0, spherical.radius)
        assertEquals(PI - 1e-6, spherical.makeSafe().phi, 1e-12)
        assertEquals(spherical, Spherical().copy(spherical))
    }

    @Test
    fun cylindricalCoordinatesPreserveHeightAndSignedAzimuth() {
        val source = Vector3(-2.0, 3.0, 0.0)
        val cylindrical = Cylindrical().setFromVector3(source)
        assertEquals(Cylindrical(2.0, -PI / 2, 3.0), cylindrical)
        assertEquals(cylindrical, Cylindrical().copy(cylindrical))
        assertEquals(0.0, source.distanceTo(Vector3().setFromCylindrical(cylindrical)), 1e-12)
    }

    @Test
    fun sphericalHarmonicsUseTheBasisAndKeepCoefficientCopiesIndependent() {
        val sh = SphericalHarmonics3()
        sh.coefficients[0].set(2.0, 3.0, 4.0)
        sh.coefficients[2].set(1.0, 0.0, 0.0)
        val normal = Vector3(0.0, 0.0, 1.0)
        val sample = sh.getAt(normal, Vector3())
        assertEquals(2.0 * 0.282095 + 0.488603, sample.x, 1e-12)
        assertEquals(3.0 * 0.282095, sample.y, 1e-12)
        assertEquals(4.0 * 0.282095, sample.z, 1e-12)

        val array = DoubleArray(29) { -1.0 }
        sh.toArray(array, 1)
        val copy = SphericalHarmonics3().fromArray(array, 1)
        assertEquals(sh, copy)
        assertEquals(sh.hashCode(), copy.hashCode())
        assertEquals(sh, copy.add(sh).scale(0.5))
        assertEquals(sh, copy.zero().addScaledSH(sh, 2.0).lerp(SphericalHarmonics3(), 0.5))
        copy.copy(sh).zero()
        assertEquals(2.0, sh.coefficients[0].x)
        assertEquals(-1.0, array[28])
    }
}
