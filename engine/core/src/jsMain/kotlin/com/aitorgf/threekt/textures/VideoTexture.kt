package com.aitorgf.threekt.textures

import kotlin.js.JsExport

/**
 * Texture backed by a browser video element. Like Three.js it uses linear sampling without mipmaps.
 * Call [update] before drawing to schedule an upload when playback has advanced.
 */
@JsExport
class VideoTexture(private val video: dynamic) : Texture() {
    override val image: Any get() = video.unsafeCast<Any>()
    private var lastTime = -1.0

    init {
        generateMipmaps = false
        minFilter = TextureFilter.Linear
    }

    /** Marks a ready frame for upload when its playback time has changed. */
    fun update() {
        if (video.readyState.unsafeCast<Int>() < 2) return
        val time = video.currentTime.unsafeCast<Double>()
        if (time != lastTime) {
            lastTime = time
            markUpdated()
        }
    }
}
