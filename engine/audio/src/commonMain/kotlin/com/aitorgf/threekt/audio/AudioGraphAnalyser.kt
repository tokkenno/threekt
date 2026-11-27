package com.aitorgf.threekt.audio

import kotlin.js.JsExport

/** A node that exposes frequency data, the platform-neutral counterpart of the Web Audio `AnalyserNode`. */
@JsExport
interface AudioGraphAnalyser : AudioGraphNode {
    /** Size of the FFT, a power of two between 32 and 32768. */
    var fftSize: Int

    /** Number of frequency bins, half of [fftSize]. */
    val frequencyBinCount: Int

    /**
     * Writes the current magnitudes into [array], one byte per bin. The bytes hold unsigned values from 0 to 255;
     * Kotlin reads them as signed, so use `toInt() and 0xff`.
     */
    fun getByteFrequencyData(array: ByteArray)
}
