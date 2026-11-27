package com.aitorgf.threekt.audio

import kotlin.js.JsExport

/**
 * The position and orientation from which a context hears its panners, the platform-neutral counterpart of the
 * Web Audio `AudioListener`.
 */
@JsExport
interface AudioGraphListener {
    /** Automatable pose, or null on platforms that only offer [setPosition] and [setOrientation]. */
    val positionX: AudioGraphParam?
    val positionY: AudioGraphParam?
    val positionZ: AudioGraphParam?
    val forwardX: AudioGraphParam?
    val forwardY: AudioGraphParam?
    val forwardZ: AudioGraphParam?
    val upX: AudioGraphParam?
    val upY: AudioGraphParam?
    val upZ: AudioGraphParam?

    fun setPosition(x: Double, y: Double, z: Double)
    fun setOrientation(x: Double, y: Double, z: Double, upX: Double, upY: Double, upZ: Double)
}
