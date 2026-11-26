package com.aitorgf.threekt.core

import com.aitorgf.threekt.cameras.Camera
import com.aitorgf.threekt.materials.Material
import com.aitorgf.threekt.math.Euler
import com.aitorgf.threekt.math.Frustum
import com.aitorgf.threekt.math.MathUtils
import com.aitorgf.threekt.math.Matrix3
import com.aitorgf.threekt.math.Matrix4
import com.aitorgf.threekt.math.Quaternion
import com.aitorgf.threekt.math.Vector3
import com.aitorgf.threekt.renderers.WebGLRenderer
import com.aitorgf.threekt.scenes.Scene
import kotlin.js.JsExport

/**
 * Base class for nodes in a scene graph. Each node has a local transform and a world transform derived
 * from its ancestors.
 *
 * Deviations from Three.js:
 * - Kotlin type checks replace the `type` string and the `isObject3D`-style flags.
 * - [children] is a read-only snapshot; change it through [add], [remove], [attach] and [clear].
 * - From JavaScript, [add] and [remove] take one object per call.
 * - Adding a node to one of its descendants throws [IllegalArgumentException] instead of logging an error,
 *   because the resulting cycle would make every traversal recurse forever.
 * - `getObjectByProperty()` and `getObjectsByProperty()`, which look up untyped properties by name, are
 *   replaced by [getObjectById], [getObjectByName] and the typed [getObjectsByName].
 * - `toJSON()`, `animations`, the shadow callbacks and the custom depth and distance materials are not
 *   available yet, because the object loader, the animation system and shadow maps have not been ported.
 */
@JsExport
open class Object3D : EventDispatcher() {
    /** Unique number of this node, assigned in creation order. */
    val id: Int = nextId++

    /** Random UUID of this node. Not kept by [clone]. */
    var uuid: String = MathUtils.generateUUID()

    /** Optional name used to identify the object. */
    var name = ""

    /** Parent node, or null when this node is a root. */
    var parent: Object3D? = null
        private set

    private val childNodes = mutableListOf<Object3D>()

    /** Returns a snapshot of the immediate children in insertion order. */
    val children: List<Object3D> get() = childNodes.toList()

    /** Up direction used by [lookAt]. Defaults to a copy of [DEFAULT_UP]. */
    val up: Vector3 = DEFAULT_UP.copy()

    /** Local translation relative to the parent. */
    val position = Vector3()

    /** Local rotation in radians. Kept in sync with [quaternion]: changing either updates the other. */
    val rotation = Euler()

    /** Local rotation as a quaternion. Kept in sync with [rotation]: changing either updates the other. */
    val quaternion = Quaternion()

    /** Local scale along each axis. */
    val scale = Vector3(1.0, 1.0, 1.0)

    /** The camera view matrix multiplied by [matrixWorld], set by the renderer before each draw. */
    val modelViewMatrix = Matrix4()

    /** The normal matrix of [modelViewMatrix], set by the renderer before each draw. */
    val normalMatrix = Matrix3()

    /** Current local transform. */
    val matrix = Matrix4()

    /** Current transform from local coordinates to world coordinates. */
    val matrixWorld = Matrix4()

    /**
     * Whether [updateMatrixWorld] rebuilds [matrix] from [position], [quaternion], [scale] and [pivot].
     * Disable it to supply [matrix] directly. Defaults to [DEFAULT_MATRIX_AUTO_UPDATE].
     */
    var matrixAutoUpdate = DEFAULT_MATRIX_AUTO_UPDATE

    /**
     * Whether [updateMatrixWorld] computes [matrixWorld] from the parent. When disabled, [matrixWorld] keeps
     * whatever value it is given. The renderer also skips updating the scene and the camera when it is
     * disabled on them. Defaults to [DEFAULT_MATRIX_WORLD_AUTO_UPDATE].
     */
    var matrixWorldAutoUpdate = DEFAULT_MATRIX_WORLD_AUTO_UPDATE

    /**
     * Whether [matrixWorld] must be recomputed on the next [updateMatrixWorld]. Set by [updateMatrix]; set it
     * yourself after changing [matrix] by hand.
     */
    var matrixWorldNeedsUpdate = false

