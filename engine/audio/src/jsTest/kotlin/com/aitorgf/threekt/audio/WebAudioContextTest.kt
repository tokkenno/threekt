package com.aitorgf.threekt.audio

import com.aitorgf.threekt.audio.webaudio.AudioBuffer
import com.aitorgf.threekt.audio.webaudio.BaseAudioContext
import org.khronos.webgl.Float32Array
import org.khronos.webgl.get
import org.khronos.webgl.set
import kotlin.js.Promise
import kotlin.math.abs
import kotlin.math.sin
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WebAudioContextTest {
    private val sampleRate = 8000

    @AfterTest
    fun resetContext() = AudioContext.setContext(null)

    /** A one-second offline context, which renders without a user gesture or audio device. */
    private fun offlineContext(): WebAudioContext {
        val native = js("new OfflineAudioContext(1, 8000, 8000)").unsafeCast<BaseAudioContext>()
        return WebAudioContext(native).also { AudioContext.setContext(it) }
    }

    private fun tone(context: WebAudioContext, seconds: Double): AudioGraphBuffer {
        val length = (sampleRate * seconds).toInt()
        val buffer = context.context.asDynamic().createBuffer(1, length, sampleRate)
        val samples = buffer.getChannelData(0).unsafeCast<Float32Array>()
        for (i in 0 until length) samples[i] = sin(i * 0.3).toFloat()
        return context.wrapBuffer(buffer.unsafeCast<AudioBuffer>())
    }

    private fun render(context: WebAudioContext): Promise<Float32Array> =
        context.context.asDynamic().startRendering().unsafeCast<Promise<Any>>()
            .then { rendered -> rendered.asDynamic().getChannelData(0).unsafeCast<Float32Array>() }

    private fun peak(samples: Float32Array, from: Int, to: Int): Double =
        (from until to).maxOf { abs(samples[it].toDouble()) }

    @Test
    fun audioPlaysItsBufferThroughTheListener(): Promise<Unit> {
        val context = offlineContext()
        val audio = Audio(AudioListener())
        audio.setBuffer(tone(context, 0.25)).play(0.5)
        assertTrue(audio.isPlaying)
        return render(context).then { samples ->
            assertEquals(0.0, peak(samples, 0, 3900))
            assertTrue(peak(samples, 4100, 5900) > 0.5)
            assertEquals(0.0, peak(samples, 6100, 8000))
        }
    }

    @Test
    fun volumeAndWrappedFiltersShapeTheOutput(): Promise<Unit> {
        val context = offlineContext()
        val listener = AudioListener()
        val audio = Audio(listener)
        val mute = context.context.asDynamic().createGain()
        mute.gain.value = 0.0
        audio.setBuffer(tone(context, 1.0)).setFilter(context.wrapNode(mute.unsafeCast<com.aitorgf.threekt.audio.webaudio.AudioNode>()))
        audio.play()
        return render(context).then { samples -> assertEquals(0.0, peak(samples, 0, 8000)) }
    }

    @Test
    fun positionalAudioAndAnalyserUseNativeNodes(): Promise<Unit> {
        val context = offlineContext()
        val listener = AudioListener()
        val audio = PositionalAudio(listener)
        audio.setBuffer(tone(context, 1.0))
        audio.setRefDistance(2.0).setDirectionalCone(180.0, 230.0, 0.1)
        audio.setDistanceModel(DistanceModel.Linear)
        assertEquals(DistanceModel.Linear, audio.getDistanceModel())
        assertEquals(PanningModel.HRTF, audio.panner.panningModel)
        val analyser = AudioAnalyser(audio, 32)
        assertEquals(16, analyser.data.size)
        audio.position.set(1.0, 0.0, -2.0)
        audio.play()
        audio.updateMatrixWorld(true)
        listener.updateMatrixWorld(true)
        return render(context).then { samples ->
            assertTrue(peak(samples, 0, 8000) > 0.01)
            assertTrue(analyser.getAverageFrequency() >= 0.0)
        }
    }
}
