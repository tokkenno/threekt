package com.aitorgf.threekt.core

import kotlin.js.JsExport

/**
 * The `childadded` and `childremoved` events that an [Object3D] dispatches when [child] is added to or
 * removed from it.
 */
@JsExport
class ChildEvent(type: String, val child: Object3D) : DispatchedEvent(type)
