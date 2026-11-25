package com.aitorgf.threekt.core

import com.aitorgf.threekt.cameras.PerspectiveCamera
import com.aitorgf.threekt.math.Vector3
import com.aitorgf.threekt.scenes.Scene
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class Object3DTest {
    @Test
    fun worldUpdateCanRefreshAncestorsWithoutRefreshingSiblingOrChildNodes() {
        val root = Object3D()
        val parent = Object3D()
        val node = Object3D()
        val child = Object3D()
        val sibling = Object3D()
        root.add(parent).add(sibling)
        parent.add(node)
        node.add(child)
        root.position.x = 1.0
        parent.position.x = 2.0
        node.position.x = 4.0
        child.position.x = 8.0
        sibling.position.x = 16.0

        node.updateWorldMatrix(updateParents = true, updateChildren = false)

        assertEquals(1.0, root.matrixWorld.elements[12])
        assertEquals(3.0, parent.matrixWorld.elements[12])
        assertEquals(7.0, node.matrixWorld.elements[12])
        assertEquals(0.0, child.matrixWorld.elements[12])
        assertEquals(0.0, sibling.matrixWorld.elements[12])

        root.position.x = 10.0
        node.updateWorldMatrix(updateParents = false, updateChildren = true)
        assertEquals(1.0, root.matrixWorld.elements[12])
        assertEquals(15.0, child.matrixWorld.elements[12])
        assertEquals(0.0, sibling.matrixWorld.elements[12])
    }

    @Test
    fun manualLocalMatrixSurvivesWorldUpdatesUntilAutomaticCompositionIsRestored() {
        val parent = Object3D()
        val node = Object3D()
        parent.add(node)
        parent.position.x = 10.0
        node.position.x = 3.0
        node.matrixAutoUpdate = false
        node.matrix.makeTranslation(7.0, 0.0, 0.0)

        parent.updateMatrixWorld()
        assertEquals(17.0, node.matrixWorld.elements[12])
        assertEquals(7.0, node.matrix.elements[12])
        node.matrixAutoUpdate = true
        parent.updateMatrixWorld()
        assertEquals(13.0, node.matrixWorld.elements[12])
    }

    @Test
    fun worldMatrixIncludesAncestorTransforms() {
        val scene = Scene()
        val parent = Object3D()
        val child = Object3D()
        scene.position.x = 10.0
        parent.position.y = 5.0
        parent.quaternion.setFromAxisAngle(Vector3(0.0, 0.0, 1.0), PI / 2)
        parent.scale.x = 2.0
        child.position.x = 3.0
        scene.add(parent)
        parent.add(child)

        scene.updateMatrixWorld()

        val worldOrigin = Vector3().applyMatrix4(child.matrixWorld)
        assertEquals(10.0, worldOrigin.x, 1e-12)
        assertEquals(11.0, worldOrigin.y, 1e-12)
        assertEquals(0.0, worldOrigin.z, 1e-12)
        assertEquals(3.0, Vector3().applyMatrix4(child.matrix).x)

        scene.position.x = 20.0
        scene.updateMatrixWorld()
        assertEquals(20.0, Vector3().applyMatrix4(child.matrixWorld).x, 1e-12)
    }

    @Test
    fun addingToNewParentMovesTheChild() {
        val first = Object3D()
        val second = Object3D()
        val child = Object3D()
        first.add(child)
        second.add(child)

        assertTrue(first.children.isEmpty())
        assertEquals(listOf(child), second.children)
        assertSame(second, child.parent)

        second.remove(child)
        assertTrue(second.children.isEmpty())
        assertNull(child.parent)
    }

    @Test
    fun addingAnAncestorLeavesTheTreeIntact() {
        val parent = Object3D()
        val child = Object3D()
        parent.add(child)

        assertFailsWith<IllegalArgumentException> { child.add(parent) }
        assertFailsWith<IllegalArgumentException> { child.add(child) }
        assertSame(parent, child.parent)
        assertEquals(listOf(child), parent.children)
        assertTrue(child.children.isEmpty())
    }

    @Test
    fun lookAtAimsCamerasAlongNegativeZAndOtherNodesAlongPositiveZ() {
        val target = Vector3(3.0, 0.0, 0.0)
        val node = Object3D()
        node.lookAt(target)
        val forward = Vector3(0.0, 0.0, 1.0).applyQuaternion(node.quaternion)
        assertEquals(1.0, forward.x, 1e-12)

        val camera = PerspectiveCamera()
        camera.position.set(0.0, 0.0, 5.0)
        camera.lookAt(Vector3())
        val direction = camera.getWorldDirection(Vector3())
        assertEquals(-1.0, direction.z, 1e-12)
        assertEquals(0.0, direction.x, 1e-12)
    }

    @Test
    fun lookAtCompensatesForTheParentRotationAndUsesTheUpVector() {
        val parent = Object3D()
        parent.quaternion.setFromAxisAngle(Vector3(0.0, 1.0, 0.0), PI / 2)
        val camera = PerspectiveCamera()
        parent.add(camera)
        camera.lookAt(Vector3(0.0, 0.0, -10.0))
        val direction = camera.getWorldDirection(Vector3())
        assertEquals(-1.0, direction.z, 1e-12)

        val tilted = PerspectiveCamera()
        tilted.up.set(1.0, 0.0, 0.0)
        tilted.position.set(0.0, 0.0, 5.0)
        tilted.lookAt(Vector3())
        val up = Vector3(0.0, 1.0, 0.0).applyQuaternion(tilted.quaternion)
        assertEquals(1.0, up.x, 1e-12)
    }
}
