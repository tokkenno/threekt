package com.aitorgf.threekt.math

import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OBBTest {
    @Test
    fun separatingAxesDetectRotatedOverlap() {
        val a = OBB(halfSize = Vector3(1.0, 1.0, 1.0))
        // Rotated 45° about Z, its corner reaches √2 along X: 2.3 - √2 < 1 overlaps, 2.5 - √2 > 1 does not.
        val b = OBB(halfSize = Vector3(1.0, 1.0, 1.0))
            .applyMatrix4(Matrix4().makeRotationZ(PI / 4).setPosition(2.3, 0.0, 0.0))
        assertTrue(a.intersectsOBB(b))
        b.center.x = 2.5
        assertFalse(a.intersectsOBB(b))
    }

    @Test
    fun applyMatrix4ScalesHalfSizeAndRaysHitTheTransformedBox() {
        val box = OBB(halfSize = Vector3(5.0, 2.5, 3.0))
            .applyMatrix4(Matrix4().makeScale(2.0, 1.0, 1.0).setPosition(0.0, 0.0, -20.0))
        assertEquals(Vector3(10.0, 2.5, 3.0), box.halfSize)
        val hit = assertNotNull(box.intersectRay(Ray(Vector3(), Vector3(0.0, 0.0, -1.0)), Vector3()))
        assertEquals(-17.0, hit.z, 1e-12)
        assertNull(box.intersectRay(Ray(Vector3(11.0, 0.0, 0.0), Vector3(0.0, 0.0, -1.0)), Vector3()))
        assertTrue(box.containsPoint(Vector3(9.0, 0.0, -20.0)))
    }
}
