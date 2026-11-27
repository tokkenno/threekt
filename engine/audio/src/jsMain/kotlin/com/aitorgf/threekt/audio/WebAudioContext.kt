package com.aitorgf.threekt.audio

import com.aitorgf.threekt.audio.webaudio.AudioBuffer
import com.aitorgf.threekt.audio.webaudio.AudioNode
import com.aitorgf.threekt.audio.webaudio.BaseAudioContext
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.Uint8Array
import org.w3c.dom.HTMLMediaElement
import org.w3c.dom.mediacapture.MediaStream
import kotlin.js.Promise

/**
 * The browser implementation of [AudioGraphContext], backed by a Web Audio context.
 *
 * [AudioContext.getContext] creates one for a new browser `AudioContext` on first use. Browsers keep a context
 * suspended until a user gesture, so create it in an input handler, or call `resume()` on [context] from one. Use
 * [wrapNode] to connect your own Web Audio nodes, such as oscillators, to the Threekt audio objects.
 */
@JsExport
class WebAudioContext(
    /** The browser context, for creating nodes that Threekt does not wrap. */
    val context: BaseAudioContext = js("new (globalThis.AudioContext || globalThis.webkitAudioContext)()").unsafeCast<BaseAudioContext>(),
) : AudioGraphContext {
    private val native: dynamic get() = context.asDynamic()

    override val currentTime: Double get() = native.currentTime.unsafeCast<Double>()
    override val destination: AudioGraphNode = WebAudioNode(native.destination)
    override val listener: AudioGraphListener = WebAudioListener(native.listener)

    override fun createGain(): AudioGraphGain = WebAudioGain(native.createGain())
    override fun createPanner(): AudioGraphPanner = WebAudioPanner(native.createPanner())
    override fun createBufferSource(): AudioGraphBufferSource = WebAudioBufferSource(native.createBufferSource())
    override fun createAnalyser(): AudioGraphAnalyser = WebAudioAnalyser(native.createAnalyser())

    /** Wraps a browser node so that it can be passed to `Audio.setNodeSource` or used as a filter. */
    fun wrapNode(node: AudioNode): AudioGraphNode = WebAudioNode(node)

    /** Returns the browser node behind a node created by this context or by [wrapNode]. */
    fun unwrapNode(node: AudioGraphNode): AudioNode = (node as WebAudioNode).node.unsafeCast<AudioNode>()

    /** Wraps a decoded browser buffer so that it can be passed to `Audio.setBuffer`. */
    fun wrapBuffer(buffer: AudioBuffer): AudioGraphBuffer = WebAudioBuffer(buffer)

    /** Creates a source node for a `<audio>` or `<video>` element, replacing upstream `Audio.setMediaElementSource`. */
    fun createMediaElementSource(element: HTMLMediaElement): AudioGraphNode =
        WebAudioNode(native.createMediaElementSource(element))

    /** Creates a source node for a media stream, replacing upstream `Audio.setMediaStreamSource`. */
    fun createMediaStreamSource(stream: MediaStream): AudioGraphNode =
        WebAudioNode(native.createMediaStreamSource(stream))

    /** Decodes compressed audio data. The data is detached once decoded, as with the browser method. */
    fun decodeAudioData(data: ArrayBuffer): Promise<AudioGraphBuffer> =
        native.decodeAudioData(data).unsafeCast<Promise<AudioBuffer>>().then { WebAudioBuffer(it) }
}

private open class WebAudioNode(val node: dynamic) : AudioGraphNode {
    override fun connect(destination: AudioGraphNode) {
        node.connect((destination as WebAudioNode).node)
    }

    override fun disconnect(destination: AudioGraphNode) {
        node.disconnect((destination as WebAudioNode).node)
    }
}

private class WebAudioParam(private val param: dynamic) : AudioGraphParam {
    override var value: Double
        get() = param.value.unsafeCast<Double>()
        set(value) { param.value = value }

    override fun setTargetAtTime(target: Double, startTime: Double, timeConstant: Double) {
        param.setTargetAtTime(target, startTime, timeConstant)
    }

    override fun linearRampToValueAtTime(value: Double, endTime: Double) {
        param.linearRampToValueAtTime(value, endTime)
    }

    override fun setValueAtTime(value: Double, startTime: Double) {
        param.setValueAtTime(value, startTime)
    }
}

private fun param(value: dynamic): AudioGraphParam? = if (value == null || value == undefined) null else WebAudioParam(value)

private class WebAudioGain(node: dynamic) : WebAudioNode(node), AudioGraphGain {
    override val gain: AudioGraphParam = WebAudioParam(node.gain)
}

private class WebAudioBuffer(val buffer: dynamic) : AudioGraphBuffer {
    override val duration: Double get() = buffer.duration.unsafeCast<Double>()
}

