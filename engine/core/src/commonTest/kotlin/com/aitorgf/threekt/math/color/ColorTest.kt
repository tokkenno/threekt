package com.aitorgf.threekt.math.color

import com.aitorgf.threekt.core.Float32BufferAttribute
import com.aitorgf.threekt.math.Color
import com.aitorgf.threekt.math.Matrix3
import com.aitorgf.threekt.math.Vector3
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertSame

class ColorTest {
    @Test
    fun allCssKeywordsRoundTripThroughTheirPackedRgbCodes() {
        assertEquals(148, CssColorNames.entries.size)
        assertEquals(148, Color.NAMES.size)
        for (entry in CssColorNames.entries) {
            assertEquals(entry.code.toInt(), Color.NAMES[entry.cssName])
            assertEquals(entry.code.toInt(), entry.toColor().getHex(), entry.cssName)
            assertEquals(entry.toColor(), Color(entry.cssName.uppercase()), entry.cssName)
        }
        assertEquals("rebeccapurple", CssColorNames.REBECCA_PURPLE.cssName)
        assertEquals(Color("gray"), Color("grey"))
    }

    @Test
    fun colorParsesCssPercentagesAndRetainsItsValueOnUnknownInput() {
        assertEquals(0xff0080, Color("rgba(100%, 0%, 50%, 0.1)").getHex())
        assertEquals(0x00ff00, Color("hsla(120, 100%, 50%, 0.2)").getHex())
        val color = Color(0.125, 0.25, 0.5)
        val original = color.copy()
        assertSame(color, color.setStyle("not-a-color"))
        assertEquals(original, color)
        assertEquals(original, color.setStyle("#f00f"))
    }

    @Test
    fun colorRgbAndHslTargetsReconstructTheOriginalWorkingColor() {
        val color = Color(0.75, 0.5, 0.25)
        val rgb = RGB()
        val hsl = HSL()
        assertSame(rgb, color.getRGB(rgb))
        assertSame(hsl, color.getHSL(hsl))
        assertEquals(1.0 / 12.0, hsl.h, 1e-12)
        assertEquals(0.5, hsl.s, 1e-12)
        assertEquals(0.5, hsl.l, 1e-12)
        val restored = hsl.toColor()
        assertEquals(color.r, restored.r, 1e-12)
        assertEquals(color.g, restored.g, 1e-12)
        assertEquals(color.b, restored.b, 1e-12)
        assertEquals(color, rgb.toColor())
        assertEquals(0x808080, Color(0.5, 0.5, 0.5).getHex(ColorSpace.LinearSRGB))
        assertEquals(HSL(0.0, 0.0, 0.5), Color(0.5, 0.5, 0.5).getHSL(HSL()))
    }

    @Test
    fun colorArithmeticClampsSubtractionAndSupportsRgbAndHslInterpolation() {
        val first = Color(0.125, 0.25, 0.5)
        val second = Color(0.25, 0.125, 0.5)
        assertEquals(Color(0.375, 0.375, 1.0), first.copy().add(second))
        assertEquals(Color(0.0, 0.125, 0.0), first.copy().sub(second))
        assertEquals(Color(0.03125, 0.03125, 0.25), first.copy().multiply(second))
        assertEquals(Color(0.1875, 0.1875, 0.5), first.copy().lerp(second, 0.5))
        assertEquals(Color(0.375, 0.625, 1.125), first.copy().multiplyScalar(2.0).addScalar(0.125))

        val red = Color().setHSL(0.0, 1.0, 0.5)
        val green = Color().setHSL(1.0 / 3.0, 1.0, 0.5)
        assertEquals(0xffff00, red.copy().lerpHSL(green, 0.5).getHex())
        assertEquals(0x00ff00, red.copy().offsetHSL(1.0 / 3.0, 0.0, 0.0).getHex())
    }

    @Test
    fun colorBuffersAndMatrixTransformsUseRgbComponentOrder() {
        val attribute = Float32BufferAttribute(floatArrayOf(0f, 0f, 0f, 99f, 0.25f, 0.5f, 0.75f, 99f), 4)
        val color = Color().fromBufferAttribute(attribute, 1)
        assertEquals(Color(0.25, 0.5, 0.75), color)
        val swapRedBlue = Matrix3(0.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 0.0)
        assertEquals(Color(0.75, 0.5, 0.25), color.applyMatrix3(swapRedBlue))

        val output = DoubleArray(5) { -1.0 }
        assertSame(output, color.toArray(output, 1))
        assertContentEquals(doubleArrayOf(-1.0, 0.75, 0.5, 0.25, -1.0), output)
        assertEquals(color, Color().fromArray(output, 1))
        assertEquals(color, Color().setFromVector3(Vector3(0.75, 0.5, 0.25)))
    }

    @Test
    fun srgbTransferHandlesItsLinearAndPowerCurveSegments() {
        assertEquals(0.0, SRGBEncoder.decode(0.0))
        assertEquals(0.0, SRGBEncoder.encode(0.0))
        assertEquals(0.000773993808, SRGBEncoder.decode(0.01), 1e-12)
        assertEquals(0.01292, SRGBEncoder.encode(0.001), 1e-12)
        assertEquals(0.21404114048223255, SRGBEncoder.decode(0.5), 2e-10)
        assertEquals(0.5, SRGBEncoder.encode(SRGBEncoder.decode(0.5)), 1e-5)
        assertEquals(-0.1292, SRGBEncoder.encode(-0.01), 1e-12)
    }
}
