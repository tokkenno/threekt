package com.aitorgf.threekt.renderers

import com.aitorgf.threekt.cameras.Camera
import com.aitorgf.threekt.scenes.Scene
import kotlin.js.JsExport

/**
 * A renderer that draws a [Scene] as seen by a [Camera], independent of the graphics API. Core APIs that
 * receive a renderer, such as `Object3D.onBeforeRender`, take this interface so that the core does not
 * depend on a concrete renderer such as the WebGL one. Three.js passes the concrete renderer instead.
 */
@JsExport
interface Renderer {
    /** Draws [scene] as seen by [camera], updating world matrices first. */
    fun render(scene: Scene, camera: Camera)

    /** Releases the GPU resources held by the renderer. */
    fun dispose()
}
