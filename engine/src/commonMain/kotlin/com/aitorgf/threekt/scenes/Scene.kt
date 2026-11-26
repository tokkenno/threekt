package com.aitorgf.threekt.scenes

import com.aitorgf.threekt.core.Object3D
import com.aitorgf.threekt.math.Color
import kotlin.js.JsExport

/** A root node for the objects rendered together by a camera. */
@JsExport
class Scene : Object3D() {
    /** Color that fills the view before objects are drawn, or null to use the renderer's clear color. */
    var background: Color? = null

    /** Fog applied to the materials that support it, or null for no fog. */
    var fog: SceneFog? = null

    /** Copies the background color and a clone of the fog of [source] when it is a scene. Returns this scene. */
    override fun copy(source: Object3D, recursive: Boolean): Scene {
        super.copy(source, recursive)
        if (source is Scene) {
            background = source.background?.let { Color(it.r, it.g, it.b) }
            fog = source.fog?.clone()
            matrixAutoUpdate = source.matrixAutoUpdate
        }
        return this
    }

    override fun clone(recursive: Boolean): Scene = Scene().copy(this, recursive)
}
