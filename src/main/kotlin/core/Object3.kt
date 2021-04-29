package core

import math.*
import utils.events.EventEmitter
import utils.events.Observable

@ExperimentalJsExport
@JsExport
open class Object3 {
    companion object {
        private var idGenerator: Int = 0
    }

    val id: Int by lazy { idGenerator++ }

    var name: String = ""

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
            this.updateWorldMatrix(true, false);

            val m1 = MutableMatrix4()
            m1.copy(this.matrixWorld).invert();

            val otherParent = other.parent
            if (otherParent != null) {
                otherParent.updateWorldMatrix(true, false);
                m1.multiply(otherParent.matrixWorld);
            }

            other.apply(m1);
            this.add(other);
            other.updateWorldMatrix(false, true);
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

    open fun updateMatrix() {
        this.matrix.compose(position, quaternion, scale)
        this.matrixWorldNeedsUpdate = true
    }

    @JsName("applyMatrix4")
    fun apply(matrix: Matrix4): Unit {
        if ( this.matrixAutoUpdate ) {
            this.updateMatrix()
        }
        this.matrix.premultiply( matrix )
            .decompose( this.position, this.quaternion, this.scale )
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

    private fun updateWorldMatrix(updateParents: Boolean, updateChildren: Boolean) {
        val parent = this.parent;

        if ( updateParents && parent != null ) {
            parent.updateWorldMatrix( true, false );
        }

        if ( this.matrixAutoUpdate ) {
            this.updateMatrix()
        };

        if ( parent == null ) {
            this.matrixWorld.copy( this.matrix );
        } else {
            this.matrixWorld.multiply(parent.matrixWorld, this.matrix );
        }

        if (updateChildren) {
            this.children.forEach {
                it.updateWorldMatrix(false, true)
            }
        }
    }
}
