package com.aitorgf.threekt.core

import com.aitorgf.threekt.cameras.PerspectiveCamera
import com.aitorgf.threekt.math.Euler
import com.aitorgf.threekt.math.Matrix4
import com.aitorgf.threekt.math.Quaternion
import com.aitorgf.threekt.math.Vector3
import com.aitorgf.threekt.objects.Group
import com.aitorgf.threekt.objects.Mesh
import com.aitorgf.threekt.scenes.Scene
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
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

    @Test
    fun rotationAndQuaternionStayInSync() {
        val node = Object3D()
        node.rotation.y = PI / 2
        val expected = Quaternion().setFromAxisAngle(Vector3(0.0, 1.0, 0.0), PI / 2)
        assertEquals(expected.y, node.quaternion.y, 1e-12)
        assertEquals(expected.w, node.quaternion.w, 1e-12)

        node.quaternion.setFromAxisAngle(Vector3(1.0, 0.0, 0.0), PI / 4)
        assertEquals(PI / 4, node.rotation.x, 1e-12)
        assertEquals(0.0, node.rotation.y, 1e-12)

        node.rotation.set(0.1, 0.2, 0.3, Euler.Order.YXZ)
        val check = Euler().setFromQuaternion(node.quaternion, Euler.Order.YXZ)
        assertEquals(0.1, check.x, 1e-12)
        assertEquals(0.2, check.y, 1e-12)
        assertEquals(0.3, check.z, 1e-12)
    }

    @Test
    fun rotateAndTranslateFollowTheLocalAxes() {
        val node = Object3D()
        node.rotateY(PI / 2)
        node.translateZ(2.0)
        assertEquals(2.0, node.position.x, 1e-12)
        assertEquals(0.0, node.position.z, 1e-12)
        node.rotateOnWorldAxis(Vector3(0.0, 1.0, 0.0), -PI / 2)
        assertEquals(0.0, node.rotation.y, 1e-12)
        node.translateX(1.0)
        assertEquals(3.0, node.position.x, 1e-12)
    }

    @Test
    fun addAndRemoveDispatchEvents() {
        val parent = Object3D()
        val child = Object3D()
        val events = mutableListOf<String>()
        child.addEventListener(Object3D.ADDED) { events.add("added") }
        child.addEventListener(Object3D.REMOVED) { events.add("removed") }
        parent.addEventListener(Object3D.CHILD_ADDED) { events.add("childadded:" + ((it as ChildEvent).child === child)) }
        parent.addEventListener(Object3D.CHILD_REMOVED) { events.add("childremoved:" + ((it as ChildEvent).child === child)) }
        parent.add(child)
        child.removeFromParent()
        assertEquals(listOf("added", "childadded:true", "removed", "childremoved:true"), events)

        val other = Object3D()
        parent.add(child, other)
        assertEquals(2, parent.children.size)
        parent.clear()
        assertTrue(parent.children.isEmpty())
        assertNull(other.parent)
    }

    @Test
    fun attachKeepsTheWorldTransform() {
        val a = Object3D().apply { position.set(1.0, 2.0, 3.0); rotation.y = PI / 3 }
        val b = Object3D().apply { position.set(-4.0, 0.0, 1.0); scale.setScalar(2.0) }
        val child = Object3D().apply { position.set(1.0, 0.0, 0.0) }
        a.add(child)
        val before = child.getWorldPosition(Vector3())
        b.attach(child)
        assertSame(b, child.parent)
        val after = child.getWorldPosition(Vector3())
        assertEquals(before.x, after.x, 1e-9)
        assertEquals(before.y, after.y, 1e-9)
        assertEquals(before.z, after.z, 1e-9)
    }

    @Test
    fun pivotKeepsItsPointFixed() {
        val node = Object3D()
        node.pivot = Vector3(1.0, 0.0, 0.0)
        node.rotation.z = PI
        node.updateMatrixWorld()
        val pivot = Vector3(1.0, 0.0, 0.0).applyMatrix4(node.matrixWorld)
        assertEquals(1.0, pivot.x, 1e-12)
        assertEquals(0.0, pivot.y, 1e-12)
        val origin = Vector3().applyMatrix4(node.matrixWorld)
        assertEquals(2.0, origin.x, 1e-12)
    }

    @Test
    fun worldMatricesAreOnlyRecomputedWhenNeededOrForced() {
        val parent = Object3D().apply { matrixAutoUpdate = false }
        val child = Object3D().apply { matrixAutoUpdate = false }
        parent.add(child)
        parent.matrix.makeTranslation(1.0, 0.0, 0.0)
        parent.updateMatrixWorld()
        assertEquals(0.0, parent.matrixWorld.elements[12])
        parent.matrixWorldNeedsUpdate = true
        parent.updateMatrixWorld()
        assertEquals(1.0, parent.matrixWorld.elements[12])
        assertEquals(1.0, child.matrixWorld.elements[12])
        child.matrix.makeTranslation(0.0, 2.0, 0.0)
        parent.updateMatrixWorld(force = true)
        assertEquals(2.0, child.matrixWorld.elements[13])

        parent.matrixWorldAutoUpdate = false
        parent.matrix.makeTranslation(5.0, 0.0, 0.0)
        parent.updateMatrixWorld(force = true)
        assertEquals(1.0, parent.matrixWorld.elements[12])
    }

    @Test
    fun cloneCopiesTheTreeAndSharesGeometry() {
        val group = Group().apply {
            name = "group"
            position.set(1.0, 2.0, 3.0)
            rotation.x = 0.5
            layers.set(2)
            userData["key"] = "value"
        }
        val mesh = Mesh().apply { name = "mesh" }
        group.add(mesh)
        val copy = group.clone()
        assertNotSame(group.uuid, copy.uuid)
        assertEquals("group", copy.name)
        assertEquals(0.5, copy.rotation.x, 1e-12)
        assertEquals(group.quaternion, copy.quaternion)
        assertEquals(4, copy.layers.mask)
        assertEquals("value", copy.userData["key"])
        val meshCopy = copy.getObjectByName("mesh") as Mesh
        assertNotSame(mesh, meshCopy)
        assertSame(mesh.geometry, meshCopy.geometry)
        assertSame(mesh.material, meshCopy.material)
        assertEquals(0, group.clone(recursive = false).children.size)
    }

    @Test
    fun worldAccessorsAndLookups() {
        val parent = Object3D().apply { position.set(0.0, 1.0, 0.0); scale.setScalar(2.0) }
        val child = Object3D().apply { name = "child"; position.set(1.0, 0.0, 0.0) }
        parent.add(child)
        assertEquals(Vector3(2.0, 1.0, 0.0), child.getWorldPosition(Vector3()))
        assertEquals(2.0, child.getWorldScale(Vector3()).x, 1e-12)
        assertEquals(Vector3(0.0, 0.0, 1.0), child.getWorldDirection(Vector3()))
        assertTrue(Vector3(0.0, 0.0, -1.0).distanceTo(PerspectiveCamera().getWorldDirection(Vector3())) < 1e-12)
        assertSame(child, parent.getObjectById(child.id))
        assertSame(child, parent.getObjectsByName("child").single())
        assertTrue(Vector3(0.5, 0.0, 0.0).distanceTo(child.worldToLocal(Vector3(3.0, 1.0, 0.0))) < 1e-12)
        assertTrue(Vector3(4.0, 1.0, 0.0).distanceTo(child.localToWorld(Vector3(1.0, 0.0, 0.0))) < 1e-12)
        val ancestors = mutableListOf<Object3D>()
        child.traverseAncestors { ancestors.add(it) }
        assertEquals(listOf(parent), ancestors)
        assertFalse(parent.getObjectByName("missing") != null)
    }
}
