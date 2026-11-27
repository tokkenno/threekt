package com.aitorgf.threekt.loaders

import com.aitorgf.threekt.core.BufferGeometry
import com.aitorgf.threekt.core.Float32BufferAttribute
import com.aitorgf.threekt.materials.LineBasicMaterial
import com.aitorgf.threekt.math.Color
import com.aitorgf.threekt.objects.Group
import com.aitorgf.threekt.objects.LineSegments
import kotlin.js.JsExport
import kotlin.math.PI

private val gcodeWhitespace = Regex("\\s+")

/** Parses a G-code toolpath supplied as text. Like Three.js, it visualizes linear moves and skips arcs. */
@JsExport
class GCodeLoader {
    /** Emit two line objects per layer instead of combining all travel and extrusion paths. */
    var splitLayer = false

    private class Layer(val z: Double) {
        val extruded = mutableListOf<Float>()
        val travel = mutableListOf<Float>()
    }

    /** Returns green extrusion lines and red travel lines in a group rotated from Z-up to Y-up. */
    fun parse(text: String): Group {
        val layers = mutableListOf<Layer>()
        var layer: Layer? = null
        var x = 0.0
        var y = 0.0
        var z = 0.0
        var extrusion = 0.0
        var relative = false
        var extrusionRelative: Boolean? = null

        for (row in text.lineSequence()) {
            val line = row.substringBefore(';').trim()
            if (line.isEmpty()) continue
            val tokens = line.split(gcodeWhitespace)
            val command = tokens[0].uppercase()
            when (command) {
                "G90", "G91" -> {
                    relative = command == "G91"
                    extrusionRelative = null
                }
                "M82", "M83" -> extrusionRelative = command == "M83"
                "G0", "G1", "G92" -> {
                    val arguments = tokens.drop(1).associate { it.first().uppercaseChar() to it.drop(1).toDoubleOrNull() }
                    val reset = command == "G92"
                    fun coordinate(axis: Char, previous: Double, relativeAxis: Boolean): Double {
                        val value = arguments[axis] ?: return previous
                        return if (relativeAxis && !reset) previous + value else value
                    }
                    val nextX = coordinate('X', x, relative)
                    val nextY = coordinate('Y', y, relative)
                    val nextZ = coordinate('Z', z, relative)
                    val nextExtrusion = coordinate('E', extrusion, extrusionRelative ?: relative)
                    if (!reset) {
                        val depositing = nextExtrusion > extrusion
                        if (layer == null || (depositing && nextZ != layer.z)) {
                            layer = Layer(if (depositing) nextZ else z)
                            layers.add(layer)
                        }
                        val vertices = if (depositing) layer.extruded else layer.travel
                        vertices.add(x.toFloat())
                        vertices.add(y.toFloat())
                        vertices.add(z.toFloat())
                        vertices.add(nextX.toFloat())
                        vertices.add(nextY.toFloat())
                        vertices.add(nextZ.toFloat())
                    }
                    x = nextX
                    y = nextY
                    z = nextZ
                    extrusion = nextExtrusion
                }
            }
        }

        val result = Group()
        result.name = "gcode"
        val extruded = LineBasicMaterial(Color().setHex(0x00ff00))
        val travel = LineBasicMaterial(Color().setHex(0xff0000))
        fun addLines(vertices: List<Float>, material: LineBasicMaterial, index: Int) {
            val geometry = BufferGeometry()
            geometry.setAttribute("position", Float32BufferAttribute(vertices.toFloatArray(), 3))
            result.add(LineSegments(geometry, material).apply { name = "layer$index" })
        }
        if (splitLayer) {
            layers.forEachIndexed { index, current ->
                addLines(current.extruded, extruded, index)
                addLines(current.travel, travel, index)
            }
        } else {
            addLines(layers.flatMap { it.extruded }, extruded, layers.size)
            addLines(layers.flatMap { it.travel }, travel, layers.size)
        }
        result.rotation.x = -PI / 2
        return result
    }
}