    /** Layers this object belongs to. It is rendered by cameras, and picked by raycasters, that share one. */
    val layers = Layers()

    /** Whether this object and its descendants are drawn. Hidden objects still update their matrices. */
    var visible = true

    /** Whether this object is rendered into shadow maps. Stored for shadow support, which is not ported yet. */
    var castShadow = false

    /** Whether this object receives shadows. Stored for shadow support, which is not ported yet. */
    var receiveShadow = false

    /**
     * Whether the renderer skips this object when [intersectsFrustum] reports it outside the camera
     * frustum. Disable it for objects whose geometry is moved in the shader.
     */
    var frustumCulled = true

    /**
     * Drawing priority. Lower values are drawn first, overriding the depth sorting of opaque and transparent
     * objects.
     */
    var renderOrder = 0

    /**
     * Marks the object as static, meaning that its transform and geometry do not change after the first
     * render. Stored for renderers that optimize static objects; the WebGL renderer ignores it, as in
     * Three.js.
     */
    var static = false

    /**
     * Custom data attached to the object. Three.js uses a plain object that `copy()` deep-copies through
     * JSON; Threekt uses a map, and [copy] copies the map without copying its values.
     */
    val userData: MutableMap<String, Any?> = linkedMapOf()

    /**
     * Point in local space around which [rotation] and [scale] are applied, or null to use the origin.
     * [position] still sets where the origin goes in the parent.
     */
    var pivot: Vector3? = null

    init {
        rotation.onChange { quaternion.setFromEuler(rotation, false) }
        quaternion.onChange { rotation.setFromQuaternion(quaternion, rotation.order, false) }
    }

    /**
     * Called by the renderer just before this object is drawn, once per drawn [group] of a multi-material
     * object. Does nothing by default; override it to update uniforms or other state per draw.
     */
    open fun onBeforeRender(
        renderer: WebGLRenderer,
        scene: Scene,
        camera: Camera,
        geometry: BufferGeometry,
        material: Material,
        group: BufferGeometry.Group?,
    ) = Unit

    /** Called by the renderer just after this object is drawn. Does nothing by default. */
    open fun onAfterRender(
        renderer: WebGLRenderer,
        scene: Scene,
        camera: Camera,
        geometry: BufferGeometry,
        material: Material,
        group: BufferGeometry.Group?,
    ) = Unit

    /**
     * Applies [matrix] on top of the current local transform and updates [position], [quaternion] and
     * [scale] from the result. With a [pivot], the position is adjusted so that the pivot stays fixed.
     */
    fun applyMatrix4(matrix: Matrix4) {
        if (matrixAutoUpdate) updateMatrix()
        this.matrix.premultiply(matrix)
        this.matrix.decompose(position, quaternion, scale)
        val pivot = pivot
        if (pivot != null) {
            position.copy(pivot).applyMatrix4(this.matrix).sub(pivot)
        }
    }

    /** Applies the rotation [q] on top of the current rotation. Returns this object. */
    fun applyQuaternion(q: Quaternion): Object3D {
        quaternion.premultiply(q)
        return this
    }

    /** Sets the rotation to [angle] radians around the normalized [axis]. */
    fun setRotationFromAxisAngle(axis: Vector3, angle: Double) {
        quaternion.setFromAxisAngle(axis, angle)
    }

    /** Sets the rotation from [euler]. */
    fun setRotationFromEuler(euler: Euler) {
        quaternion.setFromEuler(euler, true)
    }

    /** Sets the rotation from the unscaled rotation in the upper 3×3 part of [m]. */
    fun setRotationFromMatrix(m: Matrix4) {
        quaternion.setFromRotationMatrix(m)
    }

    /** Sets the rotation from the normalized quaternion [q]. */
    fun setRotationFromQuaternion(q: Quaternion) {
        quaternion.copy(q)
    }

    /** Rotates by [angle] radians around the normalized [axis] in local space. Returns this object. */
    fun rotateOnAxis(axis: Vector3, angle: Double): Object3D {
        q1.setFromAxisAngle(axis, angle)
        quaternion.multiply(q1)
        return this
    }

