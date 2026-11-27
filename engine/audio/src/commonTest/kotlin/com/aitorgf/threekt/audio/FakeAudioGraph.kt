package com.aitorgf.threekt.audio

/** An in-memory audio graph that records connections, parameter automation and playback calls. */
class FakeAudioGraph : AudioGraphContext {
    override var currentTime = 0.0
    val connections = mutableSetOf<Pair<AudioGraphNode, AudioGraphNode>>()
    val sources = mutableListOf<Source>()
    override val destination: AudioGraphNode = Node("destination")
    override val listener = Listener()

    override fun createGain(): AudioGraphGain = Gain()
    override fun createPanner(): AudioGraphPanner = Panner()
    override fun createBufferSource(): AudioGraphBufferSource = Source().also { sources += it }
    override fun createAnalyser(): AudioGraphAnalyser = Analyser()

    fun isConnected(from: AudioGraphNode, to: AudioGraphNode) = (from to to) in connections

    open inner class Node(val label: String) : AudioGraphNode {
        override fun connect(destination: AudioGraphNode) {
            connections += this to destination
        }

        override fun disconnect(destination: AudioGraphNode) {
            check(connections.remove(this to destination)) { "$label was not connected to $destination." }
        }

        override fun toString() = label
    }

    class Param(override var value: Double = 0.0) : AudioGraphParam {
        /** The last ramp as (value, endTime). */
        var ramp: Pair<Double, Double>? = null

        override fun setTargetAtTime(target: Double, startTime: Double, timeConstant: Double) {
            value = target
        }

        override fun linearRampToValueAtTime(value: Double, endTime: Double) {
            ramp = value to endTime
        }

        override fun setValueAtTime(value: Double, startTime: Double) {
            this.value = value
        }
    }

    inner class Gain : Node("gain"), AudioGraphGain {
        override val gain = Param(1.0)
    }

    class Buffer(override val duration: Double) : AudioGraphBuffer

    inner class Source : Node("source"), AudioGraphBufferSource {
        override var buffer: AudioGraphBuffer? = null
        override var loop = false
        override var loopStart = 0.0
        override var loopEnd = 0.0
        override val detune = Param()
        override val playbackRate = Param(1.0)
        override var onEnded: (() -> Unit)? = null
        var started: Triple<Double, Double, Double?>? = null
        var stoppedAt: Double? = null

        override fun start(time: Double, offset: Double, duration: Double?) {
            started = Triple(time, offset, duration)
        }

        override fun stop(time: Double) {
            stoppedAt = time
        }
    }

    inner class Analyser : Node("analyser"), AudioGraphAnalyser {
        override var fftSize = 2048
        override val frequencyBinCount get() = fftSize / 2
        var magnitudes = byteArrayOf()

        override fun getByteFrequencyData(array: ByteArray) {
            magnitudes.copyInto(array)
        }
    }

    inner class Panner : Node("panner"), AudioGraphPanner {
        override var panningModel = PanningModel.EqualPower
        override var distanceModel = DistanceModel.Inverse
        override var refDistance = 1.0
        override var maxDistance = 10000.0
        override var rolloffFactor = 1.0
        override var coneInnerAngle = 360.0
        override var coneOuterAngle = 360.0
        override var coneOuterGain = 0.0
        override val positionX = Param()
        override val positionY = Param()
        override val positionZ = Param()
        override val orientationX = Param()
        override val orientationY = Param()
        override val orientationZ = Param()

        override fun setPosition(x: Double, y: Double, z: Double) = error("Unexpected legacy call.")
        override fun setOrientation(x: Double, y: Double, z: Double) = error("Unexpected legacy call.")
    }

    class Listener : AudioGraphListener {
        override val positionX = Param()
        override val positionY = Param()
        override val positionZ = Param()
        override val forwardX = Param()
        override val forwardY = Param()
        override val forwardZ = Param()
        override val upX = Param()
        override val upY = Param()
        override val upZ = Param()

        override fun setPosition(x: Double, y: Double, z: Double) = error("Unexpected legacy call.")
        override fun setOrientation(x: Double, y: Double, z: Double, upX: Double, upY: Double, upZ: Double) =
            error("Unexpected legacy call.")
    }
}
