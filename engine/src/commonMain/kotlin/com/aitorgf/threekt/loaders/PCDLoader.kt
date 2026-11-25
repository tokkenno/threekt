package com.aitorgf.threekt.loaders

import com.aitorgf.threekt.core.BufferGeometry
import com.aitorgf.threekt.core.Float32BufferAttribute
import com.aitorgf.threekt.materials.PointsMaterial
import com.aitorgf.threekt.math.Color
import com.aitorgf.threekt.math.color.ColorSpace
import com.aitorgf.threekt.objects.Points
import kotlin.js.JsExport

private val pcdWhitespace = Regex("\\s+")

/**
 * Parses PCD point positions and packed RGB colors in ASCII, little-endian binary, or LZF-compressed
 * binary format. The caller supplies the bytes as a Kotlin ByteArray (Int8Array on JavaScript).
 */
@JsExport
class PCDLoader {
    /** Returns a point cloud with a vertex-colored material when the file supplies RGB. */
    fun parse(bytes: ByteArray): Points {
        val header = mutableMapOf<String, List<String>>()
        var offset = 0
        while (offset < bytes.size && "DATA" !in header) {
            var end = offset
            while (end < bytes.size && bytes[end] != 10.toByte()) end++
            val line = bytes.decodeToString(offset, end).substringBefore('#').trim()
            offset = minOf(end + 1, bytes.size)
            if (line.isEmpty()) continue
            val words = line.split(pcdWhitespace)
            header[words.first().uppercase()] = words.drop(1)
        }
        val encoding = header["DATA"]?.singleOrNull()
        require(encoding == "ascii" || encoding == "binary" || encoding == "binary_compressed") { "Unsupported PCD encoding." }
        val names = requireNotNull(header["FIELDS"]) { "Missing PCD fields." }
        val sizes = requireNotNull(header["SIZE"])
        val types = requireNotNull(header["TYPE"])
        val counts = header["COUNT"] ?: List(names.size) { "1" }
        require(sizes.size == names.size && types.size == names.size && counts.size == names.size)
        var rowSize = 0
        var column = 0
        val fields = names.mapIndexed { index, name ->
            val size = sizes[index].toInt()
            val count = counts[index].toInt()
            val type = types[index]
            require(size in listOf(1, 2, 4, 8) && count > 0 && (type == "F" || type == "I" || type == "U"))
            require(count <= (Int.MAX_VALUE - rowSize) / size)
            Field(name, size, count, type, rowSize, column).also {
                rowSize += size * count
                column += count
            }
        }.associateBy { it.name }
        val count = header["POINTS"]?.single()?.toIntOrNull() ?: error("Missing PCD point count.")
        require(rowSize > 0 && count in 0..(Int.MAX_VALUE / maxOf(rowSize, 3)))
        val x = requireNotNull(fields["x"])
        val y = requireNotNull(fields["y"])
        val z = requireNotNull(fields["z"])
        val rgb = fields["rgb"]
        require(rgb == null || rgb.size == 4)

        val compressed = encoding == "binary_compressed"
        val data = when (encoding) {
            "ascii" -> {
                require(count <= bytes.size - offset) { "Truncated ASCII PCD data." }
                bytes
            }
            "binary" -> {
                require(rowSize * count <= bytes.size - offset) { "Truncated binary PCD data." }
                bytes
            }
            else -> {
                require(offset <= bytes.size - 8)
                val compressedSize = readBits(bytes, offset, 4).toInt()
                val decompressedSize = readBits(bytes, offset + 4, 4).toInt()
                require(decompressedSize == rowSize * count && compressedSize in 0..(bytes.size - offset - 8))
                // A single LZF run can expand to at most 264 bytes.
                require(decompressedSize <= compressedSize.toLong() * 264)
                decompressLZF(bytes, offset + 8, compressedSize, decompressedSize)
            }
        }
        val positions = FloatArray(count * 3)
        val colors = if (rgb != null) FloatArray(positions.size) else null
        val color = Color()

        fun setColor(vertex: Int, packed: Int) {
            val out = colors ?: return
            color.setRGB((packed ushr 16 and 255) / 255.0, (packed ushr 8 and 255) / 255.0, (packed and 255) / 255.0, ColorSpace.SRGB)
            out[vertex] = color.r.toFloat()
            out[vertex + 1] = color.g.toFloat()
            out[vertex + 2] = color.b.toFloat()
        }

        if (encoding == "ascii") {
            val lines = bytes.decodeToString(offset).lineSequence().map { it.substringBefore('#').trim() }.filter { it.isNotEmpty() }.iterator()
            for (index in 0 until count) {
                require(lines.hasNext()) { "Truncated ASCII PCD data." }
                val values = lines.next().split(pcdWhitespace)
                require(values.size >= column)
                val vertex = index * 3
                positions[vertex] = values[x.column].toFloat()
                positions[vertex + 1] = values[y.column].toFloat()
                positions[vertex + 2] = values[z.column].toFloat()
                if (rgb != null) {
                    val value = values[rgb.column]
                    setColor(vertex, if (rgb.type == "F") value.toFloat().toRawBits() else value.toLong().toInt())
                }
            }
        } else {
            fun fieldOffset(field: Field, index: Int): Int =
                if (compressed) count * field.offset + index * field.size * field.count
                else offset + index * rowSize + field.offset

            for (index in 0 until count) {
                val vertex = index * 3
                positions[vertex] = readNumber(data, fieldOffset(x, index), x)
                positions[vertex + 1] = readNumber(data, fieldOffset(y, index), y)
                positions[vertex + 2] = readNumber(data, fieldOffset(z, index), z)
                if (rgb != null) setColor(vertex, readBits(data, fieldOffset(rgb, index), 4).toInt())
            }
        }

        val geometry = BufferGeometry().setAttribute("position", Float32BufferAttribute(positions, 3))
        if (colors != null) geometry.setAttribute("color", Float32BufferAttribute(colors, 3))
        val material = PointsMaterial().apply {
            size = 0.005
            vertexColors = colors != null
        }
        return Points(geometry, material)
    }

