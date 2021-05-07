package com.aitorgf.threekt.lights

import com.aitorgf.threekt.core.Camera
import com.aitorgf.threekt.core.Light
import com.aitorgf.threekt.math.*

@ExperimentalJsExport
@JsExport
open class LightShadow(
    var camera: Camera
) {
    private var bias: Int = 0
    private var normalBias: Int = 0
    private var radius: Int = 1

    private val mapSize: MutableVector2 = MutableVector2(512.0, 512.0)

    private var map: Any? = null
    private var mapPass: Any? = null
    private var matrix: MutableMatrix4 = MutableMatrix4()

    private var autoUpdate: Boolean = true
    private var needsUpdate: Boolean = false

    var frustum: Frustum = Frustum()
        private set

    val frameExtents: MutableVector2 = MutableVector2(1.0, 1.0)

    var viewportCount: Int = 1
        private set

    private val viewports: List<Vector4> = mutableListOf(
        Vector4(0.0, 0.0, 1.0, 1.0)
    )

    private val projScreenMatrix: MutableMatrix4 = MutableMatrix4()
    private val lightPositionWorld: MutableVector3 = MutableVector3()
    private val lookTarget: MutableVector3 = MutableVector3()

    fun updateMatrices(light: Light) {
        val shadowCamera = this.camera
        val shadowMatrix = this.matrix

        lightPositionWorld.setFromMatrixPosition(light.matrixWorld)
        shadowCamera.position.set(lightPositionWorld)

        lookTarget.setFromMatrixPosition(light.target.matrixWorld)
        shadowCamera.lookAt(lookTarget)
        shadowCamera.updateMatrixWorld()

        projScreenMatrix.multiplyAndSet(shadowCamera.projectionMatrix, shadowCamera.matrixWorldInverse)
        frustum.setFromProjectionMatrix(projScreenMatrix)

        shadowMatrix.set(
            0.5, 0.0, 0.0, 0.5,
            0.0, 0.5, 0.0, 0.5,
            0.0, 0.0, 0.5, 0.5,
            0.0, 0.0, 0.0, 1.0
        )

        shadowMatrix.multiply(shadowCamera.projectionMatrix)
        shadowMatrix.multiply(shadowCamera.matrixWorldInverse)
    }

    fun getViewport(viewportIndex: Int): Vector4 {
        return this.viewports[viewportIndex]
    }

    fun dispose() {
        if (this.map) {
            this.map.dispose()
        }

        if (this.mapPass) {
            this.mapPass.dispose()
        }
    }

    fun copy(source: LightShadow): LightShadow {
        this.camera = source.camera.clone()

        this.bias = source.bias
        this.radius = source.radius

        this.mapSize.copy(source.mapSize)

        return this
    }

    fun clone() {
        return LightShadow().copy(this)
    }
}
