package com.aitorgf.threekt.core

import kotlin.js.JsExport

/**
 * Event delivered by an [EventDispatcher]. Three.js dispatches plain objects with a `type` and extra
 * fields; Threekt uses this class, and subclasses such as [ChildEvent] for events that carry data.
 */
@JsExport
open class DispatchedEvent(val type: String) {
    /** The dispatcher delivering the event. Set while the event is dispatched and null otherwise. */
    var target: EventDispatcher? = null
        internal set
}
