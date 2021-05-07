package com.aitorgf.threekt.types

import kotlin.experimental.and
import kotlin.experimental.or
import kotlin.random.Random

@ExperimentalJsExport
@JsExport
class UUID private constructor(
    val bits: ByteArray
) {
    companion object {
        fun randomUUID(): UUID {
            val bits: ByteArray = Random.nextBytes(16)
            bits[6] = (bits[6] and 0xf) or 0x40
            bits[8] = (bits[8] and 0x3f) or 0x8
            return UUID(bits)
        }
    }

    override fun toString(): String {
        val HEX_CHARS = "0123456789ABCDEF".toCharArray()

        val result = StringBuilder()

        this.bits.forEach {
            val octet = it.toInt()
            val firstIndex = (octet and 0xF0).ushr(4)
            val secondIndex = octet and 0x0F
            result.append(HEX_CHARS[firstIndex])
            result.append(HEX_CHARS[secondIndex])
        }

        return result.toString()
    }
}
