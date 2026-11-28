package com.aitorgf.threekt.core

import kotlin.js.JsExport

/**
 * How often a [BufferAttribute] changes and who reads it, as the Three.js `StaticDrawUsage` to
 * `StreamCopyUsage` constants. Renderers translate it into their own usage hints.
 */
@JsExport
enum class Usage {
    /** Set once and drawn many times. */
    StaticDraw,
    /** Changed often and drawn many times. */
    DynamicDraw,
    /** Set once and drawn a few times. */
    StreamDraw,
    StaticRead,
    DynamicRead,
    StreamRead,
    StaticCopy,
    DynamicCopy,
    StreamCopy,
}
