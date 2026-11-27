package com.aitorgf.threekt.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LayersTest {
    @Test
    fun masksMatchThreeJs() {
        val layers = Layers()
        assertEquals(1, layers.mask)
        layers.set(3)
        assertEquals(8, layers.mask)
        layers.enable(0)
        assertEquals(9, layers.mask)
        layers.toggle(3)
        assertEquals(1, layers.mask)
        layers.toggle(4)
        assertEquals(17, layers.mask)
        layers.disable(0)
        assertEquals(16, layers.mask)
        assertTrue(layers.isEnabled(4))
        assertFalse(layers.isEnabled(0))
        layers.enableAll()
        assertEquals(-1, layers.mask)
        assertTrue(layers.isEnabled(31))
        layers.disableAll()
        assertEquals(0, layers.mask)
    }

    @Test
    fun testRequiresASharedLayer() {
        val a = Layers()
        val b = Layers()
        assertTrue(a.test(b))
        b.set(1)
        assertFalse(a.test(b))
        a.enable(1)
        assertTrue(a.test(b))
    }
}