    /**
     * Rotates by [angle] radians around the normalized [axis] in world space. Assumes that no ancestor is
     * rotated. Returns this object.
     */
    fun rotateOnWorldAxis(axis: Vector3, angle: Double): Object3D {
        q1.setFromAxisAngle(axis, angle)
        quaternion.premultiply(q1)
        return this
    }

    /** Rotates by [angle] radians around the local X axis. Returns this object. */
    fun rotateX(angle: Double): Object3D = rotateOnAxis(xAxis, angle)

    /** Rotates by [angle] radians around the local Y axis. Returns this object. */
    fun rotateY(angle: Double): Object3D = rotateOnAxis(yAxis, angle)

    /** Rotates by [angle] radians around the local Z axis. Returns this object. */
    fun rotateZ(angle: Double): Object3D = rotateOnAxis(zAxis, angle)

    /** Moves by [distance] along the normalized [axis] in local space. Returns this object. */
    fun translateOnAxis(axis: Vector3, distance: Double): Object3D {
        v1.copy(axis).applyQuaternion(quaternion)
        position.add(v1.multiplyScalar(distance))
        return this
    }

    /** Moves by [distance] along the local X axis. Returns this object. */
    fun translateX(distance: Double): Object3D = translateOnAxis(xAxis, distance)

    /** Moves by [distance] along the local Y axis. Returns this object. */
    fun translateY(distance: Double): Object3D = translateOnAxis(yAxis, distance)

    /** Moves by [distance] along the local Z axis. Returns this object. */
    fun translateZ(distance: Double): Object3D = translateOnAxis(zAxis, distance)

    /** Converts [vector] in place from this object's local space to world space and returns it. */
    fun localToWorld(vector: Vector3): Vector3 {
        updateWorldMatrix(updateParents = true, updateChildren = false)
        return vector.applyMatrix4(matrixWorld)
    }

    /** Converts [vector] in place from world space to this object's local space and returns it. */
    fun worldToLocal(vector: Vector3): Vector3 {
        updateWorldMatrix(updateParents = true, updateChildren = false)
        return vector.applyMatrix4(m1.copy(matrixWorld).invert())
    }

    /**
     * Rotates this node to face the world-space point [target], using [up] as the vertical reference.
     * Cameras and lights orient their local negative Z axis toward [target]; other nodes use positive Z.
     * The world matrices of this node and its ancestors are refreshed first. Not supported for objects
     * with rotated or non-uniformly scaled parents.
     */
    fun lookAt(target: Vector3) {
        lookAtTarget.copy(target)
        val parent = parent
        updateWorldMatrix(updateParents = true, updateChildren = false)
        lookAtPosition.setFromMatrixPosition(matrixWorld)
        if (looksAlongNegativeZ) {
            m1.lookAt(lookAtPosition, lookAtTarget, up)
        } else {
            m1.lookAt(lookAtTarget, lookAtPosition, up)
        }
        quaternion.setFromRotationMatrix(m1)
        if (parent != null) {
            m1.extractRotation(parent.matrixWorld)
            q1.setFromRotationMatrix(m1)
            quaternion.premultiply(q1.invert())
        }
    }

    /** Rotates this node to face the world-space point ([x], [y], [z]), as [lookAt] with a vector. */
    @JsExport.Ignore
    fun lookAt(x: Double, y: Double, z: Double) = lookAt(Vector3(x, y, z))

    /** Whether [lookAt] aims the local negative Z axis at the target, as cameras and lights do. */
    internal open val looksAlongNegativeZ: Boolean get() = false

    /**
     * Attaches [object] to this node, removing it from its previous parent. The child dispatches an `added`
     * event and this node dispatches a `childadded` [ChildEvent]. Throws [IllegalArgumentException] if this
     * would create a cycle. Returns this node.
     */
    fun add(obj: Object3D): Object3D {
        var ancestor: Object3D? = this
        while (ancestor != null) {
            require(ancestor !== obj) { "Object3D.add: an object can't be added to itself or its descendants." }
            ancestor = ancestor.parent
        }
        obj.removeFromParent()
        obj.parent = this
        childNodes.add(obj)
        obj.dispatchEvent(DispatchedEvent(ADDED))
        dispatchEvent(ChildEvent(CHILD_ADDED, obj))
        return this
    }

