package com.aitorgf.threekt.audio

import com.aitorgf.threekt.core.Object3D
import kotlin.js.JsExport
import kotlin.jvm.JvmName
import kotlin.math.max

/**
 * A non-positional, global audio source attached to an [AudioListener]. It plays an [AudioGraphBuffer] with
 * playback control, or any [AudioGraphNode] set with [setNodeSource].
 *
 * Threekt deviations:
 * - Kotlin type checks replace the `type` string.
 * - `setMediaElementSource` and `setMediaStreamSource` need browser objects, which common code cannot take. Create
 *   the node with `WebAudioContext.createMediaElementSource` or `createMediaStreamSource` and pass it to
 *   [setNodeSource]; [sourceType] is then [SourceType.AudioNode].
 * - On the JVM, the getters of [detune], [loop] and [playbackRate] are named after the property, because
 *   upstream's `getDetune()`, `getLoop()` and `getPlaybackRate()` methods take the `get` names.
 * - Methods that upstream ends early with a warning return this object instead of `undefined`.
 */
@JsExport
open class Audio(val listener: AudioListener) : Object3D() {
    /** What [source] holds. */
    enum class SourceType { Empty, Buffer, AudioNode }

    val context: AudioGraphContext = listener.context

    /** The output volume, connected to the listener's input. */
    val gain: AudioGraphGain = context.createGain()

    /** Whether [setBuffer] starts playback right away. */
    var autoplay = false
    var buffer: AudioGraphBuffer? = null
        private set

    /** Detune in cents. */
    @get:JvmName("detune")
    var detune = 0.0
        private set
    @get:JvmName("loop")
    var loop = false
        private set

    /** Loop start and end in seconds; 0 for both loops the whole buffer. */
    var loopStart = 0.0
        private set
    var loopEnd = 0.0
        private set

    /** Seconds into the buffer at which playback starts. */
    var offset = 0.0

    /** Seconds to play, or null to play to the end of the buffer. */
    var duration: Double? = null
    @get:JvmName("playbackRate")
    var playbackRate = 1.0
        private set
    var isPlaying = false
        private set

    /** Whether the source can be played, paused and stopped, which only buffer sources allow. */
    var hasPlaybackControl = true
        private set
    var source: AudioGraphNode? = null
        private set
    var sourceType = SourceType.Empty
        private set

    private var startedAt = 0.0
    private var progress = 0.0
    private var connected = false
    private var filters: List<AudioGraphNode> = emptyList()

    init {
        gain.connect(listener.getInput())
    }

    /** Returns the node that this audio sends to the listener; analysers connect to it. */
    open fun getOutput(): AudioGraphNode = gain

    /** Plays [audioNode] through this audio. Removes playback control. */
    fun setNodeSource(audioNode: AudioGraphNode): Audio {
        hasPlaybackControl = false
        sourceType = SourceType.AudioNode
        source = audioNode
        connect()
        return this
    }

    /** Sets the buffer to play, and starts playback when [autoplay] is set. */
    fun setBuffer(audioBuffer: AudioGraphBuffer): Audio {
        buffer = audioBuffer
        sourceType = SourceType.Buffer
        if (autoplay) play()
        return this
    }

    /** Starts playback after [delay] seconds, from where [pause] left it. */
    fun play(delay: Double = 0.0): Audio {
        if (isPlaying) {
            warn("Audio: Audio is already playing.")
            return this
        }
        if (!hasPlaybackControl) {
            warn("Audio: this Audio has no playback control.")
            return this
        }
        startedAt = context.currentTime + delay
        val source = context.createBufferSource()
        source.buffer = buffer
        source.loop = loop
        source.loopStart = loopStart
        source.loopEnd = loopEnd
        source.onEnded = ::onEnded
        source.start(startedAt, progress + offset, duration)
        isPlaying = true
        this.source = source
        setDetune(detune)
        setPlaybackRate(playbackRate)
        return connect()
    }

    /** Pauses playback, keeping the position for the next [play]. */
    fun pause(): Audio {
        if (!hasPlaybackControl) {
            warn("Audio: this Audio has no playback control.")
            return this
        }
        if (isPlaying) {
            // Update the current progress.
            progress += max(context.currentTime - startedAt, 0.0) * playbackRate
            // Keep the progress of looped audio within its duration.
            if (loop) progress %= duration ?: buffer!!.duration
            val source = source as AudioGraphBufferSource
            source.stop(0.0)
            source.onEnded = null
            isPlaying = false
        }
        return this
    }

