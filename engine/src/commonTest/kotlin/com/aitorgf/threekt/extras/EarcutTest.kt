package com.aitorgf.threekt.extras

import com.aitorgf.threekt.math.Vector2
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals

class EarcutTest {
    @Test
    fun largePolygonWithAHoleUsesTheZOrderHashLikeThreeJs() {
        // More than 80 vertices switch earcut to its z-order hashed ear test.
        val outline = MutableList(120) { i ->
            val r = if (i % 2 == 1) 40.0 else 100.0
            val a = i / 120.0 * PI * 2
            Vector2(cos(a) * r, sin(a) * r)
        }
        val hole = MutableList(30) { i ->
            val a = -i / 30.0 * PI * 2
            Vector2(cos(a) * 10 + 5, sin(a) * 10)
        }
        val triangles = ShapeUtils.triangulateShape(outline, listOf(hole)).flatMap { it.toList() }
        assertEquals(450, triangles.size)
        assertEquals(31435, triangles.sum())
        assertEquals(listOf(60, 135, 136, 134, 135, 60, 61, 62, 63, 63, 64, 65), triangles.take(12))
    }

    @Test
    fun squareSplitsIntoTwoTriangles() {
        val triangles = Earcut.triangulate(doubleArrayOf(0.0, 0.0, 1.0, 0.0, 1.0, 1.0, 0.0, 1.0))
        assertEquals(6, triangles.size)
        assertEquals(setOf(0, 1, 2, 3), triangles.toSet())
    }
}