    /**
     * Adds each of [objects] in order, as [add] with one object. Three.js accepts several objects in the
     * same `add()` call; Kotlin/JS cannot export a variadic overload, so from JavaScript add them one by one.
     */
    @JsExport.Ignore
    fun add(vararg objects: Object3D): Object3D {
        for (child in objects) add(child)
        return this
    }

    /**
     * Detaches [object] if it belongs to this node and clears its parent. The removed child dispatches a
     * `removed` event and this node dispatches a `childremoved` [ChildEvent]. Returns this node.
     */
    fun remove(obj: Object3D): Object3D {
        val index = childNodes.indexOfFirst { it === obj }
        if (index < 0) return this
        obj.parent = null
        childNodes.removeAt(index)
        obj.dispatchEvent(DispatchedEvent(REMOVED))
        dispatchEvent(ChildEvent(CHILD_REMOVED, obj))
        return this
    }

    /** Removes each of [objects] in order, as [remove] with one object. Kotlin only, like the variadic [add]. */
    @JsExport.Ignore
    fun remove(vararg objects: Object3D): Object3D {
        for (child in objects) remove(child)
        return this
    }

    /** Removes this node from its parent, if any. Returns this node. */
    fun removeFromParent(): Object3D {
        parent?.remove(this)
        return this
    }

    /** Removes all children. Returns this node. */
    fun clear(): Object3D = remove(*childNodes.toTypedArray())

    /**
     * Adds [object] as a child while keeping its world transform, by folding the change of parent into its
     * local transform. Dispatches the same events as [add]. Returns this node.
     */
    fun attach(obj: Object3D): Object3D {
        var ancestor: Object3D? = this
        while (ancestor != null) {
            require(ancestor !== obj) { "Object3D.attach: an object can't be attached to itself or its descendants." }
            ancestor = ancestor.parent
        }
        updateWorldMatrix(updateParents = true, updateChildren = false)
        val inverse = Matrix4().copy(matrixWorld).invert()
        val previousParent = obj.parent
        if (previousParent != null) {
            previousParent.updateWorldMatrix(updateParents = true, updateChildren = false)
            inverse.multiply(previousParent.matrixWorld)
        }
        obj.applyMatrix4(inverse)
        obj.removeFromParent()
        obj.parent = this
        childNodes.add(obj)
        obj.updateWorldMatrix(updateParents = false, updateChildren = true)
        obj.dispatchEvent(DispatchedEvent(ADDED))
        dispatchEvent(ChildEvent(CHILD_ADDED, obj))
        return this
    }

    /** Returns the first node in this subtree, in [traverse] order, whose [id] is [id], or null. */
    fun getObjectById(id: Int): Object3D? = findFirst { it.id == id }

    /** Returns the first node in this subtree, in [traverse] order, named [name], or null. */
    fun getObjectByName(name: String): Object3D? = findFirst { it.name == name }

    /**
     * Returns every node in this subtree named [name], in [traverse] order. Three.js does this with
     * `getObjectsByProperty('name', name)`.
     */
    fun getObjectsByName(name: String): Array<Object3D> {
        val result = mutableListOf<Object3D>()
        traverse { if (it.name == name) result.add(it) }
        return result.toTypedArray()
    }

    private fun findFirst(predicate: (Object3D) -> Boolean): Object3D? {
        if (predicate(this)) return this
        for (child in childNodes) {
            val found = child.findFirst(predicate)
            if (found != null) return found
        }
        return null
    }

    /** Writes the world position of this node to [target] and returns it. */
    fun getWorldPosition(target: Vector3): Vector3 {
        updateWorldMatrix(updateParents = true, updateChildren = false)
        return target.setFromMatrixPosition(matrixWorld)
    }