    /** Stops playback after [delay] seconds and rewinds to the start. */
    fun stop(delay: Double = 0.0): Audio {
        if (!hasPlaybackControl) {
            warn("Audio: this Audio has no playback control.")
            return this
        }
        progress = 0.0
        (source as AudioGraphBufferSource?)?.let {
            it.stop(context.currentTime + delay)
            it.onEnded = null
        }
        isPlaying = false
        return this
    }

    /** Connects the source through the filters to the output. Called by the setters; rarely needed directly. */
    open fun connect(): Audio {
        val source = source!!
        if (filters.isNotEmpty()) {
            source.connect(filters[0])
            for (i in 1 until filters.size) filters[i - 1].connect(filters[i])
            filters.last().connect(getOutput())
        } else {
            source.connect(getOutput())
        }
        connected = true
        return this
    }

    /** Undoes [connect]. */
    open fun disconnect(): Audio {
        if (!connected) return this
        val source = source!!
        if (filters.isNotEmpty()) {
            source.disconnect(filters[0])
            for (i in 1 until filters.size) filters[i - 1].disconnect(filters[i])
            filters.last().disconnect(getOutput())
        } else {
            source.disconnect(getOutput())
        }
        connected = false
        return this
    }

    /** Returns the filters between the source and the output, in order. */
    fun getFilters(): Array<AudioGraphNode> = filters.toTypedArray()

    /** Replaces the filters between the source and the output, reconnecting if needed. */
    fun setFilters(value: Array<AudioGraphNode>?): Audio {
        if (connected) {
            disconnect()
            filters = value?.toList().orEmpty()
            connect()
        } else {
            filters = value?.toList().orEmpty()
        }
        return this
    }

    fun setDetune(value: Double): Audio {
        detune = value
        if (isPlaying) {
            (source as AudioGraphBufferSource).detune?.setTargetAtTime(detune, context.currentTime, 0.01)
        }
        return this
    }

    fun getDetune(): Double = detune

    /** Returns the first filter, or null. */
    fun getFilter(): AudioGraphNode? = filters.firstOrNull()

    /** Sets a single filter, or removes all filters for null. */
    fun setFilter(filter: AudioGraphNode?): Audio = setFilters(if (filter != null) arrayOf(filter) else emptyArray())

    fun setPlaybackRate(value: Double): Audio {
        if (!hasPlaybackControl) {
            warn("Audio: this Audio has no playback control.")
            return this
        }
        playbackRate = value
        if (isPlaying) {
            (source as AudioGraphBufferSource).playbackRate.setTargetAtTime(playbackRate, context.currentTime, 0.01)
        }
        return this
    }

    fun getPlaybackRate(): Double = playbackRate

    /** Called when the buffer source ends on its own. */
    fun onEnded() {
        isPlaying = false
        progress = 0.0
    }

    fun getLoop(): Boolean {
        if (!hasPlaybackControl) {
            warn("Audio: this Audio has no playback control.")
            return false
        }
        return loop
    }

    fun setLoop(value: Boolean): Audio {
        if (!hasPlaybackControl) {
            warn("Audio: this Audio has no playback control.")
            return this
        }
        loop = value
        if (isPlaying) (source as AudioGraphBufferSource).loop = loop
        return this
    }

    fun setLoopStart(value: Double): Audio {
        loopStart = value
        return this
    }

    fun setLoopEnd(value: Double): Audio {
        loopEnd = value
        return this
    }

    fun getVolume(): Double = gain.gain.value

    /** Fades the volume to [value] with a time constant of 10 ms. */
    fun setVolume(value: Double): Audio {
        gain.gain.setTargetAtTime(value, context.currentTime, 0.01)
        return this
    }

    /** Copies the transform and, for buffer sources, the playback settings of [source]. */
    override fun copy(source: Object3D, recursive: Boolean): Audio {
        super.copy(source, recursive)
        if (source !is Audio) return this
        if (source.sourceType != SourceType.Buffer) {
            warn("Audio: Audio source type cannot be copied.")
            return this
        }
        autoplay = source.autoplay
        buffer = source.buffer
        detune = source.detune
        loop = source.loop
        loopStart = source.loopStart
        loopEnd = source.loopEnd
        offset = source.offset
        duration = source.duration
        playbackRate = source.playbackRate
        hasPlaybackControl = source.hasPlaybackControl
        sourceType = source.sourceType
        filters = source.filters
        return this
    }

    override fun clone(recursive: Boolean): Audio = Audio(listener).copy(this, recursive)
}
