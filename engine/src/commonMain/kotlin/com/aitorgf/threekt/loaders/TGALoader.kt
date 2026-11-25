package com.aitorgf.threekt.loaders

import kotlin.js.JsExport

/** Decodes grayscale, indexed and true-color TGA data, including run-length encoding. */
@JsExport
class TGALoader {
    /** Returns a platform-independent RGBA image. Threekt leaves texture creation to the caller. */
    fun parse(data: ByteArray): TGAImage {
        require(data.size >= 18) { "Truncated TGA header." }
        fun byte(i: Int) = data[i].toInt() and 255
        fun uint16(i: Int) = byte(i) or (byte(i + 1) shl 8)
        val imageType = byte(2)
        val rle = imageType in 9..11
        val type = if (rle) imageType - 8 else imageType
        val indexed = type == 1
        val grayscale = type == 3
        val pixelDepth = byte(16)
        require(when (type) {
            1 -> byte(1) == 1 && (pixelDepth == 8 || pixelDepth == 16)
            2 -> pixelDepth == 16 || pixelDepth == 24 || pixelDepth == 32
            3 -> pixelDepth == 8 || pixelDepth == 16
            else -> false
        }) { "Unsupported TGA image format." }
        val width = uint16(12)
        val height = uint16(14)
        require(width > 0 && height > 0 && width.toLong() * height <= Int.MAX_VALUE / 4)
        val count = width * height
        val descriptor = byte(17)
        val firstPaletteIndex = uint16(3)
        val paletteCount = if (byte(1) == 1) uint16(5) else 0
        val paletteDepth = byte(7)
        val paletteSize = (paletteDepth + 7) / 8
        if (indexed) require(paletteCount > 0 && paletteDepth in listOf(15, 16, 24, 32))
        val paletteOffset = 18 + byte(0)
        val bytesPerPixel = pixelDepth / 8
        var cursor = paletteOffset + paletteCount * paletteSize
        require(cursor <= data.size)
        if (!rle) require(count <= (data.size - cursor) / bytesPerPixel)
        if (rle) require(count.toLong() <= (data.size - cursor).toLong() * 128)
        val rgba = ByteArray(count * 4)
        var written = 0

        fun write(source: Int) {
            val x = written % width
            val y = written / width
            val outputX = if (descriptor and 16 == 0) x else width - 1 - x
            val outputY = if (descriptor and 32 != 0) y else height - 1 - y
            val dest = (outputY * width + outputX) * 4
            if (grayscale) {
                val shade = data[source]
                rgba[dest] = shade
                rgba[dest + 1] = shade
                rgba[dest + 2] = shade
                rgba[dest + 3] = if (pixelDepth == 16) data[source + 1] else -1
            } else {
                val sourceIndex = if (indexed) {
                    val index = if (pixelDepth == 16) uint16(source) else byte(source)
                    val paletteIndex = index - firstPaletteIndex
                    require(paletteIndex in 0 until paletteCount)
                    paletteOffset + paletteIndex * paletteSize
                } else source
                val depth = if (indexed) paletteDepth else pixelDepth
                if (depth == 15 || depth == 16) {
                    val packed = uint16(sourceIndex)
                    rgba[dest] = (((packed shr 10) and 31) * 255 / 31).toByte()
                    rgba[dest + 1] = (((packed shr 5) and 31) * 255 / 31).toByte()
                    rgba[dest + 2] = ((packed and 31) * 255 / 31).toByte()
                    rgba[dest + 3] = if (depth == 16 && descriptor and 15 != 0 && packed and 0x8000 == 0) 0 else -1
                } else {
                    rgba[dest] = data[sourceIndex + 2]
                    rgba[dest + 1] = data[sourceIndex + 1]
                    rgba[dest + 2] = data[sourceIndex]
                    rgba[dest + 3] = if (depth == 32) data[sourceIndex + 3] else -1
                }
            }
            written++
        }

        while (written < count) {
            val packet = if (rle) {
                require(cursor < data.size)
                byte(cursor++)
            } else 0
            val length = (packet and 127) + 1
            require(length <= count - written)
            val repeated = packet and 128 != 0
            val bytes = (if (repeated) 1 else length) * bytesPerPixel
            require(bytes <= data.size - cursor)
            repeat(length) { index -> write(cursor + (if (repeated) 0 else index * bytesPerPixel)) }
            cursor += bytes
        }
        return TGAImage(width, height, rgba)
    }
}
