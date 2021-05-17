package com.aitorgf.threekt.renderers.webgl

@ExperimentalJsExport
@JsExport
class WebGLRenderState(
    private val extensions: WebGLExtensions,
    private val capabilities: WebGLStateCapabilities
) {
    val lights = WebGLLights( extensions, capabilities );

    val lightsArray: MutableSet<WebGLLights> = HashSet();
    val shadowsArray: MutableSet<WebGLLights> = HashSet();

    fun init() {
        lightsArray.clear()
        shadowsArray.clear()
    }

    fun pushLight( light ) {
        lightsArray.push( light );
    }

    fun pushShadow( shadowLight ) {
        shadowsArray.push( shadowLight );
    }

    fun setupLights() {
        lights.setup( lightsArray );
    }

    fun setupLightsView( camera ) {
        lights.setupView( lightsArray, camera );
    }
}
