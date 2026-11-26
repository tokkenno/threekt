package com.aitorgf.threekt.loaders

import com.aitorgf.threekt.core.BufferGeometry
import com.aitorgf.threekt.core.Float32BufferAttribute
import com.aitorgf.threekt.geometries.ShapeGeometry
import com.aitorgf.threekt.geometries.TextGeometry
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals

/** Compares text shapes, ShapePath hole detection and text geometry with Three.js 0.187.0. */
class FontLoaderTest {
    /** Helvetiker regular with only the glyphs o, i, T and ?. */
    private val font = FontLoader().parse("""{"resolution":1000,"ascender":1189,"underlineThickness":50,"boundingBox":{"yMin":-334,"xMin":-111,"yMax":1189,"xMax":1672},"familyName":"Helvetiker","glyphs":{"o":{"ha":821,"o":"m 357 -25 q 94 91 194 -25 q 0 368 0 202 q 93 642 0 533 q 357 761 193 761 q 618 644 518 761 q 713 368 713 533 q 619 91 713 201 q 357 -25 521 -25 m 357 85 q 528 175 465 85 q 584 369 584 255 q 529 562 584 484 q 357 651 467 651 q 189 560 250 651 q 135 369 135 481 q 187 177 135 257 q 357 85 250 85 "},"i":{"ha":275,"o":"m 136 873 l 14 873 l 14 1013 l 136 1013 l 136 873 m 136 0 l 14 0 l 14 737 l 136 737 l 136 0 "},"T":{"ha":835,"o":"m 777 894 l 458 894 l 458 0 l 319 0 l 319 894 l 0 894 l 0 1013 l 777 1013 l 777 894 "},"?":{"ha":704,"o":"m 607 777 q 543 599 607 674 q 422 474 482 537 q 357 272 357 391 l 236 272 q 297 487 236 395 q 411 619 298 490 q 474 762 474 691 q 422 885 474 838 q 301 933 371 933 q 179 880 228 933 q 124 706 124 819 l 0 706 q 94 963 0 872 q 302 1044 177 1044 q 511 973 423 1044 q 607 777 607 895 m 370 0 l 230 0 l 230 151 l 370 151 l 370 0 "}}}""")

    private fun sum(geometry: BufferGeometry, name: String) =
        (geometry.getAttribute(name) as Float32BufferAttribute).array.sumOf { abs(it.toDouble()) }

    @Test
    fun shapesFindHolesAndBreakLinesLikeThreeJs() {
        // 'x' is missing and falls back to '?'.
        val shapes = font.generateShapes("To\nio x", 100.0)
        assertEquals(9, shapes.size)
        assertEquals(listOf(0, 1, 0, 0, 1, 0, 0, 0, 0), shapes.map { it.holes.size })
        assertEquals(128222.2361, shapes.sumOf { shape -> shape.getPoints().sumOf { abs(it.x) + abs(it.y) } }, 1e-3)

        val flat = ShapeGeometry(shapes)
        assertEquals(748, flat.getAttribute("position")!!.count)
        assertEquals(2202, flat.index!!.size)
        assertEquals(158845.914, sum(flat, "position"), 0.05)
        assertEquals(
            listOf(0 to 18, 18 to 576, 594 to 6, 600 to 6, 606 to 576, 1182 to 504, 1686 to 6, 1692 to 504, 2196 to 6),
            flat.groups.map { it.start to it.count },
        )
        assertEquals((0..8).toList(), flat.groups.map { it.materialIndex })
    }

    @Test
    fun beveledTextGeometryMatchesThreeJs() {
        val text = TextGeometry("Toi", TextGeometry.Parameters().apply {
            font = this@FontLoaderTest.font
            size = 70.0
            depth = 20.0
            curveSegments = 4
            bevelThickness = 2.0
            bevelSize = 1.5
            bevelEnabled = true
        })
        assertEquals(3804, text.getAttribute("position")!!.count)
        assertEquals(468048.454, sum(text, "position"), 0.5)
        assertEquals(5153.257, sum(text, "normal"), 0.05)
        assertEquals(
            listOf(Triple(0, 36, 0), Triple(36, 336, 1), Triple(372, 384, 0), Triple(756, 2688, 1),
                Triple(3444, 12, 0), Triple(3456, 168, 1), Triple(3624, 12, 0), Triple(3636, 168, 1)),
            text.groups.map { Triple(it.start, it.count, it.materialIndex) },
        )
    }

    @Test
    fun topToBottomTextStacksGlyphs() {
        val shapes = font.generateShapes("oi", 50.0, Font.Direction.Tb)
        assertEquals(9607.7972, shapes.sumOf { shape -> shape.getPoints().sumOf { abs(it.x) + abs(it.y) } }, 1e-3)
    }
}
