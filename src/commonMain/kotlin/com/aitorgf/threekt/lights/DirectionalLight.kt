package com.aitorgf.threekt.lights

import com.aitorgf.threekt.core.Light
import com.aitorgf.threekt.core.LightType
import com.aitorgf.threekt.core.Object3
import com.aitorgf.threekt.math.Color

@ExperimentalJsExport
@JsExport
class DirectionalLight(
    color: Color,
    intensity: Double
): Light(color, intensity) {
    override val type: LightType = LightType.Light

    var target: Object3 = Object3()
    val shadow: DirectionalLightShadow = DirectionalLightShadow()

    init {
        this.position.set( Object3.DefaultUp );
        this.updateMatrix();
    }

    override fun dispose() {
        this.shadow.dispose()
    }

    override fun copy(source: Object3, recursive: Boolean): DirectionalLight {
        super.copy(source)

        if (source is DirectionalLight) {
            this.target = source.target.clone();
            this.shadow = source.shadow.clone();
        }

        return this
    }
}
