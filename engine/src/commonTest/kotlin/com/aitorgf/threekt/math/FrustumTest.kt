package com.aitorgf.threekt.math

import com.aitorgf.threekt.cameras.OrthographicCamera
import com.aitorgf.threekt.core.Object3D
import com.aitorgf.threekt.objects.Sprite
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FrustumTest {
    @Test
    fun frustumIncludesTangentBoundsAndExcludesBoundsAcrossAPlane() {
        val camera = OrthographicCamera(near = 1.0, far = 5.0)
        val frustum = Frustum().setFromProjectionMatrix(camera.projectionMatrix)
        assertTrue(frustum.containsPoint(Vector3(1.0, 0.0, -3.0)))
        assertFalse(frustum.containsPoint(Vector3(1.1, 0.0, -3.0)))
        assertTrue(frustum.intersectsSphere(Sphere(Vector3(1.5, 0.0, -3.0), 0.5)))
        assertFalse(frustum.intersectsSphere(Sphere(Vector3(1.6, 0.0, -3.0), 0.5)))
        val box = Box3(Vector3(1.0, -0.5, -4.0), Vector3(2.0, 0.5, -2.0))
        assertTrue(frustum.intersectsBox(box))
        assertFalse(frustum.intersectsBox(box.translate(Vector3(0.1, 0.0, 0.0))))

        val sprite = Sprite()
        sprite.position.set(1.5, 0.0, -3.0)
        sprite.updateMatrixWorld()
        assertTrue(frustum.intersectsSprite(sprite))
        sprite.scale.setScalar(0.25)
        sprite.updateMatrixWorld()
        assertFalse(frustum.intersectsSprite(sprite))
        assertFalse(frustum.intersectsObject(sprite))
    }

    @Test
    fun manualFrustumPlanesAndFrustumArrayCopiesDoNotShareMutablePlanes() {
        val source = Frustum().setFromProjectionMatrix(OrthographicCamera(near = 1.0, far = 5.0).projectionMatrix)
        val planes = source.planes
        val manual = Frustum().set(planes[0], planes[1], planes[2], planes[3], planes[4], planes[5])
        assertEquals(planes, manual.planes)
        assertNotSame(planes[0], manual.planes[0])
        source.planes[0].constant = -10.0
        val point = Vector3(0.0, 0.0, -3.0)
        assertFalse(source.containsPoint(point))
        assertTrue(manual.containsPoint(point))

        val camera = OrthographicCamera(near = 1.0, far = 5.0)
        val array = FrustumArray().setFromArrayCamera(listOf(camera))
        val copy = array.copy()
        assertNotSame(array.getFrustum(0), copy.getFrustum(0))
        assertNull(copy.getFrustum(1))
        assertTrue(copy.intersectsSphere(Sphere(point, 0.25)))
        assertTrue(copy.intersectsBox(Box3().setFromCenterAndSize(point, Vector3(0.5, 0.5, 0.5))))
        array.setFromArrayCamera(emptyList<OrthographicCamera>())
        assertFalse(array.containsPoint(point))
        assertTrue(copy.containsPoint(point))
    }
}
