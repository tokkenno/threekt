package com.aitorgf.threekt.loaders

import com.aitorgf.threekt.core.Float32BufferAttribute
import com.aitorgf.threekt.math.Color
import com.aitorgf.threekt.math.color.ColorSpace
import kotlin.js.JsExport

private val stlWhitespace = Regex("\\s+")

/** Reads ASCII and little-endian binary STL, including the Magics binary color extension. */
@JsExport
class STLLoader {
    /** Parses a complete STL file. Binary STL is identified by its declared triangle count and length. */
    fun parse(bytes: ByteArray): STLGeometry {
        if (bytes.size >= 84) {
            val count = readInt(bytes, 80)
            if (count >= 0 && 84L + count * 50L == bytes.size.toLong()) return parseBinary(bytes, count)
        }
        return parseAscii(bytes.decodeToString())
    }

    private fun parseAscii(text: String): STLGeometry {
        require(text.trimStart().startsWith("solid")) { "Invalid or truncated STL data." }
        val normals = mutableListOf<Float>()
        val positions = mutableListOf<Float>()
        val normal = FloatArray(3)
        var inFacet = false
        var vertexCount = 0
        for (raw in text.lineSequence()) {
            val fields = raw.trim().split(stlWhitespace)
            when (fields[0]) {
                "facet" -> {
                    require(!inFacet && fields.size == 5 && fields[1] == "normal")
                    for (i in 0..2) normal[i] = fields[i + 2].toFloat()
                    inFacet = true
                    vertexCount = 0
                }
                "vertex" -> {
                    require(inFacet && vertexCount < 3 && fields.size == 4)
                    for (i in 0..2) {
                        positions.add(fields[i + 1].toFloat())
                        normals.add(normal[i])
                    }
                    vertexCount++
                }
                "endfacet" -> {
                    require(inFacet && vertexCount == 3)
                    inFacet = false
                }
            }
        }
        require(!inFacet)
        return geometry(positions.toFloatArray(), normals.toFloatArray(), null, 1.0)
    }

    private fun parseBinary(bytes: ByteArray, triangles: Int): STLGeometry {
        val positions = FloatArray(triangles * 9)
        val normals = FloatArray(positions.size)
        val header = "COLOR=".encodeToByteArray()
        val colorOffset = (0..(80 - header.size - 4)).firstOrNull { index ->
            header.indices.all { bytes[index + it] == header[it] }
        }?.plus(header.size)
        val colors = colorOffset?.let { FloatArray(positions.size) }
        val alpha = colorOffset?.let { octet(bytes, it + 3) / 255.0 } ?: 1.0
        val color = Color()
        for (face in 0 until triangles) {
            val start = 84 + face * 50
            val offset = face * 9
            if (colorOffset != null) {
                val packed = octet(bytes, start + 48) or (octet(bytes, start + 49) shl 8)
                if (packed and 0x8000 == 0) {
                    color.setRGB((packed and 31) / 31.0, (packed shr 5 and 31) / 31.0, (packed shr 10 and 31) / 31.0, ColorSpace.SRGB)
                } else {
                    color.setRGB(octet(bytes, colorOffset) / 255.0, octet(bytes, colorOffset + 1) / 255.0,
                        octet(bytes, colorOffset + 2) / 255.0, ColorSpace.SRGB)
                }
            }
            for (vertex in 0..2) {
                val index = offset + vertex * 3
                for (axis in 0..2) {
                    positions[index + axis] = Float.fromBits(readInt(bytes, start + 12 + (vertex * 3 + axis) * 4))
                    normals[index + axis] = Float.fromBits(readInt(bytes, start + axis * 4))
                }
                if (colors != null) {
                    colors[index] = color.r.toFloat()
                    colors[index + 1] = color.g.toFloat()
                    colors[index + 2] = color.b.toFloat()
                }
            }
        }
        return geometry(positions, normals, colors, alpha)
    }

    private fun geometry(positions: FloatArray, normals: FloatArray, colors: FloatArray?, alpha: Double): STLGeometry =
        STLGeometry(alpha).apply {
            setAttribute("position", Float32BufferAttribute(positions, 3))
            setAttribute("normal", Float32BufferAttribute(normals, 3))
            if (colors != null) setAttribute("color", Float32BufferAttribute(colors, 3))
        }

    private fun octet(bytes: ByteArray, offset: Int) = bytes[offset].toInt() and 255

    private fun readInt(bytes: ByteArray, offset: Int): Int =
        octet(bytes, offset) or (octet(bytes, offset + 1) shl 8) or
            (octet(bytes, offset + 2) shl 16) or (octet(bytes, offset + 3) shl 24)
}
