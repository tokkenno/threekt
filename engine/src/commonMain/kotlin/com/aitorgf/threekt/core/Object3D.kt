package com.aitorgf.threekt.core

import com.aitorgf.threekt.math.Matrix4
import com.aitorgf.threekt.math.Quaternion
import com.aitorgf.threekt.math.Vector3
import kotlin.js.JsExport

/**
 * Base class for nodes in a scene graph. Each node has a local transform and a world transform derived
 * from its ancestors.
 */
@JsExport
open class Object3D {
    /** Unique number of this node, assigned in creation order. */
    val id: Int = nextId++

    /**
     * Drawing priority. Lower values are drawn first, overriding the depth sorting of opaque and transparent
     * objects.
     */
    var renderOrder = 0

    /** Whether this object and its descendants are drawn. Hidden objects still update their matrices. */
    var visible = true

    /** Local translation relative to the parent. */
    val position = Vector3()
    /** Local rotation relative to the parent. */
    val quaternion = Quaternion()
    /** Local scale along each axis. */
    val scale = Vector3(1.0, 1.0, 1.0)
    /** Up direction used by [lookAt]. Defaults to a copy of [DEFAULT_UP]. */
    val up = Vector3().copy(DEFAULT_UP)

    /** Current local transform. */
    val matrix = Matrix4()
    /** Current transform from local coordinates to world coordinates. */
    val matrixWorld = Matrix4()

    /** Rebuilds the local matrix during world updates. Disable to supply [matrix] directly. */
    var matrixAutoUpdate = true

    /** Parent node, or null when this node is a root. */
    var parent: Object3D? = null
        private set

    private val childNodes = mutableListOf<Object3D>()
    /** Returns a snapshot of the immediate children in insertion order. */
    val children: List<Object3D> get() = childNodes.toList()

    /**
     * Attaches [child] to this node, removing it from its previous parent. Throws
     * [IllegalArgumentException] if this would create a cycle. Returns this node.
     */
    fun add(child: Object3D): Object3D {
        var ancestor: Object3D? = this
        while (ancestor != null) {
            require(ancestor !== child) { "Adding this child would create a cycle." }
            ancestor = ancestor.parent
        }
        child.parent?.remove(child)
        childNodes.add(child)
        child.parent = this
        return this
    }

    /** Detaches [child] if it belongs to this node and clears its parent. Returns this node. */
    fun remove(child: Object3D): Object3D {
        val index = childNodes.indexOfFirst { it === child }
        if (index >= 0) {
            childNodes.removeAt(index)
            child.parent = null
        }
        return this
    }

    /** Composes the local [matrix] from [position], [quaternion], and [scale]. */
    fun updateMatrix() {
        matrix.compose(position, quaternion, scale)
    }

    /**
     * Rotates this node to face the world-space point [target], using [up] as the vertical reference.
     * Cameras orient their local negative Z axis toward [target]; other nodes use positive Z. The world
     * matrices of this node and its ancestors are refreshed first.
     */
    fun lookAt(target: Vector3) {
        updateWorldMatrix(updateParents = true, updateChildren = false)
        lookAtPosition.setFromMatrixPosition(matrixWorld)
        if (looksAlongNegativeZ) {
            lookAtMatrix.lookAt(lookAtPosition, target, up)
        } else {
            lookAtMatrix.lookAt(target, lookAtPosition, up)
        }
        quaternion.setFromRotationMatrix(lookAtMatrix)
        val parent = parent ?: return
        lookAtMatrix.extractRotation(parent.matrixWorld)
        lookAtRotation.setFromRotationMatrix(lookAtMatrix)
        quaternion.premultiply(lookAtRotation.invert())
    }

    /** Whether [lookAt] aims the local negative Z axis at the target, as cameras and lights do. */
    internal open val looksAlongNegativeZ: Boolean get() = false

    private val lookAtPosition = Vector3()
    private val lookAtMatrix = Matrix4()
    private val lookAtRotation = Quaternion()

    /** Visits this node and its descendants in depth-first order. */
    fun traverse(visit: (Object3D) -> Unit) {
        visit(this)
        for (child in childNodes) child.traverse(visit)
    }

    /** Like [traverse], but skips hidden nodes and their descendants. */
    fun traverseVisible(visit: (Object3D) -> Unit) {
        if (!visible) return
        visit(this)
        for (child in childNodes) child.traverseVisible(visit)
    }

    /** Updates this subtree. If this node has a parent, its world matrix must be current. */
    open fun updateMatrixWorld() {
        updateWorldTransform()
        for (child in childNodes) {
            child.updateMatrixWorld()
        }
    }

    /** Updates this node and optionally its ancestors and descendants. */
    fun updateWorldMatrix(updateParents: Boolean, updateChildren: Boolean) {
        if (updateParents) parent?.updateWorldMatrix(updateParents = true, updateChildren = false)
        updateWorldTransform()
        if (updateChildren) {
            for (child in childNodes) child.updateMatrixWorld()
        }
    }

    /** Updates the world transform of this node using its parent's current transform. */
    protected open fun updateWorldTransform() {
        if (matrixAutoUpdate) updateMatrix()
        val parent = parent
        if (parent == null) {
            matrixWorld.copy(matrix)
        } else {
            matrixWorld.multiplyMatrices(parent.matrixWorld, matrix)
        }
    }

    companion object {
        private var nextId = 0

        /** Default [up] direction for new nodes. Changing it affects nodes created afterwards. */
        val DEFAULT_UP = Vector3(0.0, 1.0, 0.0)
    }
}
