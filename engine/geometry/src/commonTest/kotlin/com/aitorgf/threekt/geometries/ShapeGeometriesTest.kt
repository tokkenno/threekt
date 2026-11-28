package com.aitorgf.threekt.geometries

import com.aitorgf.threekt.core.BufferGeometry
import com.aitorgf.threekt.core.Float32BufferAttribute
import com.aitorgf.threekt.extras.core.Path
import com.aitorgf.threekt.extras.core.Shape
import com.aitorgf.threekt.math.Vector2
import kotlin.math.PI
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals

/** Compares shape sampling, triangulation and extrusion with Three.js 0.187.0 for the webgl_geometry_shapes shapes. */
class ShapeGeometriesTest {
    private fun sum(geometry: BufferGeometry, name: String) =
        (geometry.getAttribute(name) as Float32BufferAttribute).array.sumOf { abs(it.toDouble()) }

    private fun heart() = Shape().apply {
        moveTo(25.0, 25.0)
        bezierCurveTo(25.0, 25.0, 20.0, 0.0, 0.0, 0.0)
        bezierCurveTo(-30.0, 0.0, -30.0, 35.0, -30.0, 35.0)
        bezierCurveTo(-30.0, 55.0, -10.0, 77.0, 25.0, 95.0)
        bezierCurveTo(60.0, 77.0, 80.0, 55.0, 80.0, 35.0)
        bezierCurveTo(80.0, 35.0, 80.0, 0.0, 50.0, 0.0)
        bezierCurveTo(35.0, 0.0, 25.0, 25.0, 25.0, 25.0)
    }

    private fun smiley() = Shape().apply {
        moveTo(80.0, 40.0)
        absarc(40.0, 40.0, 40.0, 0.0, PI * 2, false)
        holes.add(Path().moveTo(35.0, 20.0).absellipse(25.0, 20.0, 10.0, 10.0, 0.0, PI * 2, true))
        holes.add(Path().moveTo(65.0, 20.0).absarc(55.0, 20.0, 10.0, 0.0, PI * 2, true))
        holes.add(
            Path().moveTo(20.0, 40.0).quadraticCurveTo(40.0, 60.0, 60.0, 40.0).bezierCurveTo(70.0, 45.0, 70.0, 50.0, 60.0, 60.0)
                .quadraticCurveTo(40.0, 80.0, 20.0, 60.0).quadraticCurveTo(5.0, 50.0, 20.0, 40.0),
        )
    }

    private fun spline() = Shape().apply {
        moveTo(0.0, 0.0)
        splineThru(arrayOf(Vector2(70.0, 20.0), Vector2(80.0, 90.0), Vector2(-30.0, 70.0), Vector2(0.0, 0.0)))
    }

    private fun track() = Shape().apply {
        moveTo(40.0, 40.0)
        lineTo(40.0, 160.0)
        absarc(60.0, 160.0, 20.0, PI, 0.0, true)
        lineTo(80.0, 40.0)
        absarc(60.0, 40.0, 20.0, 2 * PI, PI, true)
    }

    private val options = ExtrudeGeometry.Options().apply {
        depth = 8.0
        bevelSegments = 2
        steps = 2
        bevelSize = 1.0
        bevelThickness = 1.0
    }

    private fun assertShape(
        shape: Shape, count: Int, indexCount: Int, indexSum: Int, positionSum: Double,
        extrudeCount: Int, extrudePositions: Double, extrudeNormals: Double, extrudeUvs: Double,
        pointCount: Int, pointSum: Double, spacedSum: Double,
    ) {
        val points = shape.getPoints()
        assertEquals(pointCount, points.size)
        assertEquals(pointSum, points.sumOf { abs(it.x) + abs(it.y) }, 1e-3)
        val spaced = shape.getSpacedPoints(50)
        assertEquals(51, spaced.size)
        assertEquals(spacedSum, spaced.sumOf { abs(it.x) + abs(it.y) }, 1e-3)

        val flat = ShapeGeometry(arrayOf(shape))
        assertEquals(count, flat.getAttribute("position")!!.count)
        assertEquals(indexCount, flat.index!!.count)
        assertEquals(indexSum, (0 until flat.index!!.count).sumOf { flat.index!!.getX(it).toInt() })
        assertEquals(positionSum, sum(flat, "position"), 1e-2)

        val extruded = ExtrudeGeometry(arrayOf(shape), options)
        assertEquals(extrudeCount, extruded.getAttribute("position")!!.count)
        assertEquals(extrudePositions, sum(extruded, "position"), 0.5)
        assertEquals(extrudeNormals, sum(extruded, "normal"), 0.05)
        assertEquals(extrudeUvs, sum(extruded, "uv"), 0.5)
    }

    @Test
    fun bezierHeartMatchesThreeJs() = assertShape(heart(), 72, 210, 7188, 4993.062, 3012, 223230.656, 4139.180, 118944.429, 73, 5043.062, 3874.247)

    @Test
    fun smileyWithHolesMatchesThreeJs() = assertShape(smiley(), 123, 381, 20254, 9523.993, 5064, 413925.597, 6916.823, 237083.385, 25, 2040.0, 4120.0)

    @Test
    fun splineShapeMatchesThreeJs() = assertShape(spline(), 60, 174, 5225, 5338.815, 2508, 235376.741, 3363.482, 126989.771, 61, 5338.815, 4596.492)

    @Test
    fun trackWithArcsMatchesThreeJs() = assertShape(track(), 51, 147, 3790, 8120.0, 2088, 342943.418, 2820.105, 198643.418, 52, 8200.0, 8080.0)

    @Test
    fun extrusionWithoutBevelHasOnlyCapsAndOneSideLayer() {
        val geometry = ExtrudeGeometry(arrayOf(heart()), ExtrudeGeometry.Options().apply { depth = 5.0; bevelEnabled = false })
        assertEquals(852, geometry.getAttribute("position")!!.count)
    }
}
