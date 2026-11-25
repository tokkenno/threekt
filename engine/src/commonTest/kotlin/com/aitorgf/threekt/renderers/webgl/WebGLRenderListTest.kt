package com.aitorgf.threekt.renderers.webgl

import com.aitorgf.threekt.cameras.PerspectiveCamera
import com.aitorgf.threekt.materials.Material
import com.aitorgf.threekt.materials.MeshBasicMaterial
import com.aitorgf.threekt.math.Matrix4
import com.aitorgf.threekt.objects.Line
import com.aitorgf.threekt.objects.Mesh
import com.aitorgf.threekt.objects.Points
import com.aitorgf.threekt.scenes.Scene
import kotlin.test.Test
import kotlin.test.assertEquals

class WebGLRenderListTest {
    private val opaqueMaterial = MeshBasicMaterial()
    private val transparentMaterial = MeshBasicMaterial().apply { transparent = true }

    private fun mesh(z: Double, material: Material) = Mesh(material = material).apply { position.z = z }

    private fun build(vararg meshes: com.aitorgf.threekt.core.Object3D): WebGLRenderList {
        val scene = Scene()
        meshes.forEach { scene.add(it) }
        scene.updateMatrixWorld()
        val camera = PerspectiveCamera(near = 1.0, far = 10.0)
        camera.updateMatrixWorld()
        val projScreenMatrix = Matrix4().multiplyMatrices(camera.projectionMatrix, camera.matrixWorldInverse)
        return WebGLRenderList().apply { build(scene, projScreenMatrix) }
    }

    @Test
    fun opaqueMeshesAreNearestFirstAndTransparentMeshesFarthestFirst() {
        val farOpaque = mesh(-8.0, opaqueMaterial)
        val nearOpaque = mesh(-2.0, opaqueMaterial)
        val nearTransparent = mesh(-3.0, transparentMaterial)
        val farTransparent = mesh(-6.0, transparentMaterial)
        val list = build(nearTransparent, farOpaque, farTransparent, nearOpaque)
        assertEquals(listOf(nearOpaque, farOpaque), list.opaque.map { it.`object` })
        assertEquals(listOf(farTransparent, nearTransparent), list.transparent.map { it.`object` })
    }

    @Test
    fun opaqueMeshesAreGroupedByMaterialBeforeDepth() {
        val other = MeshBasicMaterial()
        val farFirstMaterial = mesh(-8.0, opaqueMaterial)
        val nearOtherMaterial = mesh(-2.0, other)
        val nearFirstMaterial = mesh(-3.0, opaqueMaterial)
        val list = build(nearOtherMaterial, farFirstMaterial, nearFirstMaterial)
        assertEquals(listOf(nearFirstMaterial, farFirstMaterial, nearOtherMaterial), list.opaque.map { it.`object` })
    }

    @Test
    fun renderOrderOverridesDepthAndTiesKeepCreationOrder() {
        val first = mesh(-5.0, transparentMaterial)
        val second = mesh(-5.0, transparentMaterial)
        val near = mesh(-2.0, transparentMaterial).apply { renderOrder = -1 }
        val list = build(second, near, first)
        assertEquals(listOf(near, first, second), list.transparent.map { it.`object` })
    }

    @Test
    fun linesAreSortedWithMeshes() {
        val line = Line(material = opaqueMaterial).apply { position.z = -2.0 }
        val mesh = mesh(-4.0, opaqueMaterial)
        val list = build(mesh, line)
        assertEquals(listOf(line, mesh), list.opaque.map { it.`object` })
    }

    @Test
    fun pointsAreSortedWithMeshes() {
        val points = Points(material = opaqueMaterial).apply { position.z = -2.0 }
        val mesh = mesh(-4.0, opaqueMaterial)
        assertEquals(listOf(points, mesh), build(mesh, points).opaque.map { it.`object` })
    }

    @Test
    fun hiddenObjectsAndTheirDescendantsAreSkipped() {
        val hiddenParent = mesh(-3.0, opaqueMaterial).apply { visible = false }
        val child = mesh(-4.0, opaqueMaterial)
        hiddenParent.add(child)
        val shown = mesh(-5.0, opaqueMaterial)
        assertEquals(listOf(shown), build(hiddenParent, shown).opaque.map { it.`object` })
    }
}
