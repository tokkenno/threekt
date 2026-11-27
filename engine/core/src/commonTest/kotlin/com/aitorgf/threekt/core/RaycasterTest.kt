package com.aitorgf.threekt.core

import com.aitorgf.threekt.cameras.OrthographicCamera
import com.aitorgf.threekt.cameras.PerspectiveCamera
import com.aitorgf.threekt.geometries.PlaneGeometry
import com.aitorgf.threekt.math.Vector2
import com.aitorgf.threekt.math.Vector3
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
        farMesh.updateMatrixWorld()
        nearMesh.updateMatrixWorld()
        val raycaster = Raycaster()
        for (camera in listOf(PerspectiveCamera(), OrthographicCamera())) {
            camera.position.z = 3.0
            camera.updateMatrixWorld()
            raycaster.setFromCamera(Vector2(), camera)
            val hits = raycaster.intersectObjects(arrayOf(farMesh, nearMesh))
            val hit = hits.first()
            assertSame(nearMesh, hit.obj)
            assertEquals(2.0, hit.distance, 1e-10)
            assertEquals(0.5, hit.uv!!.x, 1e-10)
            assertEquals(0.5, hit.uv!!.y, 1e-10)
            assertTrue(hits.any { it.obj === farMesh })
        }
    }

    @Test
    fun lineThresholdIsDividedByTheAverageScale() {
        // The segment is 0.25 local units from the ray; the average scale is 4 / 3, as in Three.js.
        val lines = LineSegments(BufferGeometry().setAttribute("position",
            Float32BufferAttribute(floatArrayOf(-1f, 0.25f, 0f, 1f, 0.25f, 0f), 3),
        )).apply { scale.y = 2.0 }
        lines.updateMatrixWorld()
        val camera = PerspectiveCamera().apply { position.z = 3.0 }
        camera.updateMatrixWorld()
        val raycaster = Raycaster()
        raycaster.setFromCamera(Vector2(), camera)
        raycaster.params.line.threshold = 0.3
        assertTrue(raycaster.intersectObject(lines).isEmpty())
        raycaster.params.line.threshold = 0.4
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
        camera.updateMatrixWorld()
        raycaster.setFromCamera(Vector2(), camera)
        val hit = raycaster.intersectObject(points).single()
        assertEquals(1, hit.index)
        assertEquals(3.0, hit.distance)
    }

    @Test
    fun meshHitsReportTheFaceNormalBarycoordAndInterpolatedNormal() {
        val mesh = Mesh(PlaneGeometry(2.0, 2.0)).apply { updateMatrixWorld() }
        val raycaster = Raycaster(Vector3(0.25, -0.5, 1.0), Vector3(0.0, 0.0, -1.0))
        val hit = raycaster.intersectObject(mesh).single()
        val face = hit.face!!
        assertEquals(1.0, face.normal.z, 1e-12)
        assertEquals(1.0, hit.normal!!.z, 1e-12)
        assertEquals(1.0, hit.barycoord!!.x + hit.barycoord!!.y + hit.barycoord!!.z, 1e-12)
        assertTrue(hit.faceIndex == 0 || hit.faceIndex == 1)

        // Seen from behind, a front-side mesh is not hit; the interpolated normal faces the ray otherwise.
        raycaster.set(Vector3(0.25, -0.5, -1.0), Vector3(0.0, 0.0, 1.0))
        assertTrue(raycaster.intersectObject(mesh).isEmpty())
        mesh.material.side = com.aitorgf.threekt.materials.Side.Double
        assertEquals(-1.0, raycaster.intersectObject(mesh).single().normal!!.z, 1e-12)
    }

    @Test
    fun layersAndPropagationControlWhichObjectsAreTested() {
        val parent = Mesh(PlaneGeometry(2.0, 2.0)).apply { layers.set(1) }
        val child = Mesh(PlaneGeometry(2.0, 2.0)).apply { position.z = -1.0 }
        parent.add(child)
        parent.updateMatrixWorld()
        val raycaster = Raycaster(Vector3(0.3, -0.6, 1.0), Vector3(0.0, 0.0, -1.0))
        assertSame(child, raycaster.intersectObject(parent).single().obj)
        assertTrue(raycaster.intersectObject(parent, recursive = false).isEmpty())
        raycaster.layers.enableAll()
        assertEquals(2, raycaster.intersectObject(parent).size)
    }
}
