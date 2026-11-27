package com.aitorgf.threekt.loaders

import com.aitorgf.threekt.core.BufferGeometry
import com.aitorgf.threekt.core.Float32BufferAttribute
import com.aitorgf.threekt.math.Color
import kotlin.js.JsExport
import kotlin.math.abs

/**
 * Reads MagicaVoxel SIZE, XYZI and RGBA chunks and builds greedy surface meshes in Y-up coordinates.
 * Threekt returns the model geometries in file order. Scene transforms and material chunks are skipped.
 */
@JsExport
class VOXLoader {
    private class Model(val size: IntArray, val volume: ByteArray)

    fun parse(data: ByteArray): Array<BufferGeometry> {
        require(data.size >= 20 && data.decodeToString(0, 4) == "VOX ") { "Invalid VOX header." }
        val reader = BinaryReader(data, true, 8)
        val models = mutableListOf<Model>()
        var size = intArrayOf(0, 0, 0)
        var palette = defaultPalette
        while (reader.offset <= data.size - 12) {
            val type = data.decodeToString(reader.offset, reader.offset + 4)
            reader.offset += 4
            val chunkSize = reader.int()
            val childSize = reader.int()
            require(chunkSize in 0..data.size - reader.offset && childSize in 0..data.size - reader.offset - chunkSize)
            val end = reader.offset + chunkSize
            when (type) {
                "SIZE" -> {
                    require(chunkSize >= 12)
                    size = IntArray(3) { reader.int().also { require(it in 1..256) } }
                }
                "XYZI" -> {
                    require(chunkSize >= 4 && size[0] > 0)
                    val count = reader.int()
                    require(count in 0..(chunkSize - 4) / 4)
                    val volume = ByteArray(size[0] * size[1] * size[2])
                    repeat(count) {
                        val x = reader.unsignedByte()
                        val y = reader.unsignedByte()
                        val z = reader.unsignedByte()
                        val color = reader.unsignedByte()
                        require(x < size[0] && y < size[1] && z < size[2])
                        volume[x + size[0] * (y + size[1] * z)] = color.toByte()
                    }
                    models.add(Model(size, volume))
                }
                "RGBA" -> {
                    require(chunkSize >= 1024)
                    palette = IntArray(256)
                    for (i in 1..255) {
                        val rgba = reader.int()
                        palette[i] = ((rgba and 255) shl 16) or (rgba and 0xff00) or ((rgba ushr 16) and 255)
                    }
                }
            }
            reader.offset = end
        }
        require(models.isNotEmpty()) { "The VOX file contains no models." }
        return models.map { mesh(it, palette) }.toTypedArray()
    }

    private fun mesh(model: Model, palette: IntArray): BufferGeometry {
        val (sx, sy, sz) = model.size
        val stride = intArrayOf(1, sx, sx * sy)
        val positions = mutableListOf<Float>()
        val colors = mutableListOf<Float>()
        val indices = mutableListOf<Int>()
        val color = Color()
        val point = IntArray(3)
        for (axis in 0..2) {
            val u = (axis + 1) % 3
            val v = (axis + 2) % 3
            val spanU = model.size[u]
            val spanV = model.size[v]
            val mask = IntArray(spanU * spanV)
            for (slice in 0..model.size[axis]) {
                for (row in 0 until spanV) {
                    for (column in 0 until spanU) {
                        val slot = slice * stride[axis] + column * stride[u] + row * stride[v]
                        val behind = if (slice > 0) model.volume[slot - stride[axis]].toInt() and 255 else 0
                        val front = if (slice < model.size[axis]) model.volume[slot].toInt() and 255 else 0
                        mask[column + row * spanU] = when {
                            front == 0 -> behind
                            behind == 0 -> -front
                            else -> 0
                        }
                    }
                }
                for (row in 0 until spanV) {
                    var column = 0
                    while (column < spanU) {
                        val slot = column + row * spanU
                        val face = mask[slot]
                        if (face == 0) {
                            column++
                            continue
                        }
                        var width = 1
                        while (column + width < spanU && mask[slot + width] == face) width++
                        var height = 1
                        while (row + height < spanV && (0 until width).all { mask[slot + height * spanU + it] == face }) height++
                        val start = positions.size / 3
                        color.setHex(palette[abs(face)])
                        point[axis] = slice
                        fun vertex(uu: Int, vv: Int) {
                            point[u] = uu
                            point[v] = vv
                            positions.add(point[0] - sx / 2f)
                            positions.add(point[2] - sz / 2f)
                            positions.add(sy / 2f - point[1])
                            colors.add(color.r.toFloat())
                            colors.add(color.g.toFloat())
                            colors.add(color.b.toFloat())
                        }
                        vertex(column, row)
                        if (face > 0) {
                            vertex(column + width, row)
                            vertex(column + width, row + height)
                            vertex(column, row + height)
                        } else {
                            vertex(column, row + height)
                            vertex(column + width, row + height)
                            vertex(column + width, row)
                        }
                        indices.addAll(listOf(start, start + 1, start + 2, start, start + 2, start + 3))
                        for (i in 0 until height) {
                            mask.fill(0, slot + i * spanU, slot + i * spanU + width)
                        }
                        column += width
                    }
                }
            }
        }
        return BufferGeometry()
            .setAttribute("position", Float32BufferAttribute(positions.toFloatArray(), 3))
            .setAttribute("color", Float32BufferAttribute(colors.toFloatArray(), 3))
            .setIndex(indices.toIntArray()).apply { computeVertexNormals() }
    }
}

