package com.aitorgf.threekt.core

import kotlin.js.JsExport

/** Event delivered by an [EventDispatcher]. [target] is the dispatcher that sent it. */
@JsExport
class DispatchedEvent(val type: String, val target: EventDispatcher)