    /** Writes the world rotation of this node to [target] and returns it. */
    fun getWorldQuaternion(target: Quaternion): Quaternion {
        updateWorldMatrix(updateParents = true, updateChildren = false)
        matrixWorld.decompose(decomposePosition, target, decomposeScale)
        return target
    }

    /** Writes the world scale of this node to [target] and returns it. */
    fun getWorldScale(target: Vector3): Vector3 {
        updateWorldMatrix(updateParents = true, updateChildren = false)
        matrixWorld.decompose(decomposePosition, decomposeQuaternion, target)
        return target
    }

    /**
     * Writes the direction of the local positive Z axis in world space, normalized, to [target] and returns
     * it. Cameras return their viewing direction, the negative Z axis, instead.
     */
    open fun getWorldDirection(target: Vector3): Vector3 {
        updateWorldMatrix(updateParents = true, updateChildren = false)
        val e = matrixWorld.elements
        return target.set(e[8], e[9], e[10]).normalize()
    }

    /**
     * Adds the hits of the ray of [raycaster] with this object to [intersects]. Does nothing by default;
     * meshes, lines, points and sprites test their geometry. Returns whether [Raycaster] should go on to test
     * the children, which Three.js signals by returning `false` to stop.
     */
    open fun raycast(raycaster: Raycaster, intersects: MutableList<Intersection>): Boolean = true

    /**
     * Returns whether this object may be inside [frustum]. The renderer skips objects for which this returns
     * false when [frustumCulled] is set. Returns true by default; drawable objects test their bounds. Not
     * exported to JavaScript, because [Frustum] is not exported yet.
     */
    @JsExport.Ignore
    open fun intersectsFrustum(frustum: Frustum): Boolean = true

    /** Visits this node and its descendants in depth-first order. Avoid changing the graph from [callback]. */
    fun traverse(callback: (Object3D) -> Unit) {
        callback(this)
        for (child in childNodes) child.traverse(callback)
    }

    /** Like [traverse], but skips hidden nodes and their descendants. */
    fun traverseVisible(callback: (Object3D) -> Unit) {
        if (!visible) return
        callback(this)
        for (child in childNodes) child.traverseVisible(callback)
    }

    /** Visits the ancestors of this node from its parent up to the root. */
    fun traverseAncestors(callback: (Object3D) -> Unit) {
        val parent = parent ?: return
        callback(parent)
        parent.traverseAncestors(callback)
    }

    /**
     * Composes the local [matrix] from [position], [quaternion], [scale] and [pivot], and sets
     * [matrixWorldNeedsUpdate].
     */
    fun updateMatrix() {
        matrix.compose(position, quaternion, scale)
        val pivot = pivot
        if (pivot != null) {
            val px = pivot.x
            val py = pivot.y
            val pz = pivot.z
            val te = matrix.elements
            te[12] += px - te[0] * px - te[4] * py - te[8] * pz
            te[13] += py - te[1] * px - te[5] * py - te[9] * pz
            te[14] += pz - te[2] * px - te[6] * py - te[10] * pz
        }
        matrixWorldNeedsUpdate = true
    }

    /**
     * Updates the world transform of this node and its descendants. A world matrix is recomputed when its
     * node needs it or when [force] is set, and then every descendant is recomputed too. If this node has a
     * parent, the parent's world matrix must be current.
     */
    open fun updateMatrixWorld(force: Boolean = false) {
        if (matrixAutoUpdate) updateMatrix()
        var forceChildren = force
        if (matrixWorldNeedsUpdate || force) {
            if (matrixWorldAutoUpdate) {
                val parent = parent
                if (parent == null) {
                    matrixWorld.copy(matrix)
                } else {
                    matrixWorld.multiplyMatrices(parent.matrixWorld, matrix)
                }
            }
            matrixWorldNeedsUpdate = false
            forceChildren = true
        }
        for (child in childNodes) child.updateMatrixWorld(forceChildren)
    }

