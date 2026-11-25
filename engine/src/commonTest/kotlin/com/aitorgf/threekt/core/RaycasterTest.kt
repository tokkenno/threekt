package com.aitorgf.threekt.core

import com.aitorgf.threekt.cameras.OrthographicCamera
import com.aitorgf.threekt.cameras.PerspectiveCamera
import com.aitorgf.threekt.geometries.PlaneGeometry
import com.aitorgf.threekt.math.Vector2
import com.aitorgf.threekt.objects.LineSegments
import com.aitorgf.threekt.objects.Mesh
import com.aitorgf.threekt.objects.Points
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class RaycasterTest {
    @Test
    fun bothProjectionsReturnInterpolatedUvsInDistanceOrder() {
        val farMesh = Mesh(PlaneGeometry(2.0, 2.0))
        val nearMesh = Mesh(PlaneGeometry(2.0, 2.0)).apply { position.z = 1.0 }
        val raycaster = Raycaster()
        for (camera in listOf(PerspectiveCamera(), OrthographicCamera())) {
            camera.position.z = 3.0
            raycaster.setFromCamera(Vector2(), camera)
            val hits = raycaster.intersectObjects(arrayOf(farMesh, nearMesh))
            val hit = hits.first()
            assertSame(nearMesh, hit.object3d)
            assertEquals(2.0, hit.distance, 1e-10)
            assertEquals(0.5, hit.uv!!.x, 1e-10)
            assertEquals(0.5, hit.uv!!.y, 1e-10)
            assertTrue(hits.any { it.object3d === farMesh })
        }
    }

    @Test
    fun lineThresholdIsMeasuredInWorldUnits() {
        val lines = LineSegments(BufferGeometry().setAttribute("position",
            Float32BufferAttribute(floatArrayOf(-1f, 0.25f, 0f, 1f, 0.25f, 0f), 3),
        )).apply { scale.y = 2.0 }
        val camera = PerspectiveCamera().apply { position.z = 3.0 }
        val raycaster = Raycaster()
        raycaster.setFromCamera(Vector2(), camera)
        raycaster.lineThreshold = 0.4
        assertTrue(raycaster.intersectObject(lines).isEmpty())
        raycaster.lineThreshold = 0.6
        val hit = raycaster.intersectObject(lines).single()
        assertEquals(0.5, hit.point.y)
        assertEquals(3.0, hit.distance)
    }

    @Test
    fun indexedPointsRespectDrawRange() {
        val points = Points(BufferGeometry()
            .setAttribute("position", Float32BufferAttribute(floatArrayOf(0f, 0f, 1f, 0f, 0f, 0f), 3))
            .setIndex(intArrayOf(0, 1)).apply { setDrawRange(1, 1) })
        val raycaster = Raycaster()
        val camera = PerspectiveCamera().apply { position.z = 3.0 }
        raycaster.setFromCamera(Vector2(), camera)
        val hit = raycaster.intersectObject(points).single()
        assertEquals(1, hit.index)
        assertEquals(3.0, hit.distance)
    }
}
