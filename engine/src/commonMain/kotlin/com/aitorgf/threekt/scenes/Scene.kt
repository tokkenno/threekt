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
}
