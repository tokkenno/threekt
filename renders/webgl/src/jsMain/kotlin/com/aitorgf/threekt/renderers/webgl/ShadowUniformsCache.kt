package com.aitorgf.threekt.renderers.webgl

import com.aitorgf.threekt.core.Light
import com.aitorgf.threekt.core.LightType
import com.aitorgf.threekt.types.UUID

@ExperimentalJsExport
@JsExport
class ShadowUniformsCache {
    private val lights: MutableMap<UUID, Light> = HashMap()

    fun get(light: Light): Light {
        val oldLight = lights[light.id]
        if (oldLight != null) {
            return oldLight
        }

        val uniforms = when (light.type) {
            LightType.DirectionalLight -> {
                shadowBias: 0,
                shadowNormalBias: 0,
                shadowRadius: 1,
                shadowMapSize: new Vector2()
            }
            LightType.SpotLight -> {
                shadowBias: 0,
                shadowNormalBias: 0,
                shadowRadius: 1,
                shadowMapSize: new Vector2()
            }
            LightType.PointLight -> {
                shadowBias: 0,
                shadowNormalBias: 0,
                shadowRadius: 1,
                shadowMapSize: new Vector2(),
                shadowCameraNear: 1,
                shadowCameraFar: 1000
            }
            else -> throw WebGLException("Unknown shadow light")
        }

        this.lights[light.id] = uniforms
        return uniforms
    }
}
