package com.aitorgf.threekt.renderers.webgl

import com.aitorgf.threekt.core.Light
import com.aitorgf.threekt.core.LightType
import com.aitorgf.threekt.types.UUID

@ExperimentalJsExport
@JsExport
class UniformsCache {
    private val lights: MutableMap<UUID, Light> = HashMap()

    fun get(light: Light): Light {
        val oldLight = lights[light.id]
        if (oldLight != null) {
            return oldLight
        }

        val uniforms = when (light.type) {
            LightType.DirectionalLight -> {
                direction: new Vector3(),
                color: new Color()
            }
            LightType.SpotLight -> {
                position: new Vector3(),
                direction: new Vector3(),
                color: new Color(),
                distance: 0,
                coneCos: 0,
                penumbraCos: 0,
                decay: 0
            }
            LightType.PointLight -> {
                position: new Vector3(),
                color: new Color(),
                distance: 0,
                decay: 0
            }
            LightType.HemisphereLight -> {
                direction: new Vector3(),
                skyColor: new Color(),
                groundColor: new Color()
            }
            LightType.RectAreaLight -> {
                color: new Color(),
                position: new Vector3(),
                halfWidth: new Vector3(),
                halfHeight: new Vector3()
            }
            else -> throw WebGLException("Unknown light")
        }

        this.lights[light.id] = uniforms
        return uniforms

    }
}
