package com.aitorgf.threekt.core

import com.aitorgf.threekt.math.Color
import com.aitorgf.threekt.extras.DataUtils
import kotlin.test.assertIs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BufferAttributeTest {
    @Test
    fun uint8AttributesReadBytesAsUnsignedAndNormalizeOnRequest() {
        val bytes = byteArrayOf(0, 127, -128, -1)
        val attribute = Uint8BufferAttribute(bytes, 4)
        assertEquals(1, attribute.count)
        assertEquals(255.0, attribute.getW(0))
        assertEquals(128.0, attribute.getZ(0))
        attribute.normalized = true
        assertEquals(1.0, attribute.getW(0))
        assertEquals(128.0 / 255.0, attribute.getZ(0))
        assertEquals(Color(0.0, 127.0 / 255.0, 128.0 / 255.0), Color().fromBufferAttribute(attribute, 0))
    }

    @Test
    fun attributesRequireWholeItemsAndCountUpdates() {
        assertFailsWith<IllegalArgumentException> { Float32BufferAttribute(FloatArray(4), 3) }
        assertFailsWith<IllegalArgumentException> { Uint8BufferAttribute(ByteArray(3), 0) }
        val attribute = Float32BufferAttribute(floatArrayOf(1f, 2f, 3f, 4f, 5f, 6f), 3)
        assertEquals(2, attribute.count)
        assertEquals(5.0, attribute.getY(1))
        assertEquals(0, attribute.version)
        attribute.markUpdated()
        assertEquals(1, attribute.version)
    }

    @Test
    fun geometryStoresAttributesOfAnyType() {
        val geometry = BufferGeometry()
            .setAttribute("position", Float32BufferAttribute(FloatArray(9), 3))
            .setAttribute("color", Uint8BufferAttribute(ByteArray(12), 4, normalized = true))
        assertEquals(true, geometry.hasAttribute("color"))
        assertEquals(3, geometry.getAttribute("color")?.count)
        geometry.deleteAttribute("color")
        assertEquals(false, geometry.hasAttribute("color"))
    }

    @Test
    fun int16AttributesNormalizeToSignedUnitRange() {
        val attribute = Int16BufferAttribute(shortArrayOf(32767, -32767, -32768), 1, normalized = true)
        assertEquals(1.0, attribute.getX(0))
        assertEquals(-1.0, attribute.getX(1))
        assertEquals(-1.0, attribute.getX(2))
        attribute.normalized = false
        assertEquals(-32768.0, attribute.getX(2))
    }

    @Test
    fun integerArraysConvertWrittenValuesAsJavaScriptTypedArrays() {
        val int8 = Int8BufferAttribute(ByteArray(3), 1)
        int8.setX(0, 130.0).setX(1, -1.7).setX(2, Double.NaN)
        assertEquals(listOf(-126.0, -1.0, 0.0), List(3) { int8.getX(it) })

        val clamped = Uint8ClampedBufferAttribute(ByteArray(4), 1)
        clamped.setX(0, 300.0).setX(1, -5.0).setX(2, 2.5).setX(3, 3.5)
        assertEquals(listOf(255.0, 0.0, 2.0, 4.0), List(4) { clamped.getX(it) })

        val uint16 = Uint16BufferAttribute(ShortArray(1), 1)
        uint16.setX(0, 65535.0)
        assertEquals(65535.0, uint16.getX(0))
        val uint32 = Uint32BufferAttribute(IntArray(1), 1)
        uint32.setX(0, 4294967295.0)
        assertEquals(4294967295.0, uint32.getX(0))
    }

    @Test
    fun normalizedIntegersUseTheScaleOfTheirArrayType() {
        val uint16 = Uint16BufferAttribute(ShortArray(1), 1, normalized = true)
        uint16.setX(0, 0.5)
        assertEquals(32768.0 / 65535.0, uint16.getX(0), 1e-12)
        val int8 = Int8BufferAttribute(byteArrayOf(-128), 1, normalized = true)
        assertEquals(-1.0, int8.getX(0))
        val int32 = Int32BufferAttribute(IntArray(1), 1, normalized = true)
        int32.setX(0, -0.25)
        assertEquals(-0.25, int32.getX(0), 1e-9)
    }

    @Test
    fun halfFloatsRoundTripThroughDataUtils() {
        assertEquals(0x3c00, DataUtils.toHalfFloat(1.0))
        assertEquals(0xc000, DataUtils.toHalfFloat(-2.0))
        assertEquals(0x7bff, DataUtils.toHalfFloat(1e9))
        assertEquals(0.5, DataUtils.fromHalfFloat(0x3800))
        val half = Float16BufferAttribute(ShortArray(3), 3)
        half.setXYZ(0, 1.5, -0.25, 1000.0)
        assertEquals(1.5, half.getX(0))
        assertEquals(-0.25, half.getY(0))
        assertEquals(1000.0, half.getZ(0))
    }

    @Test
    fun copyAtCopiesRawComponentsAndCopyRequiresTheSameType() {
        val source = Float32BufferAttribute(floatArrayOf(1f, 2f, 3f, 4f), 2)
        val target = Float32BufferAttribute(FloatArray(4), 2)
        target.copyAt(1, source, 0)
        assertEquals(listOf(0f, 0f, 1f, 2f), target.array.toList())
        val clone = source.clone()
        source.array[0] = 9f
        assertEquals(1f, clone.array[0])
        assertFailsWith<IllegalArgumentException> { Uint16BufferAttribute(ShortArray(2), 1).copy(source) }
        source.addUpdateRange(0, 2)
        assertEquals(1, source.updateRanges.size)
        source.clearUpdateRanges()
        assertEquals(0, source.updateRanges.size)
    }

    @Test
    fun setIndexChoosesSixteenOrThirtyTwoBitStorage() {
        val small = BufferGeometry().setIndex(intArrayOf(0, 1, 65534))
        assertIs<Uint16BufferAttribute>(small.index)
        assertEquals(65534.0, small.index!!.getX(2))
        val large = BufferGeometry().setIndex(intArrayOf(65535, 0, 1))
        assertIs<Uint32BufferAttribute>(large.index)
    }
}
