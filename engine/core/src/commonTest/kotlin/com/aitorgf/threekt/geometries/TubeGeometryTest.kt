package com.aitorgf.threekt.geometries

import com.aitorgf.threekt.core.BufferGeometry
import com.aitorgf.threekt.core.Float32BufferAttribute
import com.aitorgf.threekt.curves.DecoratedTorusKnot5c
import com.aitorgf.threekt.curves.FigureEightPolynomialKnot
import com.aitorgf.threekt.curves.GrannyKnot
import com.aitorgf.threekt.curves.HeartCurve
import com.aitorgf.threekt.curves.VivianiCurve
import com.aitorgf.threekt.extras.core.Curve
import com.aitorgf.threekt.extras.core.Shape
import com.aitorgf.threekt.extras.curves.CatmullRomCurve3
import com.aitorgf.threekt.math.Vector3
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals

/** Compares tubes, Frenet frames, Catmull–Rom splines and path extrusion with Three.js 0.187.0. */
class TubeGeometryTest {
    private fun sum(geometry: BufferGeometry, name: String) =
        (geometry.getAttribute(name) as Float32BufferAttribute).array.sumOf { abs(it.toDouble()) }

    private fun assertTube(
        curve: Curve<Vector3>, segments: Int, radial: Int, closed: Boolean,
        count: Int, positions: Double, normals: Double, binormals: Double, length: Double,
    ) {
        val tube = TubeGeometry(curve, segments, 2.0, radial, closed)
        assertEquals(count, tube.getAttribute("position")!!.count)
        assertEquals(positions, sum(tube, "position"), 0.05)
        assertEquals(normals, sum(tube, "normal"), 0.01)
        assertEquals(binormals, tube.binormals.sumOf { abs(it.x) + abs(it.y) + abs(it.z) }, 1e-4)
        assertEquals(length, curve.getLength(), 1e-6)
    }

    @Test
    fun closedKnotTubesMatchThreeJs() {
        assertTube(GrannyKnot(), 100, 3, true, 404, 15102.405, 609.939, 150.796896, 397.699121)
        assertTube(HeartCurve(3.5), 100, 3, true, 404, 24005.396, 534.353, 133.572003, 357.542194)
        assertTube(DecoratedTorusKnot5c(), 300, 4, true, 1505, 94204.567, 2299.169, 452.697484, 1589.604452)
    }

    @Test
    fun openTubesMatchThreeJs() {
        assertTube(VivianiCurve(70.0), 200, 6, false, 1407, 144481.228, 2160.095, 293.021524, 534.760413)
        assertTube(FigureEightPolynomialKnot(), 50, 12, false, 663, 66045.472, 1009.573, 78.441525, 608.502188)
    }

    private val points = arrayOf(
        Vector3(0.0, 10.0, -10.0), Vector3(10.0, 0.0, -10.0), Vector3(20.0, 0.0, 0.0),
        Vector3(30.0, 0.0, 10.0), Vector3(30.0, 0.0, 20.0), Vector3(20.0, 0.0, 30.0),
    )

    @Test
    fun catmullRomTypesMatchThreeJs() {
        val expected = mapOf(
            CatmullRomCurve3.CurveType.Centripetal to listOf(1373.733623, 67.422098, 1335.521861, 115.620524),
            CatmullRomCurve3.CurveType.Chordal to listOf(1373.095170, 67.449767, 1332.512174, 119.498945),
            CatmullRomCurve3.CurveType.CatmullRom to listOf(1366.812500, 66.980164, 1336.325500, 115.544874),
        )
        for ((type, values) in expected) {
            for ((index, closed) in listOf(false, true).withIndex()) {
                val curve = CatmullRomCurve3(points, closed, type, 0.3)
                val sampled = curve.getPoints(40).sumOf { abs(it.x) + abs(it.y) + abs(it.z) }
                assertEquals(values[index * 2], sampled, 1e-6, "$type closed=$closed")
                assertEquals(values[index * 2 + 1], curve.getLength(), 1e-6, "$type closed=$closed")
            }
        }
    }

    @Test
    fun extrusionAlongASplineMatchesThreeJs() {
        val shape = Shape().apply {
            moveTo(0.0, 0.0); lineTo(0.0, 3.0); lineTo(2.0, 3.0); lineTo(2.0, 0.0); lineTo(0.0, 0.0)
        }
        val path = CatmullRomCurve3(arrayOf(
            Vector3(-60.0, -100.0, 60.0), Vector3(-60.0, 20.0, 60.0), Vector3(-60.0, 120.0, 60.0),
            Vector3(60.0, 20.0, -60.0), Vector3(60.0, -100.0, -60.0),
        ))
        val geometry = ExtrudeGeometry(arrayOf(shape), ExtrudeGeometry.Options().apply {
            steps = 100
            bevelEnabled = false
            extrudePath = path
        })
        assertEquals(2412, geometry.getAttribute("position")!!.count)
        assertEquals(396436.562, sum(geometry, "position"), 0.5)
        assertEquals(3036.894, sum(geometry, "normal"), 0.05)
    }
}
