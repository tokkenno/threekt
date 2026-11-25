package com.aitorgf.threekt.core

import com.aitorgf.threekt.math.Vector2
import com.aitorgf.threekt.math.Vector3
import kotlin.js.JsExport

/**
 * Ray hit with a world-space [point] and optional local-space face normal and UV. [instanceId] is the
 * index of the hit instance of an `InstancedMesh`, or null for other objects.
 */
@JsExport
class Intersection(
    val distance: Double,
    val point: Vector3,
    val object3d: Object3D,
    val index: Int,
    val a: Int = -1,
    val b: Int = -1,
    val c: Int = -1,
    val normal: Vector3? = null,
    val uv: Vector2? = null,
    val instanceId: Int? = null,
)
