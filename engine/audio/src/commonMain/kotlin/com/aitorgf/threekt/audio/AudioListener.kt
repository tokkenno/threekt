package com.aitorgf.threekt.audio

import com.aitorgf.threekt.core.Object3D
import com.aitorgf.threekt.core.Timer
import com.aitorgf.threekt.math.Quaternion
import com.aitorgf.threekt.math.Vector3
import kotlin.js.JsExport
import kotlin.jvm.JvmName

// Scratch objects for the world pose. They must not be named like Object3D members such as `position` or `up`,
// which would shadow them inside the class.
private val worldPosition = Vector3()
private val worldQuaternion = Quaternion()
private val worldScale = Vector3()
private val worldForward = Vector3()
private val worldUp = Vector3()

/**
 * The virtual listener of all positional and non-positional audio effects in the scene. A scene usually has one
 * listener, added to the camera so that the sound follows the user's point of view.
 *
 * Every frame in which its world matrix is updated, the listener moves the context's listener to its world pose,
 * with a short ramp where the platform supports it.
 */
@JsExport
open class AudioListener : Object3D() {
    /** The shared context from [AudioContext.getContext]. */
    val context: AudioGraphContext = AudioContext.getContext()

    /** The master gain, connected to the context's destination or to the [filter]. */
    val gain: AudioGraphGain = context.createGain()

    /** An optional filter between [gain] and the destination. On the JVM its getter is `filter()`, as [getFilter] takes the `get` name. */
    @get:JvmName("filter")
    var filter: AudioGraphNode? = null
        private set

    /** Seconds between the last two matrix updates, used to ramp listener and panner poses. */
    var timeDelta = 0.0
        private set

    private val timer = Timer()

    init {
        gain.connect(context.destination)
    }

    /** Returns the node that audio sources connect to. */
    fun getInput(): AudioGraphNode = gain

    /** Removes the [filter], connecting the gain straight to the destination again. */
    fun removeFilter(): AudioListener {
        val current = filter ?: return this
        gain.disconnect(current)
        current.disconnect(context.destination)
        gain.connect(context.destination)
        filter = null
        return this
    }

    fun getFilter(): AudioGraphNode? = filter

    /** Inserts [value] between the master gain and the destination, replacing the current filter. */
    fun setFilter(value: AudioGraphNode): AudioListener {
        val current = filter
        if (current != null) {
            gain.disconnect(current)
            current.disconnect(context.destination)
        } else {
            gain.disconnect(context.destination)
        }
        filter = value
        gain.connect(value)
        value.connect(context.destination)
        return this
    }

    fun getMasterVolume(): Double = gain.gain.value

    /** Fades the master volume to [value] with a time constant of 10 ms. */
    fun setMasterVolume(value: Double): AudioListener {
        gain.gain.setTargetAtTime(value, context.currentTime, 0.01)
        return this
    }

    override fun updateMatrixWorld(force: Boolean) {
        super.updateMatrixWorld(force)
        timer.update()
        val listener = context.listener
        timeDelta = timer.getDelta()
        matrixWorld.decompose(worldPosition, worldQuaternion, worldScale)
        // The initial forward and up directions must be orthogonal.
        worldForward.set(0.0, 0.0, -1.0).applyQuaternion(worldQuaternion)
        worldUp.set(0.0, 1.0, 0.0).applyQuaternion(worldQuaternion)
        val positionX = listener.positionX
        if (positionX != null) {
            // Code path for browsers with automatable listener parameters (see three.js #14393).
            val endTime = context.currentTime + timeDelta
            positionX.linearRampToValueAtTime(worldPosition.x, endTime)
            listener.positionY!!.linearRampToValueAtTime(worldPosition.y, endTime)
            listener.positionZ!!.linearRampToValueAtTime(worldPosition.z, endTime)
            listener.forwardX!!.linearRampToValueAtTime(worldForward.x, endTime)
            listener.forwardY!!.linearRampToValueAtTime(worldForward.y, endTime)
            listener.forwardZ!!.linearRampToValueAtTime(worldForward.z, endTime)
            listener.upX!!.linearRampToValueAtTime(worldUp.x, endTime)
            listener.upY!!.linearRampToValueAtTime(worldUp.y, endTime)
            listener.upZ!!.linearRampToValueAtTime(worldUp.z, endTime)
        } else {
            listener.setPosition(worldPosition.x, worldPosition.y, worldPosition.z)
            listener.setOrientation(worldForward.x, worldForward.y, worldForward.z, worldUp.x, worldUp.y, worldUp.z)
        }
    }
}
