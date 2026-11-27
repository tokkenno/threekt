package com.aitorgf.threekt.math

import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class Matrix4Test {
    @Test
    fun identityPreservesPoint() {
        val point = Vector3(1.0, 2.0, 3.0)

        assertSame(point, point.applyMatrix4(Matrix4()))
        assertEquals(1.0, point.x)
        assertEquals(2.0, point.y)
        assertEquals(3.0, point.z)
    }

    @Test
    fun composeAppliesScaleThenRotationThenTranslation() {
        val rotation = Quaternion().setFromAxisAngle(Vector3(0.0, 0.0, 1.0), PI / 2)
        val matrix = Matrix4()
        val point = Vector3(1.0, 2.0, 3.0)

        assertSame(matrix, matrix.compose(Vector3(10.0, 20.0, 30.0), rotation, Vector3(2.0, 3.0, 4.0)))
        point.applyMatrix4(matrix)

        assertEquals(4.0, point.x, 1e-12)
        assertEquals(22.0, point.y, 1e-12)
        assertEquals(42.0, point.z, 1e-12)
    }

    @Test
    fun applyMatrix4DividesByHomogeneousCoordinate() {
        val matrix = Matrix4()
        matrix.elements[11] = 1.0
        val point = Vector3(6.0, 9.0, 2.0).applyMatrix4(matrix)

        assertEquals(2.0, point.x, 1e-12)
        assertEquals(3.0, point.y, 1e-12)
        assertEquals(2.0 / 3.0, point.z, 1e-12)
    }
}
