package com.aitorgf.threekt.loaders

/** Reads explicitly ordered bytes from a model asset, advancing [offset] after each field. */
internal class BinaryReader(val data: ByteArray, val littleEndian: Boolean, var offset: Int = 0) {
    fun unsignedByte(): Int {
        require(offset in data.indices) { "Truncated binary data." }
        return data[offset++].toInt() and 255
    }

    fun int(bytes: Int = 4): Int {
        var value = 0
        for (i in 0 until bytes) {
            val position = if (littleEndian) i else bytes - 1 - i
            value = value or (unsignedByte() shl (position * 8))
        }
        return value
    }

    fun float(): Float = Float.fromBits(int())

    fun double(): Double {
        val first = int().toLong() and 0xffffffffL
        val second = int().toLong() and 0xffffffffL
        return Double.fromBits(if (littleEndian) (second shl 32) or first else (first shl 32) or second)
    }
}
