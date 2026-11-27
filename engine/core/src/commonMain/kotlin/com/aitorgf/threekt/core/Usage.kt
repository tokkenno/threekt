package com.aitorgf.threekt.core

import kotlin.js.JsExport

/**
 * How often a [BufferAttribute] changes and who reads it, as the Three.js `StaticDrawUsage` to
 * `StreamCopyUsage` constants. [glValue] is the matching WebGL and OpenGL ES usage.
 */
@JsExport
enum class Usage(val glValue: Int) {
    /** Set once and drawn many times. */
    StaticDraw(0x88E4),
    /** Changed often and drawn many times. */
    DynamicDraw(0x88E8),
    /** Set once and drawn a few times. */
    StreamDraw(0x88E0),
    StaticRead(0x88E5),
    DynamicRead(0x88E9),
    StreamRead(0x88E1),
    StaticCopy(0x88E6),
    DynamicCopy(0x88EA),
    StreamCopy(0x88E2),
}
