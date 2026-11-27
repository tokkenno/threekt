package com.aitorgf.threekt.core

import kotlin.js.JsExport

/** Registers listeners by event type and notifies them when events are dispatched. */
@JsExport
open class EventDispatcher {
    private val listeners = mutableMapOf<String, MutableList<(DispatchedEvent) -> Unit>>()

    /** Adds [listener] for events of [type]. Adding the same listener twice has no effect. */
    fun addEventListener(type: String, listener: (DispatchedEvent) -> Unit) {
        val typeListeners = listeners.getOrPut(type) { mutableListOf() }
        if (typeListeners.none { it === listener }) typeListeners.add(listener)
    }

    /** Returns whether [listener] is registered for events of [type]. */
    fun hasEventListener(type: String, listener: (DispatchedEvent) -> Unit): Boolean =
        listeners[type]?.any { it === listener } == true

    /** Removes [listener] for events of [type] if it is registered. */
    fun removeEventListener(type: String, listener: (DispatchedEvent) -> Unit) {
        listeners[type]?.removeAll { it === listener }
    }

    /**
     * Notifies every listener for the type of [event] in registration order. [DispatchedEvent.target] is
     * this dispatcher during the dispatch and null afterwards. Listeners added or removed while the event
     * is being dispatched take effect on the next dispatch.
     */
    fun dispatchEvent(event: DispatchedEvent) {
        val typeListeners = listeners[event.type]?.toList() ?: return
        event.target = this
        for (listener in typeListeners) listener(event)
        event.target = null
    }

    /** Dispatches a [DispatchedEvent] of [type] without extra data. */
    @JsExport.Ignore
    fun dispatchEvent(type: String) = dispatchEvent(DispatchedEvent(type))
}