    /**
     * Updates the world transform of this node, of its ancestors first when [updateParents] is set, and of
     * its descendants when [updateChildren] is set. [force] recomputes the world matrix even when it is not
     * marked as outdated.
     */
    open fun updateWorldMatrix(updateParents: Boolean, updateChildren: Boolean, force: Boolean = false) {
        val parent = parent
        if (updateParents && parent != null) {
            parent.updateWorldMatrix(updateParents = true, updateChildren = false)
        }
        if (matrixAutoUpdate) updateMatrix()
        var forceChildren = force
        if (matrixWorldNeedsUpdate || force) {
            if (matrixWorldAutoUpdate) {
                if (parent == null) {
                    matrixWorld.copy(matrix)
                } else {
                    matrixWorld.multiplyMatrices(parent.matrixWorld, matrix)
                }
            }
            matrixWorldNeedsUpdate = false
            forceChildren = true
        }
        if (updateChildren) {
            for (child in childNodes) child.updateWorldMatrix(updateParents = false, updateChildren = true, force = forceChildren)
        }
    }

    /**
     * Returns a copy of this object, and of its descendants when [recursive] is set. Geometries and
     * materials are shared with the original. Subclasses override this method to return their own type;
     * Three.js creates the copy with `new this.constructor()` instead.
     */
    open fun clone(recursive: Boolean = true): Object3D = Object3D().copy(this, recursive)

    /**
     * Copies the transform and settings of [source] into this object, and adds clones of its children when
     * [recursive] is set. Does not copy [id], [uuid] or [parent]. Returns this object.
     */
    open fun copy(source: Object3D, recursive: Boolean = true): Object3D {
        name = source.name
        up.copy(source.up)
        position.copy(source.position)
        rotation.order = source.rotation.order
        quaternion.copy(source.quaternion)
        scale.copy(source.scale)
        pivot = source.pivot?.copy()
        matrix.copy(source.matrix)
        matrixWorld.copy(source.matrixWorld)
        matrixAutoUpdate = source.matrixAutoUpdate
        matrixWorldAutoUpdate = source.matrixWorldAutoUpdate
        matrixWorldNeedsUpdate = source.matrixWorldNeedsUpdate
        layers.mask = source.layers.mask
        visible = source.visible
        castShadow = source.castShadow
        receiveShadow = source.receiveShadow
        frustumCulled = source.frustumCulled
        renderOrder = source.renderOrder
        static = source.static
        userData.clear()
        userData.putAll(source.userData)
        if (recursive) {
            for (child in source.childNodes) add(child.clone())
        }
        return this
    }

    /** Dispatches a `dispose` event so that renderers and listeners can release resources for this object. */
    fun dispose() {
        dispatchEvent(DispatchedEvent(DISPOSE))
    }

    private val lookAtTarget = Vector3()
    private val lookAtPosition = Vector3()
    private val v1 = Vector3()
    private val q1 = Quaternion()
    private val m1 = Matrix4()
    private val decomposePosition = Vector3()
    private val decomposeQuaternion = Quaternion()
    private val decomposeScale = Vector3()

    companion object {
        private var nextId = 0
        private val xAxis = Vector3(1.0, 0.0, 0.0)
        private val yAxis = Vector3(0.0, 1.0, 0.0)
        private val zAxis = Vector3(0.0, 0.0, 1.0)

        /** Type of the event a node dispatches when it is added to a parent. */
        const val ADDED = "added"

        /** Type of the event a node dispatches when it is removed from its parent. */
        const val REMOVED = "removed"

        /** Type of the [ChildEvent] a node dispatches when a child is added to it. */
        const val CHILD_ADDED = "childadded"

        /** Type of the [ChildEvent] a node dispatches when a child is removed from it. */
        const val CHILD_REMOVED = "childremoved"

        /** Type of the event dispatched by [dispose]. */
        const val DISPOSE = "dispose"

        /** Default [up] direction for new nodes. Changing it affects nodes created afterwards. */
        val DEFAULT_UP = Vector3(0.0, 1.0, 0.0)

        /** Default [matrixAutoUpdate] for new nodes. */
        var DEFAULT_MATRIX_AUTO_UPDATE = true

        /** Default [matrixWorldAutoUpdate] for new nodes. */
        var DEFAULT_MATRIX_WORLD_AUTO_UPDATE = true
    }
}
