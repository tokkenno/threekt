package com.aitorgf.threekt.exporters

/** Sequential writer for the fixed-size binary geometry formats. */
internal class BinaryWriter(val bytes: ByteArray, var offset: Int = 0, private val littleEndian: Boolean = true) {
    fun byte(value: Int) {
        bytes[offset++] = value.toByte()
    }

    fun int(value: Int) {
        for (i in 0..3) byte(value ushr ((if (littleEndian) i else 3 - i) * 8))
    }

    fun float(value: Double) = int(value.toFloat().toRawBits())
}
