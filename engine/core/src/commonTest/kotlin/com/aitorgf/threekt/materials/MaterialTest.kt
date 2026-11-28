package com.aitorgf.threekt.materials

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MaterialTest {
    @Test
    fun materialsDefaultToOpaqueFrontFacesWithDepth() {
        val material = MeshBasicMaterial()
        assertEquals(Side.Front, material.side)
        assertEquals(false, material.transparent)
        assertEquals(1.0, material.opacity)
        assertTrue(material.depthTest && material.depthWrite)
    }
}
