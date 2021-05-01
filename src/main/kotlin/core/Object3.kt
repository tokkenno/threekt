package core

import math.*
import utils.events.EventEmitter
import utils.events.Observable

@ExperimentalJsExport
@JsExport
open class Object3 {
    companion object {
        private var idGenerator: Int = 0
        private val defaultUp: Vector3 = Vector3(0.0, 1.0, 0.0)
    }

    val id: Int by lazy { idGenerator++ }

    var name: String = ""

    val up: MutableVector3 = Object3.defaultUp.toMutable()
    val position: MutableVector3 = MutableVector3()
    val rotation: Euler = Euler()
    val scale: MutableVector3 = MutableVector3(1.0, 1.0, 1.0)

    private val quaternion: Quaternion = Quaternion()

    val matrix: MutableMatrix4 = MutableMatrix4()
    val matrixWorld: MutableMatrix4 = MutableMatrix4()

    var matrixAutoUpdate: Boolean = true
    var matrixWorldNeedsUpdate: Boolean = false

    var parent: Object3? = null
    var children: Array<Object3> = emptyArray()

    /**
     * When this is set, it checks every frame if the object is in the frustum of the camera before rendering the object. If set to `false` the object gets rendered every frame even if it is not in the frustum of the camera. Default is `true`.
     */
    var frustumCulled: Boolean = true

    var renderOrder: Int = 0

    var visible: Boolean = true

    val layers: Layers = Layers()

    var userData: Any? = null

    /** Whether the object gets rendered into shadow map. */
    var castShadow: Boolean = false

    /** Whether the material receives shadows. */
    var receiveShadow: Boolean = false

    private val onAddEmitter = EventEmitter()

    val onAdd: Observable
        get() = onAddEmitter

    private val onRemoveEmitter = EventEmitter()

    val onRemove: Observable
        get() = onRemoveEmitter

    init {
        this.quaternion.onChange.subscribe {
            this.rotation.setFromQuaternion(this.quaternion)
        }

        this.rotation.onChange.subscribe {
            this.quaternion.setFromEuler(this.rotation)
        }
    }

    open fun clone(recursive: Boolean = true): Object3 {
        return Object3().copy(this, recursive)
    }

    open fun copy(source: Object3, recursive: Boolean = true): Object3 {
        this.name = source.name

        this.up.set(source.up)

        this.position.set(source.position)
        this.rotation.order = source.rotation.order
        this.quaternion.set(source.quaternion)
        this.scale.set(source.scale)

        this.matrix.set(source.matrix)
        this.matrixWorld.set(source.matrixWorld)

        this.matrixAutoUpdate = source.matrixAutoUpdate
        this.matrixWorldNeedsUpdate = source.matrixWorldNeedsUpdate

        this.layers.setLayers(source.layers)
        this.visible = source.visible

        this.castShadow = source.castShadow
        this.receiveShadow = source.receiveShadow

        this.frustumCulled = source.frustumCulled
        this.renderOrder = source.renderOrder

        this.userData = JSON.parse(JSON.stringify(source.userData))

        if (recursive) {
            source.children.forEach { child ->
                this.add(child.clone())
            }
        }

        return this
    }

    fun add(vararg others: Object3): Object3 {
        for (other in others) {
            val otherParent = other.parent
            if (otherParent != null) {
                otherParent.remove(other)
            }
            other.parent = this
            val newChildren = this.children.toMutableSet()
            newChildren.add(other)
            this.children = newChildren.toTypedArray()
            this.onAddEmitter.emit(other)
        }
        return this
    }

    fun attach(vararg others: Object3): Object3 {
        for (other in others) {
            this.updateWorldMatrix(true, false)

            val m1 = MutableMatrix4()
            m1.set(this.matrixWorld).invert()

            val otherParent = other.parent
            if (otherParent != null) {
                otherParent.updateWorldMatrix(true, false)
                m1.multiply(otherParent.matrixWorld)
            }

            other.apply(m1)
            this.add(other)
            other.updateWorldMatrix(false, true)
        }
        return this
    }

    fun remove(vararg others: Object3): Object3 {
        for (other in others) {
            other.parent = null
            this.children = this.children.filter { it.id == other.id }.toTypedArray()
            this.onRemoveEmitter.emit(other)
        }
        return this
    }

    fun clear(): Object3 {
        this.remove(*this.children)
        return this
    }

    @JsName("applyMatrix4")
    fun apply(matrix: Matrix4): Unit {
        if (this.matrixAutoUpdate) {
            this.updateMatrix()
        }
        this.matrix.premultiply(matrix)
            .decompose(this.position, this.quaternion, this.scale)
    }

    open fun beforeRender(): Unit {}
    open fun afterRender(): Unit {}

    fun rotateOnAxis(axis: Vector3, angle: Radian): Object3 {
        this.quaternion.premultiply(Quaternion().setFromAxisAngle(axis, angle))
        return this
    }

    fun rotateX(angle: Radian): Object3 {
        return this.rotateOnAxis(Vector3.xAxis, angle)
    }

    fun rotateY(angle: Radian): Object3 {
        return this.rotateOnAxis(Vector3.yAxis, angle)
    }

    fun rotateZ(angle: Radian): Object3 {
        return this.rotateOnAxis(Vector3.zAxis, angle)
    }

    open protected fun updateMatrix(): Unit {
        this.matrix.compose(this.position, this.quaternion, this.scale)
        this.matrixWorldNeedsUpdate = true
    }

    open protected fun updateMatrixWorld(force: Boolean = false) {
        if (this.matrixAutoUpdate) {
            this.updateMatrix()
        }

        var needForce = force

        if (this.matrixWorldNeedsUpdate || needForce) {
            val thisParent = this.parent
            if (thisParent == null) {
                this.matrixWorld.set(this.matrix)
            } else {
                this.matrixWorld.multiplyAndSet(thisParent.matrixWorld, this.matrix)
            }

            this.matrixWorldNeedsUpdate = false
            needForce = true
        }

        this.children.forEach { child ->
            child.updateMatrixWorld(needForce)
        }
    }

    open protected fun updateWorldMatrix(updateParents: Boolean, updateChildren: Boolean) {
        val parent = this.parent

        if (updateParents && parent != null) {
            parent.updateWorldMatrix(true, false)
        }

        if (this.matrixAutoUpdate) {
            this.updateMatrix()
        }

        if (parent == null) {
            this.matrixWorld.set(this.matrix)
        } else {
            this.matrixWorld.multiplyAndSet(parent.matrixWorld, this.matrix)
        }

        if (updateChildren) {
            this.children.forEach {
                it.updateWorldMatrix(false, true)
            }
        }
    }
}
