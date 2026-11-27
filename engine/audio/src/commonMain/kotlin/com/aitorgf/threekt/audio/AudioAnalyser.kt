package com.aitorgf.threekt.audio

import kotlin.js.JsExport

/**
 * Reads the frequency spectrum of an [Audio].
 *
 * Threekt deviation: [data] is a [ByteArray] (an `Int8Array` in JavaScript) instead of a `Uint8Array`, so that it
 * can back a `DataTexture`. Its bytes hold unsigned magnitudes from 0 to 255.
 */
@JsExport
class AudioAnalyser(audio: Audio, fftSize: Int = 2048) {
    val analyser: AudioGraphAnalyser = audio.context.createAnalyser()

    /** Magnitudes written by [getFrequencyData], one byte per bin. */
    val data: ByteArray

    init {
        analyser.fftSize = fftSize
        data = ByteArray(analyser.frequencyBinCount)
        audio.getOutput().connect(analyser)
    }

    /** Fills [data] with the current magnitudes and returns it. */
    fun getFrequencyData(): ByteArray {
        analyser.getByteFrequencyData(data)
        return data
    }

    /** Returns the average magnitude over all bins, from 0 to 255. */
    fun getAverageFrequency(): Double {
        var value = 0.0
        val data = getFrequencyData()
        for (i in data.indices) value += data[i].toInt() and 0xff
        return value / data.size
    }
}
