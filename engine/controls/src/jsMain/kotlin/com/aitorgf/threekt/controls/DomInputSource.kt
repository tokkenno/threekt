package com.aitorgf.threekt.controls

import kotlin.js.JsExport

/**
 * Adapts `keydown` events of a DOM [target], such as `window`, to an [InputSource]. A handled key
 * prevents the browser's default action, such as scrolling with the arrow keys. The DOM listener is
 * attached while the source has at least one [InputListener].
 */
@JsExport
class DomInputSource(private val target: dynamic) : InputSource {
    private val listeners = InputListenerList()

    override fun addInputListener(listener: InputListener) {
        val wasEmpty = listeners.isEmpty
        if (listeners.add(listener) && wasEmpty) target.addEventListener("keydown", onKeyDown)
    }

    override fun removeInputListener(listener: InputListener) {
        if (listeners.remove(listener) && listeners.isEmpty) target.removeEventListener("keydown", onKeyDown)
    }

    private val onKeyDown: (dynamic) -> Unit = { event ->
        val input = KeyInput(
            code = event.code.unsafeCast<String>(),
            ctrlKey = event.ctrlKey == true,
            metaKey = event.metaKey == true,
            shiftKey = event.shiftKey == true,
        )
        if (listeners.anyHandled { it.onKeyDown(input) }) event.preventDefault()
    }
}
