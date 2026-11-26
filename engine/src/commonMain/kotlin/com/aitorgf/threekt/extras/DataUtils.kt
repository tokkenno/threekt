package com.aitorgf.threekt.extras

import com.aitorgf.threekt.math.MathUtils
import kotlin.js.JsExport

/**
 * Conversions between 32-bit floats and the 16-bit half floats stored by
 * [com.aitorgf.threekt.core.Float16BufferAttribute], using the lookup tables of Three.js `DataUtils`.
 */
@JsExport
object DataUtils {
    private val baseTable = IntArray(512)
    private val shiftTable = IntArray(512)
    private val mantissaTable = IntArray(2048)
    private val exponentTable = IntArray(64)
    private val offsetTable = IntArray(64)

    init {
        for (i in 0 until 256) {
            val e = i - 127
            when {
                e < -27 -> {
                    baseTable[i] = 0x0000
                    baseTable[i or 0x100] = 0x8000
                    shiftTable[i] = 24
                    shiftTable[i or 0x100] = 24
                }
                e < -14 -> {
                    baseTable[i] = 0x0400 shr (-e - 14)
                    baseTable[i or 0x100] = (0x0400 shr (-e - 14)) or 0x8000
                    shiftTable[i] = -e - 1
                    shiftTable[i or 0x100] = -e - 1
                }
                e <= 15 -> {
                    baseTable[i] = (e + 15) shl 10
                    baseTable[i or 0x100] = ((e + 15) shl 10) or 0x8000
                    shiftTable[i] = 13
                    shiftTable[i or 0x100] = 13
                }
                e < 128 -> {
                    baseTable[i] = 0x7c00
                    baseTable[i or 0x100] = 0xfc00
                    shiftTable[i] = 24
                    shiftTable[i or 0x100] = 24
                }
                else -> {
                    baseTable[i] = 0x7c00
                    baseTable[i or 0x100] = 0xfc00
                    shiftTable[i] = 13
                    shiftTable[i or 0x100] = 13
                }
            }
        }
        for (i in 1 until 1024) {
            var m = i shl 13
            var e = 0
            while (m and 0x00800000 == 0) {
                m = m shl 1
                e -= 0x00800000
            }
            m = m and 0x00800000.inv()
            e += 0x38800000
            mantissaTable[i] = m or e
        }
        for (i in 1024 until 2048) mantissaTable[i] = 0x38000000 + ((i - 1024) shl 13)
        for (i in 1 until 31) exponentTable[i] = i shl 23
        exponentTable[31] = 0x47800000
        exponentTable[32] = 0x80000000.toInt()
        for (i in 33 until 63) exponentTable[i] = 0x80000000.toInt() + ((i - 32) shl 23)
        exponentTable[63] = 0xc7800000.toInt()
        for (i in 1 until 64) if (i != 32) offsetTable[i] = 1024
    }

    /**
     * Returns the half-float bits, from `0` to `0xffff`, that represent [value]. Values beyond ±65504, the
     * largest half float, are clamped; Three.js also logs a warning for them.
     */
    fun toHalfFloat(value: Double): Int {
        val f = MathUtils.clamp(value, -65504.0, 65504.0).toFloat().toRawBits()
        val e = (f shr 23) and 0x1ff
        return baseTable[e] + ((f and 0x007fffff) shr shiftTable[e])
    }

    /** Returns the number represented by the half-float bits [value], from `0` to `0xffff`. */
    fun fromHalfFloat(value: Int): Double {
        val m = value shr 10
        return Float.fromBits(mantissaTable[offsetTable[m] + (value and 0x3ff)] + exponentTable[m]).toDouble()
    }
}
