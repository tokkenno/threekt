package com.aitorgf.threekt.audio

import com.aitorgf.threekt.core.Object3D
import com.aitorgf.threekt.math.Quaternion
import com.aitorgf.threekt.math.Vector3
import kotlin.js.JsExport

// Scratch objects for the world pose. They must not be named like Object3D members such as `position` or `up`,
// which would shadow them inside the class.
private val worldPosition = Vector3()
private val worldQuaternion = Quaternion()
private val worldScale = Vector3()
private val worldOrientation = Vector3()

/**
 * An audio source placed in the scene: it is heard from its world position, and its local +Z axis points the
 * directional cone. The panner follows the object's world pose whenever the world matrix is updated while the
 * audio plays (or always, for sources without playback control).
 */
@JsExport
open class PositionalAudio(listener: AudioListener) : Audio(listener) {
    val panner: AudioGraphPanner = context.createPanner()

    init {
        panner.panningModel = PanningModel.HRTF
        panner.connect(gain)
    }

    override fun connect(): PositionalAudio {
        super.connect()
        panner.connect(gain)
        return this
    }

    override fun disconnect(): PositionalAudio {
        super.disconnect()
        panner.disconnect(gain)
        return this
    }

    override fun getOutput(): AudioGraphNode = panner

    fun getRefDistance(): Double = panner.refDistance

    fun setRefDistance(value: Double): PositionalAudio {
        panner.refDistance = value
        return this
    }

    fun getRolloffFactor(): Double = panner.rolloffFactor

    fun setRolloffFactor(value: Double): PositionalAudio {
        panner.rolloffFactor = value
        return this
    }

    fun getDistanceModel(): DistanceModel = panner.distanceModel

    fun setDistanceModel(value: DistanceModel): PositionalAudio {
        panner.distanceModel = value
        return this
    }

    fun getMaxDistance(): Double = panner.maxDistance

    fun setMaxDistance(value: Double): PositionalAudio {
        panner.maxDistance = value
        return this
    }

    /** Sets the cone angles in degrees and the gain outside the outer cone. */
    fun setDirectionalCone(coneInnerAngle: Double, coneOuterAngle: Double, coneOuterGain: Double): PositionalAudio {
        panner.coneInnerAngle = coneInnerAngle
        panner.coneOuterAngle = coneOuterAngle
        panner.coneOuterGain = coneOuterGain
        return this
    }

    override fun updateMatrixWorld(force: Boolean) {
        super.updateMatrixWorld(force)
        if (hasPlaybackControl && !isPlaying) return
        matrixWorld.decompose(worldPosition, worldQuaternion, worldScale)
        worldOrientation.set(0.0, 0.0, 1.0).applyQuaternion(worldQuaternion)
        val positionX = panner.positionX
        if (positionX != null) {
            // Code path for browsers with automatable panner parameters (see three.js #14393).
            val endTime = context.currentTime + listener.timeDelta
            positionX.linearRampToValueAtTime(worldPosition.x, endTime)
            panner.positionY!!.linearRampToValueAtTime(worldPosition.y, endTime)
            panner.positionZ!!.linearRampToValueAtTime(worldPosition.z, endTime)
            panner.orientationX!!.linearRampToValueAtTime(worldOrientation.x, endTime)
            panner.orientationY!!.linearRampToValueAtTime(worldOrientation.y, endTime)
            panner.orientationZ!!.linearRampToValueAtTime(worldOrientation.z, endTime)
        } else {
            panner.setPosition(worldPosition.x, worldPosition.y, worldPosition.z)
            panner.setOrientation(worldOrientation.x, worldOrientation.y, worldOrientation.z)
        }
    }

    override fun copy(source: Object3D, recursive: Boolean): PositionalAudio {
        super.copy(source, recursive)
        return this
    }

    override fun clone(recursive: Boolean): PositionalAudio = PositionalAudio(listener).copy(this, recursive)
}
