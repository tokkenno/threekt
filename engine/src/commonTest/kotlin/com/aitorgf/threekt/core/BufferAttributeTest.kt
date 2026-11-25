package com.aitorgf.threekt.core

import com.aitorgf.threekt.math.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BufferAttributeTest {
    @Test
    fun uint8AttributesReadBytesAsUnsignedAndNormalizeOnRequest() {
        val bytes = byteArrayOf(0, 127, -128, -1)
        val attribute = Uint8BufferAttribute(bytes, 4)
        assertEquals(1, attribute.count)
        assertEquals(255, attribute.getUnsigned(3))
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
}
