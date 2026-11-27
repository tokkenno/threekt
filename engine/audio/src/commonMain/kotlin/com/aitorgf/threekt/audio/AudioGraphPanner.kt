package com.aitorgf.threekt.audio

import kotlin.js.JsExport

/**
 * A node that positions its input in 3D space relative to the listener, the platform-neutral counterpart of the
 * Web Audio `PannerNode`. Angles are in degrees.
 */
@JsExport
interface AudioGraphPanner : AudioGraphNode {
    var panningModel: PanningModel
    var distanceModel: DistanceModel
    var refDistance: Double
    var maxDistance: Double
    var rolloffFactor: Double
    var coneInnerAngle: Double
    var coneOuterAngle: Double
    var coneOuterGain: Double

    /** Automatable position and orientation, or null on platforms that only offer [setPosition] and [setOrientation]. */
    val positionX: AudioGraphParam?
    val positionY: AudioGraphParam?
    val positionZ: AudioGraphParam?
    val orientationX: AudioGraphParam?
    val orientationY: AudioGraphParam?
    val orientationZ: AudioGraphParam?

    fun setPosition(x: Double, y: Double, z: Double)
    fun setOrientation(x: Double, y: Double, z: Double)
}