    private fun readNumber(data: ByteArray, offset: Int, field: Field): Float {
        val bits = readBits(data, offset, field.size)
        return when (field.type) {
            "F" -> when (field.size) {
                4 -> Float.fromBits(bits.toInt())
                8 -> Double.fromBits(bits).toFloat()
                else -> error("Unsupported floating-point PCD field size: ${field.size}")
            }
            "I" -> {
                val shift = (8 - field.size) * 8
                ((bits shl shift) shr shift).toFloat()
            }
            else -> bits.toULong().toFloat()
        }
    }

    private fun readBits(data: ByteArray, offset: Int, size: Int): Long {
        var bits = 0L
        for (i in 0 until size) bits = bits or ((data[offset + i].toLong() and 255L) shl (8 * i))
        return bits
    }

    private fun decompressLZF(input: ByteArray, offset: Int, size: Int, length: Int): ByteArray {
        val output = ByteArray(length)
        var read = offset
        var written = 0
        val end = offset + size
        fun readByte(): Int {
            require(read < end) { "Truncated LZF data." }
            return input[read++].toInt() and 255
        }
        while (read < end) {
            val control = readByte()
            if (control < 32) {
                val count = control + 1
                require(count <= end - read && count <= length - written)
                input.copyInto(output, written, read, read + count)
                read += count
                written += count
            } else {
                var count = control shr 5
                val high = (control and 31) shl 8
                if (count == 7) count += readByte()
                var reference = written - high - readByte() - 1
                count += 2
                require(reference >= 0 && count <= length - written) { "Invalid LZF back-reference." }
                repeat(count) { output[written++] = output[reference++] }
            }
        }
        require(written == length)
        return output
    }

    private class Field(val name: String, val size: Int, val count: Int, val type: String, val offset: Int, val column: Int)
}
