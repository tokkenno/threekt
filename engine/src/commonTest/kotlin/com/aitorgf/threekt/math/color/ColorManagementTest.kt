package com.aitorgf.threekt.math.color

import com.aitorgf.threekt.math.Color
import com.aitorgf.threekt.math.Matrix3
import com.aitorgf.threekt.math.Vector3
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class ColorManagementTest {
    @Test
    fun registryExposesStandardSrgbTransfersAndMetadata() {
        assertEquals(ColorTransfer.Linear, ColorManagement.getTransfer(ColorSpace.None))
        assertEquals(ColorTransfer.Linear, ColorManagement.getTransfer(ColorSpace.LinearSRGB))
        assertEquals(ColorTransfer.SRGB, ColorManagement.getTransfer(ColorSpace.SRGB))
        assertEquals(ColorSpace.SRGB, ColorManagement.getDrawingBufferColorSpace(ColorSpace.LinearSRGB))
        assertEquals(ColorSpace.SRGB, ColorManagement.getUnpackColorSpace(ColorSpace.LinearSRGB))
        assertNull(ColorManagement.getUnpackColorSpace(ColorSpace.SRGB))
        assertEquals("standard", ColorManagement.getToneMappingMode(ColorSpace.SRGB))
        val coefficients = Vector3()
        assertSame(coefficients, ColorManagement.getLuminanceCoefficients(coefficients, ColorSpace.LinearSRGB))
        assertEquals(Vector3(0.2126, 0.7152, 0.0722), coefficients)
    }

    @Test
    fun conversionSkipsIdentityNoneAndDisabledColorManagement() {
        val original = Color(0.5, 0.25, 0.75)
        val color = original.copy()
        val wasEnabled = ColorManagement.enabled
        try {
            ColorManagement.enabled = true
            assertSame(color, ColorManagement.convert(color, ColorSpace.SRGB, ColorSpace.SRGB))
            ColorManagement.convert(color, ColorSpace.None, ColorSpace.LinearSRGB)
            ColorManagement.convert(color, ColorSpace.SRGB, ColorSpace.None)
            assertEquals(original, color)

            ColorManagement.enabled = false
            ColorManagement.convert(color, ColorSpace.SRGB, ColorSpace.LinearSRGB)
            assertEquals(original, color)

            ColorManagement.enabled = true
            ColorManagement.convert(color, ColorSpace.SRGB, ColorSpace.LinearSRGB)
            assertEquals(SRGBEncoder.decode(original.r), color.r, 1e-12)
            ColorManagement.convert(color, ColorSpace.LinearSRGB, ColorSpace.SRGB)
            assertEquals(original.r, color.r, 1e-5)
            assertEquals(original.g, color.g, 1e-5)
            assertEquals(original.b, color.b, 1e-5)
        } finally {
            ColorManagement.enabled = wasEnabled
        }
    }

    @Test
    fun registeredDefinitionsConvertThroughXyzWhenPrimariesDiffer() {
        val source = ColorManagement.spaces.getValue(ColorSpace.LinearSRGB)
        val target = ColorManagement.spaces.getValue(ColorSpace.SRGB)
        val toXYZ = Matrix3().makeScale(2.0, 3.0)
        val fromXYZ = Matrix3().makeScale(5.0, 7.0)
        val wasEnabled = ColorManagement.enabled
        try {
            ColorManagement.enabled = true
            val sourceDefinition = source.copy(toXYZ = toXYZ)
            val targetDefinition = target.copy(
                primaries = target.primaries.copyOf(),
                transfer = ColorTransfer.Linear,
                fromXYZ = fromXYZ,
                toneMappingMode = "extended",
            )
            ColorManagement.define(mapOf(ColorSpace.LinearSRGB to sourceDefinition, ColorSpace.SRGB to targetDefinition))
            assertSame(targetDefinition.primaries, ColorManagement.getPrimaries(ColorSpace.SRGB))
            assertEquals("extended", ColorManagement.getToneMappingMode(ColorSpace.SRGB))
            val color = Color(1.0, 2.0, 3.0)
            assertSame(color, ColorManagement.convert(color, ColorSpace.LinearSRGB, ColorSpace.SRGB))
            assertEquals(Color(10.0, 42.0, 3.0), color)
            val matrix = ColorManagement.getMatrix(Matrix3(), ColorSpace.LinearSRGB, ColorSpace.SRGB)
            assertEquals(Matrix3().makeScale(10.0, 21.0), matrix)
        } finally {
            ColorManagement.define(mapOf(ColorSpace.LinearSRGB to source, ColorSpace.SRGB to target))
            ColorManagement.enabled = wasEnabled
        }
    }
}
