package com.aitorgf.threekt.objects

import com.aitorgf.threekt.cameras.PerspectiveCamera
import com.aitorgf.threekt.core.Raycaster
import com.aitorgf.threekt.geometries.Box
import com.aitorgf.threekt.materials.MeshBasicMaterial
import com.aitorgf.threekt.materials.SpriteMaterial
import com.aitorgf.threekt.math.Color
import com.aitorgf.threekt.math.Matrix4
import com.aitorgf.threekt.math.Vector2
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class InstancedMeshTest {
    @Test
    fun matricesAndColorsRoundTripAndColorsStartWhite() {
        val mesh = InstancedMesh(Box(), MeshBasicMaterial(), 3)
        assertEquals(Matrix4(), mesh.getMatrixAt(2, Matrix4()))
        assertNull(mesh.instanceColor)
        val matrix = Matrix4().makeTranslation(1.0, 2.0, 3.0)
        mesh.setMatrixAt(1, matrix)
        assertEquals(matrix, mesh.getMatrixAt(1, Matrix4()))
        mesh.setColorAt(0, Color(0.5, 0.25, 0.0))
        assertEquals(Color(1.0, 1.0, 1.0), mesh.getColorAt(2, Color()))
        assertEquals(0.25, mesh.getColorAt(0, Color()).g)
        mesh.computeBoundingBox()
        assertEquals(3.5, mesh.boundingBox!!.max.z)
    }

    @Test
    fun raycastingReportsTheNearestInstance() {
        val mesh = InstancedMesh(Box(), MeshBasicMaterial(), 2)
        mesh.setMatrixAt(0, Matrix4().makeTranslation(0.0, 0.0, -5.0))
        mesh.setMatrixAt(1, Matrix4().makeTranslation(0.0, 0.0, -2.0))
        val raycaster = Raycaster()
        raycaster.setFromCamera(Vector2(), PerspectiveCamera())
        val hits = raycaster.intersectObject(mesh)
        assertEquals(1, hits.first().instanceId)
        assertEquals(1.5, hits.first().distance, 1e-9)
        assertEquals(setOf(0, 1), hits.map { it.instanceId }.toSet())
    }

    @Test
    fun spritesFaceTheCameraForRaycasting() {
        val sprite = Sprite(SpriteMaterial()).apply {
            position.set(0.0, 0.0, -4.0)
            scale.set(2.0, 2.0, 1.0)
        }
        val camera = PerspectiveCamera().apply { position.x = 0.5 }
        camera.lookAt(sprite.position)
        camera.updateMatrixWorld()
        sprite.updateMatrixWorld()
        val raycaster = Raycaster()
        raycaster.setFromCamera(Vector2(), camera)
        val hit = raycaster.intersectObject(sprite).single()
        assertSame(sprite, hit.obj)
        assertEquals(0.5, hit.uv!!.x, 1e-9)
        assertEquals(0.5, hit.uv!!.y, 1e-9)
        raycaster.setFromCamera(Vector2(0.9, 0.9), camera)
        assertEquals(0, raycaster.intersectObject(sprite).size)
    }
}
