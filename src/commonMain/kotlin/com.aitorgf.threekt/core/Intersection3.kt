package com.aitorgf.threekt.core

import com.aitorgf.threekt.math.Vector2
import com.aitorgf.threekt.math.Vector3

@ExperimentalJsExport
@JsExport
class Intersection3(
    val distance: Double,
    val point: Vector3,
    val face: Any, // TODO: Define type
    val faceIndex: Int,
    val obj: Object3,
    val uv: Vector2,
    val uv2: Vector2,
    val instanceId: Int // TODO: What is this?
) {
}
