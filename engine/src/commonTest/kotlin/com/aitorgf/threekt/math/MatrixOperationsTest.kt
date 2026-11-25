package com.aitorgf.threekt.math

import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertSame

class MatrixOperationsTest {
    @Test
    fun matrix2ReadsColumnMajorArraysAndResetsToIdentity() {
        val matrix = Matrix2().fromArray(doubleArrayOf(99.0, 2.0, 3.0, 5.0, 7.0), 1)
        val expected = Matrix2(2.0, 5.0, 3.0, 7.0)
        assertEquals(expected, matrix)
        assertEquals(expected.hashCode(), matrix.hashCode())
        assertSame(matrix, matrix.identity())
        assertEquals(Matrix2(), matrix)
    }

    @Test
    fun matrix3TransposesExtractsBasisAndWritesToArrayOffsets() {
        val matrix = Matrix3(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 10.0)
        val x = Vector3()
        val y = Vector3()
        val z = Vector3()
        matrix.extractBasis(x, y, z)
        assertEquals(Vector3(1.0, 4.0, 7.0), x)
        assertEquals(Vector3(2.0, 5.0, 8.0), y)
        assertEquals(Vector3(3.0, 6.0, 10.0), z)
        val transposed = DoubleArray(9)
        assertSame(matrix, matrix.transposeIntoArray(transposed))
        assertContentEquals(doubleArrayOf(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 10.0), transposed)
        val output = DoubleArray(10) { -1.0 }
        matrix.copy().transpose().toArray(output, 1)
        assertContentEquals(doubleArrayOf(-1.0) + transposed, output)
        assertEquals(matrix, Matrix3().fromArray(output, 1).transpose())
        assertEquals(-3.0, matrix.determinant())
    }

    @Test
    fun matrix3UvTransformsAboutTheSpecifiedCenter() {
        val matrix = Matrix3().setUvTransform(0.25, 0.5, 2.0, 3.0, PI / 2, 0.5, 0.5)
        val center = Vector2(0.5, 0.5).applyMatrix3(matrix)
        val neighbor = Vector2(1.5, 0.5).applyMatrix3(matrix)
        assertEquals(0.0, center.distanceTo(Vector2(0.75, 1.0)), 1e-12)
        assertEquals(0.0, neighbor.distanceTo(Vector2(0.75, -2.0)), 1e-12)

        val point = Vector2(1.0, 0.0).applyMatrix3(Matrix3().translate(2.0, 0.0).rotate(PI / 2).scale(2.0, 3.0))
        assertEquals(0.0, point.distanceTo(Vector2(0.0, -9.0)), 1e-12)
    }

    @Test
    fun singularMatricesBecomeZeroAndAffineDeterminantsIncludeReflections() {
        val singular3 = Matrix3().makeScale(2.0, 0.0)
        assertEquals(0.0, singular3.determinant())
        assertEquals(Matrix3().multiplyScalar(0.0), singular3.invert())

        val affine = Matrix4().makeScale(-2.0, 3.0, 4.0).setPosition(3.0, 4.0, 5.0)
        assertEquals(-24.0, affine.determinantAffine())
        assertEquals(affine.determinant(), affine.determinantAffine())
        val sourcePoint = Vector3(2.0, 4.0, 8.0)
        val restored = sourcePoint.copy().applyMatrix4(affine).applyMatrix4(affine.copy().invert())
        assertEquals(0.0, restored.distanceTo(sourcePoint), 1e-12)
        assertEquals(Matrix4().multiplyScalar(0.0), Matrix4().makeScale(0.0, 1.0, 1.0).invert())
        assertEquals(Matrix4().multiplyScalar(0.0), Matrix4().multiplyScalar(0.0).invert())
    }

    @Test
    fun matrix4ExtractsOrientationAndPreservesTranslationDuringLookAt() {
        val x = Vector3()
        val y = Vector3()
        val z = Vector3()
        val matrix = Matrix4().makeTranslation(5.0, 7.0, 11.0)
        val eye = Vector3(0.0, 0.0, 1.0)
        matrix.lookAt(eye, Vector3(), Vector3(0.0, 0.0, 1.0))
        matrix.extractBasis(x, y, z)
        assertEquals(1.0, x.length(), 1e-12)
        assertEquals(1.0, y.length(), 1e-12)
        assertEquals(1.0, z.length(), 1e-12)
        assertEquals(0.0, x.dot(y), 1e-12)
        assertEquals(0.0, x.copy().cross(y).distanceTo(z), 1e-12)
        assertEquals(Vector3(5.0, 7.0, 11.0), Vector3().setFromMatrixPosition(matrix))

        val scaled = Matrix4().makeBasis(x, y, z).scale(Vector3(2.0, 3.0, 4.0))
        val rotation = Matrix4().extractRotation(scaled)
        assertEquals(0.0, Vector3().setFromMatrixColumn(rotation, 1).distanceTo(y), 1e-12)
        assertEquals(4.0, scaled.getMaxScaleOnAxis(), 1e-12)
        assertEquals(Vector3(5.0, 7.0, 11.0), Vector3().setFromMatrixPosition(scaled.copyPosition(matrix)))
    }

    @Test
    fun matrix4ShearAndTransposeKeepColumnMajorLayout() {
        val shear = Matrix4().makeShear(1.0, 2.0, 3.0, 4.0, 5.0, 6.0)
        assertEquals(Vector3(22.0, 21.0, 13.0), Vector3(1.0, 2.0, 3.0).applyMatrix4(shear))
        val array = DoubleArray(17) { -1.0 }
        shear.copy().transpose().toArray(array, 1)
        assertEquals(-1.0, array[0])
        assertEquals(shear, Matrix4().fromArray(array, 1).transpose())
        val inner = Matrix3().setFromMatrix4(shear)
        assertEquals(shear, Matrix4().setFromMatrix3(inner))
    }
}
