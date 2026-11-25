package com.aitorgf.threekt.loaders

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PCDLoaderTest {
    @Test
    fun compressedFieldsAreStoredSeparatelyAndUseLittleEndianNumbers() {
        val header = """
            FIELDS x y z rgb
            SIZE 4 4 4 4
            TYPE F F F U
            POINTS 2
            DATA binary_compressed
        """.trimIndent().plus("\n").encodeToByteArray()
        // Two values per field. One LZF literal run contains the full 32-byte payload.
        val payload = intArrayOf(
            1f.toRawBits(), 2f.toRawBits(), 3f.toRawBits(), 4f.toRawBits(),
            5f.toRawBits(), 6f.toRawBits(), 0x00ff0000, 0x0000ff00,
        )
        val bytes = header + byteArrayOf(33, 0, 0, 0, 32, 0, 0, 0, 31) +
            ByteArray(32) { index -> (payload[index / 4] ushr (index % 4 * 8)).toByte() }
        val geometry = PCDLoader().parse(bytes).geometry
        val positions = geometry.getAttribute("position")!!
        val colors = geometry.getAttribute("color")!!
        assertEquals(2, positions.count)
        assertEquals(3.0, positions.getY(0))
        assertEquals(6.0, positions.getZ(1))
        assertEquals(1.0, colors.getX(0))
        assertEquals(1.0, colors.getY(1))
        assertFailsWith<IllegalArgumentException> { PCDLoader().parse(bytes.copyOf(bytes.size - 1)) }
    }
}
