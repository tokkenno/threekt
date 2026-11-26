package com.aitorgf.threekt.loaders

import com.aitorgf.threekt.core.BufferGeometry
import com.aitorgf.threekt.core.Float32BufferAttribute
import com.aitorgf.threekt.math.Color
import com.aitorgf.threekt.math.color.ColorSpace
import kotlin.js.JsExport

/** Parses ASCII and little- or big-endian binary PLY vertex and face data supplied as bytes. */
@JsExport
class PLYLoader {
    private class Property(val name: String, val type: String, val countType: String? = null)
    private class Element(val name: String, val count: Int, val properties: MutableList<Property> = mutableListOf())

    /** Returns vertices, optional normal/color/UV attributes and indexed triangles or quads. */
    fun parse(data: ByteArray): BufferGeometry {
        val elements = mutableListOf<Element>()
        var encoding = ""
        var cursor = 0
        fun line(): String {
            val start = cursor
            while (cursor < data.size && data[cursor] != 10.toByte()) cursor++
            require(cursor < data.size) { "Incomplete PLY header." }
            return data.decodeToString(start, cursor++).trim()
        }
        require(line() == "ply") { "Expected a PLY header." }
        while (true) {
            val tokens = line().split(plyWhitespace)
            when (tokens.first()) {
                "end_header" -> break
                "format" -> encoding = tokens[1]
                "element" -> elements.add(Element(tokens[1], tokens[2].toInt().also { require(it >= 0) }))
                "property" -> elements.last().properties.add(
                    if (tokens[1] == "list") Property(tokens[4], tokens[3], tokens[2])
                    else Property(tokens[2], tokens[1]),
                )
            }
        }
        require(encoding in setOf("ascii", "binary_little_endian", "binary_big_endian"))
        val tokens = if (encoding == "ascii") plyToken.findAll(data.decodeToString(cursor)).iterator() else null
        val binary = BinaryReader(data, encoding == "binary_little_endian", cursor)
        fun scalar(type: String): Double {
            if (tokens != null) {
                require(tokens.hasNext()) { "Truncated ASCII PLY data." }
                return tokens.next().value.toDouble()
            }
            return when (type) {
                "char", "int8" -> binary.int(1).toByte().toDouble()
                "uchar", "uint8" -> binary.int(1).toDouble()
                "short", "int16" -> binary.int(2).toShort().toDouble()
                "ushort", "uint16" -> binary.int(2).toDouble()
                "int", "int32" -> binary.int().toDouble()
                "uint", "uint32" -> (binary.int().toLong() and 0xffffffffL).toDouble()
                "float", "float32" -> binary.float().toDouble()
                "double", "float64" -> binary.double()
                else -> error("Unsupported PLY scalar type $type.")
            }
        }
        val geometry = BufferGeometry()
        val indices = mutableListOf<Int>()
        val color = Color()
        for (element in elements) {
            val names = element.properties.map { it.name }
            val x = names.indexOf("x")
            val y = names.indexOf("y")
            val z = names.indexOf("z")
            val nx = names.indexOf("nx")
            val ny = names.indexOf("ny")
            val nz = names.indexOf("nz")
            val red = names.indexOf("red")
            val green = names.indexOf("green")
            val blue = names.indexOf("blue")
            val u = names.indexOfFirst { it == "s" || it == "u" || it == "texture_u" }
            val v = names.indexOfFirst { it == "t" || it == "v" || it == "texture_v" }
            val vertex = element.name == "vertex"
            val positions = if (vertex) FloatArray(element.count * 3) else null
            val normals = if (vertex && minOf(nx, ny, nz) >= 0) FloatArray(element.count * 3) else null
            val colors = if (vertex && minOf(red, green, blue) >= 0) FloatArray(element.count * 3) else null
            val uvs = if (vertex && minOf(u, v) >= 0) FloatArray(element.count * 2) else null
            val row = DoubleArray(names.size)
            for (i in 0 until element.count) {
                for ((j, property) in element.properties.withIndex()) {
                    val countType = property.countType
                    if (countType == null) {
                        row[j] = scalar(property.type)
                    } else {
                        val count = scalar(countType).toInt()
                        require(count >= 0 && count <= data.size)
                        val face = element.name == "face" && property.name in setOf("vertex_indices", "vertex_index")
                        if (face && (count == 3 || count == 4)) {
                            val a = scalar(property.type).toInt()
                            val b = scalar(property.type).toInt()
                            val c = scalar(property.type).toInt()
                            if (count == 3) indices.addAll(listOf(a, b, c))
                            else {
                                val d = scalar(property.type).toInt()
                                indices.addAll(listOf(a, b, d, b, c, d))
                            }
                        } else repeat(count) { scalar(property.type) }
                    }
                }
                fun store(target: FloatArray, a: Double, b: Double, c: Double) {
                    target[i * 3] = a.toFloat()
                    target[i * 3 + 1] = b.toFloat()
                    target[i * 3 + 2] = c.toFloat()
                }
                positions?.let { store(it, row[x], row[y], row[z]) }
                normals?.let { store(it, row[nx], row[ny], row[nz]) }
                colors?.let {
                    color.setRGB(row[red] / 255, row[green] / 255, row[blue] / 255, ColorSpace.SRGB)
                    store(it, color.r, color.g, color.b)
                }
                if (uvs != null) {
                    uvs[i * 2] = row[u].toFloat()
                    uvs[i * 2 + 1] = row[v].toFloat()
                }
            }
            positions?.let { geometry.setAttribute("position", Float32BufferAttribute(it, 3)) }
            normals?.let { geometry.setAttribute("normal", Float32BufferAttribute(it, 3)) }
            colors?.let { geometry.setAttribute("color", Float32BufferAttribute(it, 3)) }
            uvs?.let { geometry.setAttribute("uv", Float32BufferAttribute(it, 2)) }
        }
        val count = requireNotNull(geometry.getAttribute("position")).count
        require(indices.all { it in 0 until count }) { "PLY face index out of bounds." }
        if (indices.isNotEmpty()) geometry.setIndex(indices.toIntArray())
        geometry.computeBoundingSphere()
        return geometry
    }
}

private val plyWhitespace = Regex("\\s+")
private val plyToken = Regex("\\S+")
