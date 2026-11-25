package com.aitorgf.threekt.controls

import kotlin.js.JsExport

/** Delivers platform input to registered [InputListener]s. */
@JsExport
interface InputSource {
    /** Starts delivering input to [listener]. Adding the same listener twice has no effect. */
    fun addInputListener(listener: InputListener)
    /** Stops delivering input to [listener]. */
    fun removeInputListener(listener: InputListener)
}
