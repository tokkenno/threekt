package com.aitorgf.threekt.controls

/** Listener registry shared by [InputSource] implementations. */
internal class InputListenerList {
    private val listeners = mutableListOf<InputListener>()

    /** Whether no listener is registered. */
    val isEmpty: Boolean get() = listeners.isEmpty()

    /** Adds [listener] unless it is already registered. Returns whether it was added. */
    fun add(listener: InputListener): Boolean {
        if (listeners.any { it === listener }) return false
        listeners.add(listener)
        return true
    }

    /** Removes [listener]. Returns whether it was registered. */
    fun remove(listener: InputListener): Boolean = listeners.removeAll { it === listener }

    /** Notifies a snapshot of the listeners, so that listeners can unregister during delivery. */
    fun forEach(action: (InputListener) -> Unit) {
        for (listener in listeners.toList()) action(listener)
    }

    /** Notifies every listener and returns whether any of them handled the event. */
    fun anyHandled(action: (InputListener) -> Boolean): Boolean {
        var handled = false
        forEach { if (action(it)) handled = true }
        return handled
    }
}
