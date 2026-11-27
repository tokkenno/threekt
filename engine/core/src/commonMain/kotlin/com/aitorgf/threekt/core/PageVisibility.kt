package com.aitorgf.threekt.core

import kotlin.js.JsExport

/**
 * Reports whether the page or window that hosts the renderer is hidden, as the DOM `document` does
 * through `document.hidden` and its `visibilitychange` event. [Timer.connect] uses it to skip the time
 * that passes while the page is hidden. In the browser, `DocumentPageVisibility` adapts a DOM document.
 */
@JsExport
interface PageVisibility {
    /** Whether the page is currently hidden. */
    val hidden: Boolean

    /** Registers [listener] to be called whenever [hidden] changes. */
    fun addVisibilityChangeListener(listener: () -> Unit)

    /** Unregisters a listener added with [addVisibilityChangeListener]. */
    fun removeVisibilityChangeListener(listener: () -> Unit)
}
