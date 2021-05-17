package com.aitorgf.threekt.core

@ExperimentalJsExport
@JsExport
external interface Renderer {
    var pixelRatio: Double
    val width: Int
    val height: Int

    fun init(): Unit
    fun dispose(): Unit

    fun render(scene: Scene, camera: Camera): Unit

    fun resize(width: Int, height: Int): Unit
}
