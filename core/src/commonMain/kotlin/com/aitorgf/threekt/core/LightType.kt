package com.aitorgf.threekt.core

@ExperimentalJsExport
@JsExport
class LightType(val name: String) {
    companion object {
        val Light = LightType("Light")
        val DirectionalLight = LightType("DirectionalLight")
        val HemisphereLight = LightType("HemisphereLight")
        val PointLight = LightType("PointLight")
        val RectAreaLight = LightType("RectAreaLight")
        val SpotLight = LightType("SpotLight")
    }
}