private class WebAudioBufferSource(node: dynamic) : WebAudioNode(node), AudioGraphBufferSource {
    override var buffer: AudioGraphBuffer? = null
        set(value) {
            field = value
            node.buffer = (value as WebAudioBuffer?)?.buffer
        }
    override var loop: Boolean
        get() = node.loop.unsafeCast<Boolean>()
        set(value) { node.loop = value }
    override var loopStart: Double
        get() = node.loopStart.unsafeCast<Double>()
        set(value) { node.loopStart = value }
    override var loopEnd: Double
        get() = node.loopEnd.unsafeCast<Double>()
        set(value) { node.loopEnd = value }
    override val detune: AudioGraphParam? = param(node.detune)
    override val playbackRate: AudioGraphParam = WebAudioParam(node.playbackRate)
    override var onEnded: (() -> Unit)? = null
        set(value) {
            field = value
            node.onended = if (value == null) null else { _: dynamic -> value() }
        }

    override fun start(time: Double, offset: Double, duration: Double?) {
        // An explicit undefined duration is invalid in some browsers, so leave the argument out instead.
        if (duration == null) node.start(time, offset) else node.start(time, offset, duration)
    }

    override fun stop(time: Double) {
        node.stop(time)
    }
}

private class WebAudioAnalyser(node: dynamic) : WebAudioNode(node), AudioGraphAnalyser {
    override var fftSize: Int
        get() = node.fftSize.unsafeCast<Int>()
        set(value) { node.fftSize = value }
    override val frequencyBinCount: Int get() = node.frequencyBinCount.unsafeCast<Int>()

    override fun getByteFrequencyData(array: ByteArray) {
        val bytes = array.unsafeCast<Int8Array>()
        node.getByteFrequencyData(Uint8Array(bytes.buffer, bytes.byteOffset, bytes.length))
    }
}

private class WebAudioPanner(node: dynamic) : WebAudioNode(node), AudioGraphPanner {
    override var panningModel: PanningModel
        get() = if (node.panningModel == "HRTF") PanningModel.HRTF else PanningModel.EqualPower
        set(value) { node.panningModel = if (value == PanningModel.HRTF) "HRTF" else "equalpower" }
    override var distanceModel: DistanceModel
        get() = DistanceModel.entries.first { it.name.lowercase() == node.distanceModel }
        set(value) { node.distanceModel = value.name.lowercase() }
    override var refDistance: Double
        get() = node.refDistance.unsafeCast<Double>()
        set(value) { node.refDistance = value }
    override var maxDistance: Double
        get() = node.maxDistance.unsafeCast<Double>()
        set(value) { node.maxDistance = value }
    override var rolloffFactor: Double
        get() = node.rolloffFactor.unsafeCast<Double>()
        set(value) { node.rolloffFactor = value }
    override var coneInnerAngle: Double
        get() = node.coneInnerAngle.unsafeCast<Double>()
        set(value) { node.coneInnerAngle = value }
    override var coneOuterAngle: Double
        get() = node.coneOuterAngle.unsafeCast<Double>()
        set(value) { node.coneOuterAngle = value }
    override var coneOuterGain: Double
        get() = node.coneOuterGain.unsafeCast<Double>()
        set(value) { node.coneOuterGain = value }
    override val positionX = param(node.positionX)
    override val positionY = param(node.positionY)
    override val positionZ = param(node.positionZ)
    override val orientationX = param(node.orientationX)
    override val orientationY = param(node.orientationY)
    override val orientationZ = param(node.orientationZ)

    override fun setPosition(x: Double, y: Double, z: Double) {
        node.setPosition(x, y, z)
    }

    override fun setOrientation(x: Double, y: Double, z: Double) {
        node.setOrientation(x, y, z)
    }
}

private class WebAudioListener(private val listener: dynamic) : AudioGraphListener {
    // Firefox exposes the listener pose only through setPosition and setOrientation.
    private val automatable = listener.positionX != null && listener.positionX != undefined &&
        listener.forwardX != null && listener.forwardX != undefined
    override val positionX = if (automatable) param(listener.positionX) else null
    override val positionY = if (automatable) param(listener.positionY) else null
    override val positionZ = if (automatable) param(listener.positionZ) else null
    override val forwardX = if (automatable) param(listener.forwardX) else null
    override val forwardY = if (automatable) param(listener.forwardY) else null
    override val forwardZ = if (automatable) param(listener.forwardZ) else null
    override val upX = if (automatable) param(listener.upX) else null
    override val upY = if (automatable) param(listener.upY) else null
    override val upZ = if (automatable) param(listener.upZ) else null

    override fun setPosition(x: Double, y: Double, z: Double) {
        listener.setPosition(x, y, z)
    }

    override fun setOrientation(x: Double, y: Double, z: Double, upX: Double, upY: Double, upZ: Double) {
        listener.setOrientation(x, y, z, upX, upY, upZ)
    }
}
