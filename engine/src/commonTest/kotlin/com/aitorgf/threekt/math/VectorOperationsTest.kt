package com.aitorgf.threekt.math

import com.aitorgf.threekt.core.Float32BufferAttribute
import kotlin.math.PI
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class VectorOperationsTest {
    @Test
    fun vector2RotatesAroundAPivotAndAppliesAffineTransforms() {
        val vector = Vector2(3.0, 2.0)
        val pivot = Vector2(1.0, 2.0)
        assertSame(vector, vector.rotateAround(pivot, PI / 2))
        assertEquals(0.0, vector.distanceTo(Vector2(1.0, 4.0)), 1e-12)
        vector.applyMatrix3(Matrix3().makeTranslation(-3.0, 2.0))
        assertEquals(0.0, vector.distanceTo(Vector2(-2.0, 6.0)), 1e-12)

        vector.width = 3.0
        vector.height = 4.0
        assertEquals(5.0, vector.length())
        assertEquals(7.0, vector.manhattanLength())
        assertEquals(7.0, vector.manhattanDistanceTo(Vector2(4.0, -2.0)))
        assertEquals(4.0, Vector2(2.0, 0.0).cross(Vector2(0.0, 2.0)))
        assertEquals(PI / 2, Vector2(0.0, 1.0).angle(), 1e-12)
        assertEquals(Vector2(1.0, 3.0), Vector2().lerpVectors(Vector2(), Vector2(4.0, 12.0), 0.25))
    }

    @Test
    fun vector3ProjectionsCrossProductsAndReflectionUseIndependentComponents() {
        val source = Vector3(2.0, -3.0, 4.0)
        val axis = Vector3(0.0, 2.0, 0.0)
        assertEquals(0.0, source.copy().projectOnVector(axis).distanceTo(Vector3(0.0, -3.0, 0.0)), 1e-12)
        assertEquals(Vector3(), source.copy().projectOnVector(Vector3()))
        assertEquals(Vector3(2.0, 0.0, 4.0), source.copy().projectOnPlane(axis))
        assertEquals(Vector3(2.0, 3.0, 4.0), source.copy().reflect(axis.normalize()))
        val x = Vector3(1.0, 0.0, 0.0)
        val y = Vector3(0.0, 1.0, 0.0)
        assertEquals(Vector3(0.0, 0.0, 1.0), x.copy().cross(y))
        assertEquals(PI / 2, x.angleTo(y), 1e-12)
        assertEquals(PI / 2, x.angleTo(Vector3()), 1e-12)
        assertEquals(Vector3(4.0, -9.0, 16.0), Vector3().multiplyVectors(source, Vector3(2.0, 3.0, 4.0)))
    }

    @Test
    fun vector3DistinguishesRotationFromDirectionAndPositionTransforms() {
        val x = Vector3(1.0, 0.0, 0.0)
        val z = Vector3(0.0, 0.0, 1.0)
        val y = Vector3(0.0, 1.0, 0.0)
        val matrix = Matrix4().makeRotationZ(PI / 2).scale(Vector3(2.0, 3.0, 4.0))
        matrix.setPosition(5.0, 6.0, 7.0)
        assertEquals(0.0, x.copy().transformDirection(matrix).distanceTo(y), 1e-12)
        assertEquals(0.0, x.copy().applyAxisAngle(z, PI / 2).distanceTo(y), 1e-12)
        assertEquals(0.0, x.copy().applyEuler(Euler(z = PI / 2)).distanceTo(y), 1e-12)
        assertEquals(0.0, x.copy().applyNormalMatrix(Matrix3().getNormalMatrix(matrix)).distanceTo(y), 1e-12)
        assertEquals(Vector3(2.0, 3.0, 4.0), Vector3().setFromMatrixScale(matrix))
        assertEquals(Vector3(5.0, 6.0, 7.0), Vector3().setFromMatrixPosition(matrix))
        assertEquals(1.0, Vector3().randomDirection(Random(7)).length(), 1e-12)
    }

    @Test
    fun vector4KeepsHomogeneousWAndHandlesAxisAngleSingularities() {
        val matrix = Matrix4().makeTranslation(10.0, 20.0, 30.0)
        assertEquals(Vector4(21.0, 42.0, 63.0, 2.0), Vector4(1.0, 2.0, 3.0, 2.0).applyMatrix4(matrix))
        assertEquals(Vector4(10.0, 20.0, 30.0, 1.0), Vector4().setFromMatrixPosition(matrix))
        assertEquals(Vector4(1.0, 0.0, 0.0, 0.0), Vector4().setAxisAngleFromRotationMatrix(Matrix4()))

        val q = Quaternion().setFromAxisAngle(Vector3(0.0, 0.0, 1.0), PI / 3)
        val axisAngle = Vector4().setAxisAngleFromQuaternion(q)
        assertEquals(1.0, axisAngle.z, 1e-12)
        assertEquals(PI / 3, axisAngle.w, 1e-12)
        assertEquals(Vector4(2.0, 4.0, 6.0, 8.0), Vector4(1.0, 2.0, 3.0, 4.0).multiply(Vector4().setScalar(2.0)))
        assertEquals(Vector4(-2.0, 1.0, 2.0, -2.0), Vector4(-3.0, 1.0, 8.0, -5.0).clampScalar(-2.0, 2.0))
    }

    @Test
    fun mutableVectorArrayAndAttributeOffsetsRejectInvalidRangesBeforeMutating() {
        val vector = Vector3()
        val attribute = Float32BufferAttribute(floatArrayOf(1f, 2f, 3f, 99f, 4f, 5f, 6f, 99f), 4)
        assertSame(vector, vector.fromBufferAttribute(attribute, 1))
        assertEquals(Vector3(4.0, 5.0, 6.0), vector)
        val output = DoubleArray(5) { -1.0 }
        assertSame(output, vector.toArray(output, 1))
        assertContentEquals(doubleArrayOf(-1.0, 4.0, 5.0, 6.0, -1.0), output)
        assertEquals(vector, Vector3().fromArray(output, 1))
        assertFailsWith<IllegalArgumentException> { vector.fromArray(output, 3) }
        assertFailsWith<IllegalArgumentException> { vector.toArray(output, -1) }
        assertFailsWith<IllegalArgumentException> { vector.fromBufferAttribute(attribute, 2) }
        assertFailsWith<IllegalArgumentException> { vector.fromBufferAttribute(Float32BufferAttribute(floatArrayOf(1f), 1), 0) }
        assertFailsWith<IndexOutOfBoundsException> { vector[3] }
        assertFailsWith<IndexOutOfBoundsException> { vector[-1] = 0.0 }
        assertEquals(Vector3(4.0, 5.0, 6.0), vector)
    }

    @Test
    fun mutableVectorLengthMethodsPreserveDirectionAndHandleZero() {
        val vector = Vector2(3.0, 4.0)
        assertSame(vector, vector.clampLength(0.0, 2.5))
        assertEquals(Vector2(1.5, 2.0), vector)
        assertEquals(0.0, vector.setLength(10.0).distanceTo(Vector2(6.0, 8.0)), 1e-12)
        assertEquals(Vector2(), Vector2().clampLength(2.0, 4.0))
        assertFailsWith<IllegalArgumentException> { vector.clampLength(4.0, 2.0) }
    }
}
