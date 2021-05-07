package com.aitorgf.threekt.core

import com.aitorgf.threekt.math.MutableMatrix4
import com.aitorgf.threekt.math.MutableVector3

@ExperimentalJsExport
@JsExport
open class Camera : Object3() {
    /** This is the inverse of matrixWorld. MatrixWorld contains the Matrix which has the world transform of the Camera. */
    val matrixWorldInverse: MutableMatrix4 = MutableMatrix4()

    /** This is the matrix which contains the projection. */
    val projectionMatrix: MutableMatrix4 = MutableMatrix4()

    /** The inverse of projectionMatrix. */
    val projectionMatrixInverse: MutableMatrix4 = MutableMatrix4()

    override fun clone(recursive: Boolean): Camera {
        return Camera().copy(this, recursive)
    }

    override fun copy(source: Object3, recursive: Boolean): Camera {
        super.copy(source, recursive)

        if (source is Camera) {
            this.matrixWorldInverse.set(source.matrixWorldInverse)
            this.projectionMatrix.set(source.projectionMatrix)
            this.projectionMatrixInverse.set(source.projectionMatrixInverse)
        }

        return this
    }

    /** Returns a Vector3 representing the world space direction in which the camera is looking. (Note: A camera looks down its local, negative z-axis). */
    fun getWorldDirection(): MutableVector3 {
        this.updateWorldMatrix(true, false);
        val e = this.matrixWorld.elements;
        return MutableVector3(-e[8], -e[9], -e[10]).normalize();
    }

    override fun updateMatrixWorld(force: Boolean) {
        super.updateMatrixWorld(force);
        this.matrixWorldInverse.set(this.matrixWorld).invert();
    }

    override fun updateWorldMatrix(updateParents: Boolean, updateChildren: Boolean): Unit {
        super.updateWorldMatrix(updateParents, updateChildren);
        this.matrixWorldInverse.set(this.matrixWorld).invert();
    }

    /** Updates the camera projection matrix. Must be called after any change of parameters. */
    open fun updateProjectionMatrix(): Unit {
    }

    fun project(v: MutableVector3) {
        v.apply(this.matrixWorldInverse).apply(this.projectionMatrix)
    }

    fun unproject(v: MutableVector3) {
        v.apply(this.projectionMatrixInverse).apply(this.matrixWorld)
    }
}
