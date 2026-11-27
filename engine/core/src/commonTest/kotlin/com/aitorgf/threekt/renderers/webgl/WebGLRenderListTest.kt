package com.aitorgf.threekt.renderers.webgl

import com.aitorgf.threekt.cameras.PerspectiveCamera
import com.aitorgf.threekt.materials.Material
import com.aitorgf.threekt.materials.MeshBasicMaterial
import com.aitorgf.threekt.core.Object3D
import com.aitorgf.threekt.geometries.Box
import com.aitorgf.threekt.math.Frustum
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

    // Empty geometries have no bounds, so culling is disabled unless a test needs it.
    private fun mesh(z: Double, material: Material) = Mesh(material = material).apply {
        position.z = z
        frustumCulled = false
    }

    private fun build(vararg meshes: Object3D, camera: PerspectiveCamera = PerspectiveCamera(near = 1.0, far = 10.0)): WebGLRenderList {
        val scene = Scene()
        meshes.forEach { scene.add(it) }
        scene.updateMatrixWorld()
        camera.updateMatrixWorld()
        val projScreenMatrix = Matrix4().multiplyMatrices(camera.projectionMatrix, camera.matrixWorldInverse)
        val frustum = Frustum().setFromProjectionMatrix(projScreenMatrix)
        return WebGLRenderList().apply { build(scene, camera, projScreenMatrix, frustum) }
    }

    @Test
    fun opaqueMeshesAreNearestFirstAndTransparentMeshesFarthestFirst() {
        val farOpaque = mesh(-8.0, opaqueMaterial)
        val nearOpaque = mesh(-2.0, opaqueMaterial)
        val nearTransparent = mesh(-3.0, transparentMaterial)
        val farTransparent = mesh(-6.0, transparentMaterial)
        val list = build(nearTransparent, farOpaque, farTransparent, nearOpaque)
        assertEquals(listOf(nearOpaque, farOpaque), list.opaque.map { it.obj })
        assertEquals(listOf(farTransparent, nearTransparent), list.transparent.map { it.obj })
    }

    @Test
    fun opaqueMeshesAreGroupedByMaterialBeforeDepth() {
        val other = MeshBasicMaterial()
        val farFirstMaterial = mesh(-8.0, opaqueMaterial)
        val nearOtherMaterial = mesh(-2.0, other)
        val nearFirstMaterial = mesh(-3.0, opaqueMaterial)
        val list = build(nearOtherMaterial, farFirstMaterial, nearFirstMaterial)
        assertEquals(listOf(nearFirstMaterial, farFirstMaterial, nearOtherMaterial), list.opaque.map { it.obj })
    }

    @Test
    fun renderOrderOverridesDepthAndTiesKeepCreationOrder() {
        val first = mesh(-5.0, transparentMaterial)
        val second = mesh(-5.0, transparentMaterial)
        val near = mesh(-2.0, transparentMaterial).apply { renderOrder = -1 }
        val list = build(second, near, first)
        assertEquals(listOf(near, first, second), list.transparent.map { it.obj })
    }

    @Test
    fun linesAreSortedWithMeshes() {
        val line = Line(material = opaqueMaterial).apply { position.z = -2.0; frustumCulled = false }
        val mesh = mesh(-4.0, opaqueMaterial)
        val list = build(mesh, line)
        assertEquals(listOf(line, mesh), list.opaque.map { it.obj })
    }

    @Test
    fun pointsAreSortedWithMeshes() {
        val points = Points(material = opaqueMaterial).apply { position.z = -2.0; frustumCulled = false }
        val mesh = mesh(-4.0, opaqueMaterial)
        assertEquals(listOf(points, mesh), build(mesh, points).opaque.map { it.obj })
    }

    @Test
    fun hiddenObjectsAndTheirDescendantsAreSkipped() {
        val hiddenParent = mesh(-3.0, opaqueMaterial).apply { visible = false }
        val child = mesh(-4.0, opaqueMaterial)
        hiddenParent.add(child)
        val shown = mesh(-5.0, opaqueMaterial)
        assertEquals(listOf(shown), build(hiddenParent, shown).opaque.map { it.obj })
    }

    @Test
    fun objectsOutsideTheCameraLayersAreSkippedButTheirChildrenAreNot() {
        val parent = mesh(-3.0, opaqueMaterial).apply { layers.set(1) }
        val child = mesh(-1.0, opaqueMaterial)
        parent.add(child)
        assertEquals(listOf(parent.children[0]), build(parent).opaque.map { it.obj })
        val camera = PerspectiveCamera(near = 1.0, far = 10.0).apply { layers.enable(1) }
        assertEquals(2, build(parent, camera = camera).opaque.size)
    }

    @Test
    fun frustumCullingUsesTheGeometryBoundsUnlessDisabled() {
        val inside = Mesh(Box(), opaqueMaterial).apply { position.z = -5.0 }
        val behind = Mesh(Box(), opaqueMaterial).apply { position.z = 5.0 }
        val unculled = Mesh(Box(), opaqueMaterial).apply {
            position.z = 5.0
            frustumCulled = false
        }
        assertEquals(listOf(inside, unculled), build(inside, behind, unculled).opaque.map { it.obj })
    }
}
