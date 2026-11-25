package com.aitorgf.threekt.loaders

import com.aitorgf.threekt.core.BufferGeometry
import com.aitorgf.threekt.core.Float32BufferAttribute
import com.aitorgf.threekt.materials.LineBasicMaterial
import com.aitorgf.threekt.materials.MeshPhongMaterial
import com.aitorgf.threekt.materials.PointsMaterial
import com.aitorgf.threekt.math.Color
import com.aitorgf.threekt.math.Vector3
import com.aitorgf.threekt.math.color.ColorSpace
import com.aitorgf.threekt.objects.Group
import com.aitorgf.threekt.objects.LineSegments
import com.aitorgf.threekt.objects.Mesh
import com.aitorgf.threekt.objects.Points
import kotlin.js.JsExport

private val whitespace = Regex("\\s+")
private val continuation = Regex("\\\\\\r?\\n")

/**
 * Parses Wavefront OBJ text into a [Group]. Supports polygon fans, polylines, points, normals, UVs,
 * vertex colors and relative indices. Threekt receives the text from the caller and uses default
 * materials for OBJ files, including those that reference an MTL library.
 */
@JsExport
class OBJLoader {
    /** Parses [text]. Malformed numeric data or out-of-range indices throw [IllegalArgumentException]. */
    fun parse(text: String): Group {
        val vertices = mutableListOf<Vector3>()
        val normals = mutableListOf<Vector3>()
        val uvs = mutableListOf<Pair<Float, Float>>()
        val colors = mutableListOf<Color?>()
        val result = Group()
        var name = ""
        var smooth = true
        val batches = linkedMapOf<String, Batch>()
        val ab = Vector3()
        val ac = Vector3()

        fun flush() {
            for ((primitive, batch) in batches) {
                val geometry = BufferGeometry().setAttribute("position", Float32BufferAttribute(batch.positions.toFloatArray(), 3))
                if (batch.normals.isNotEmpty()) geometry.setAttribute("normal", Float32BufferAttribute(batch.normals.toFloatArray(), 3))
                if (batch.hasUvs) geometry.setAttribute("uv", Float32BufferAttribute(batch.uvs.toFloatArray(), 2))
                if (batch.hasColors) geometry.setAttribute("color", Float32BufferAttribute(batch.colors.toFloatArray(), 3))
                val child = when (primitive) {
                    "f" -> Mesh(geometry, MeshPhongMaterial().apply { flatShading = !smooth; vertexColors = batch.hasColors })
                    "l" -> LineSegments(geometry, LineBasicMaterial().apply { vertexColors = batch.hasColors })
                    else -> Points(geometry, PointsMaterial().apply { vertexColors = batch.hasColors; sizeAttenuation = false })
                }
                child.name = name
                result.add(child)
            }
            batches.clear()
        }

        fun reference(token: String): Reference {
            val indices = token.split('/')
            return Reference(
                resolveIndex(indices[0], vertices.size),
                indices.getOrNull(1)?.takeIf { it.isNotEmpty() }?.let { resolveIndex(it, uvs.size) },
                indices.getOrNull(2)?.takeIf { it.isNotEmpty() }?.let { resolveIndex(it, normals.size) },
            )
        }

        fun append(batch: Batch, ref: Reference, fallbackNormal: Vector3? = null) {
            batch.positions.append(vertices[ref.position])
            val color = colors[ref.position]
            batch.hasColors = batch.hasColors || color != null
            batch.colors.add((color?.r ?: 1.0).toFloat())
            batch.colors.add((color?.g ?: 1.0).toFloat())
            batch.colors.add((color?.b ?: 1.0).toFloat())
            val uv = ref.uv?.let(uvs::get)
            batch.hasUvs = batch.hasUvs || uv != null
            batch.uvs.add(uv?.first ?: 0f)
            batch.uvs.add(uv?.second ?: 0f)
            if (fallbackNormal != null) batch.normals.append(ref.normal?.let(normals::get) ?: fallbackNormal)
        }

        val source = text.replace(continuation, "")
        for (rawLine in source.lineSequence()) {
            val line = rawLine.substringBefore('#').trim()
            if (line.isEmpty()) continue
            val fields = line.split(whitespace)
            when (val command = fields[0]) {
                "v", "vn" -> {
                    require(fields.size >= 4) { "Invalid OBJ vertex: $line" }
                    val vector = Vector3(fields[1].toDouble(), fields[2].toDouble(), fields[3].toDouble())
                    if (command == "vn") {
                        normals.add(vector)
                    } else {
                        vertices.add(vector)
                        colors.add(if (fields.size >= 7) Color().setRGB(
                            fields[4].toDouble(), fields[5].toDouble(), fields[6].toDouble(), ColorSpace.SRGB,
                        ) else null)
                    }
                }
                "vt" -> {
                    require(fields.size >= 2) { "Invalid OBJ texture coordinate: $line" }
                    uvs.add(fields[1].toFloat() to (fields.getOrNull(2)?.toFloat() ?: 0f))
                }
                "o", "g" -> {
                    flush()
                    name = fields.drop(1).joinToString(" ")
                }
                "usemtl" -> flush()
                "s" -> {
                    val value = fields.getOrNull(1)?.lowercase()
                    val nextSmooth = value != "0" && value != "off"
                    if (nextSmooth != smooth) flush()
                    smooth = nextSmooth
                }
                "f", "l", "p" -> {
                    val refs = fields.drop(1).map(::reference)
                    val minimum = when (command) { "f" -> 3; "l" -> 2; else -> 1 }
                    require(refs.size >= minimum) { "Not enough indices for OBJ primitive: $line" }
                    val batch = batches.getOrPut(command) { Batch() }
                    when (command) {
                        "f" -> for (i in 1 until refs.lastIndex) {
                            val a = refs[0]
                            val b = refs[i]
                            val c = refs[i + 1]
                            ab.subVectors(vertices[b.position], vertices[a.position])
                            ac.subVectors(vertices[c.position], vertices[a.position])
                            val normal = ab.cross(ac).normalize()
                            append(batch, a, normal)
                            append(batch, b, normal)
                            append(batch, c, normal)
                        }
                        "l" -> for (i in 0 until refs.lastIndex) {
                            append(batch, refs[i])
                            append(batch, refs[i + 1])
                        }
                        else -> for (ref in refs) append(batch, ref)
                    }
                }
            }
        }
        if (result.children.isEmpty() && batches.isEmpty() && vertices.isNotEmpty()) {
            val batch = Batch()
            for (i in vertices.indices) append(batch, Reference(i))
            batches["p"] = batch
        }
        flush()
        return result
    }

    private fun resolveIndex(value: String, size: Int): Int {
        val index = value.toInt()
        val resolved = if (index < 0) size + index else index - 1
        require(resolved in 0 until size) { "OBJ index $index is outside its list of $size entries." }
        return resolved
    }

    private fun MutableList<Float>.append(vector: Vector3) {
        add(vector.x.toFloat())
        add(vector.y.toFloat())
        add(vector.z.toFloat())
    }

    private class Reference(val position: Int, val uv: Int? = null, val normal: Int? = null)

    private class Batch {
        val positions = mutableListOf<Float>()
        val normals = mutableListOf<Float>()
        val uvs = mutableListOf<Float>()
        val colors = mutableListOf<Float>()
        var hasUvs = false
        var hasColors = false
    }
}
