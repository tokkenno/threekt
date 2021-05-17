package com.aitorgf.threekt.renderers.webgl

import com.aitorgf.threekt.core.Scene

@ExperimentalJsExport
@JsExport
class WebGLRenderStates(
    private val extensions: WebGLExtensions,
    private val capabilities: WebGLStateCapabilities
) {
    private val renderStates: MutableMap<Scene, MutableList<WebGLRenderState>> = HashMap()

    fun get(scene: Scene, renderCallDepth: Int = 0) {
        var sceneStates = this.renderStates[scene]

        if ( sceneStates == null ) {
            val renderState = WebGLRenderState( extensions, capabilities )
            renderStates.set( scene, mutableListOf(renderState))
        } else {
            if ( renderCallDepth >= sceneStates.size ) {
                val renderState = WebGLRenderState( extensions, capabilities )
                sceneStates.add( renderState )
            } else {
                renderState = renderStates.get( scene )[ renderCallDepth ]
            }
        }

        return renderState
    }
}