private val defaultPalette = intArrayOf(
    0x000000, 0xffffff, 0xffffcc, 0xffff99, 0xffff66, 0xffff33, 0xffff00, 0xffccff,
    0xffcccc, 0xffcc99, 0xffcc66, 0xffcc33, 0xffcc00, 0xff99ff, 0xff99cc, 0xff9999,
    0xff9966, 0xff9933, 0xff9900, 0xff66ff, 0xff66cc, 0xff6699, 0xff6666, 0xff6633,
    0xff6600, 0xff33ff, 0xff33cc, 0xff3399, 0xff3366, 0xff3333, 0xff3300, 0xff00ff,
    0xff00cc, 0xff0099, 0xff0066, 0xff0033, 0xff0000, 0xccffff, 0xccffcc, 0xccff99,
    0xccff66, 0xccff33, 0xccff00, 0xccccff, 0xcccccc, 0xcccc99, 0xcccc66, 0xcccc33,
    0xcccc00, 0xcc99ff, 0xcc99cc, 0xcc9999, 0xcc9966, 0xcc9933, 0xcc9900, 0xcc66ff,
    0xcc66cc, 0xcc6699, 0xcc6666, 0xcc6633, 0xcc6600, 0xcc33ff, 0xcc33cc, 0xcc3399,
    0xcc3366, 0xcc3333, 0xcc3300, 0xcc00ff, 0xcc00cc, 0xcc0099, 0xcc0066, 0xcc0033,
    0xcc0000, 0x99ffff, 0x99ffcc, 0x99ff99, 0x99ff66, 0x99ff33, 0x99ff00, 0x99ccff,
    0x99cccc, 0x99cc99, 0x99cc66, 0x99cc33, 0x99cc00, 0x9999ff, 0x9999cc, 0x999999,
    0x999966, 0x999933, 0x999900, 0x9966ff, 0x9966cc, 0x996699, 0x996666, 0x996633,
    0x996600, 0x9933ff, 0x9933cc, 0x993399, 0x993366, 0x993333, 0x993300, 0x9900ff,
    0x9900cc, 0x990099, 0x990066, 0x990033, 0x990000, 0x66ffff, 0x66ffcc, 0x66ff99,
    0x66ff66, 0x66ff33, 0x66ff00, 0x66ccff, 0x66cccc, 0x66cc99, 0x66cc66, 0x66cc33,
    0x66cc00, 0x6699ff, 0x6699cc, 0x669999, 0x669966, 0x669933, 0x669900, 0x6666ff,
    0x6666cc, 0x666699, 0x666666, 0x666633, 0x666600, 0x6633ff, 0x6633cc, 0x663399,
    0x663366, 0x663333, 0x663300, 0x6600ff, 0x6600cc, 0x660099, 0x660066, 0x660033,
    0x660000, 0x33ffff, 0x33ffcc, 0x33ff99, 0x33ff66, 0x33ff33, 0x33ff00, 0x33ccff,
    0x33cccc, 0x33cc99, 0x33cc66, 0x33cc33, 0x33cc00, 0x3399ff, 0x3399cc, 0x339999,
    0x339966, 0x339933, 0x339900, 0x3366ff, 0x3366cc, 0x336699, 0x336666, 0x336633,
    0x336600, 0x3333ff, 0x3333cc, 0x333399, 0x333366, 0x333333, 0x333300, 0x3300ff,
    0x3300cc, 0x330099, 0x330066, 0x330033, 0x330000, 0x00ffff, 0x00ffcc, 0x00ff99,
    0x00ff66, 0x00ff33, 0x00ff00, 0x00ccff, 0x00cccc, 0x00cc99, 0x00cc66, 0x00cc33,
    0x00cc00, 0x0099ff, 0x0099cc, 0x009999, 0x009966, 0x009933, 0x009900, 0x0066ff,
    0x0066cc, 0x006699, 0x006666, 0x006633, 0x006600, 0x0033ff, 0x0033cc, 0x003399,
    0x003366, 0x003333, 0x003300, 0x0000ff, 0x0000cc, 0x000099, 0x000066, 0x000033,
    0xee0000, 0xdd0000, 0xbb0000, 0xaa0000, 0x880000, 0x770000, 0x550000, 0x440000,
    0x220000, 0x110000, 0x00ee00, 0x00dd00, 0x00bb00, 0x00aa00, 0x008800, 0x007700,
    0x005500, 0x004400, 0x002200, 0x001100, 0x0000ee, 0x0000dd, 0x0000bb, 0x0000aa,
    0x000088, 0x000077, 0x000055, 0x000044, 0x000022, 0x000011, 0xeeeeee, 0xdddddd,
    0xbbbbbb, 0xaaaaaa, 0x888888, 0x777777, 0x555555, 0x444444, 0x222222, 0x111111,
)
