package com.aitorgf.threekt.core

import kotlin.js.JsExport

/**
 * Adapts a DOM [document] to [PageVisibility] through `document.hidden` and its `visibilitychange`
 * event, so that `timer.connect(new DocumentPageVisibility(document))` matches the Three.js
 * `timer.connect(document)`.
 */
@JsExport
class DocumentPageVisibility(private val document: dynamic) : PageVisibility {
    private val handlers = mutableListOf<Pair<() -> Unit, (dynamic) -> Unit>>()

    override val hidden: Boolean get() = document.hidden == true

    override fun addVisibilityChangeListener(listener: () -> Unit) {
        if (handlers.any { it.first === listener }) return
        val handler: (dynamic) -> Unit = { listener() }
        handlers.add(listener to handler)
        document.addEventListener("visibilitychange", handler, false)
    }

    override fun removeVisibilityChangeListener(listener: () -> Unit) {
        val index = handlers.indexOfFirst { it.first === listener }
        if (index < 0) return
        document.removeEventListener("visibilitychange", handlers.removeAt(index).second)
    }
}
