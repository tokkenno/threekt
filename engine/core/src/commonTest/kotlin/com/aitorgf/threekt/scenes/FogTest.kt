package com.aitorgf.threekt.scenes

import com.aitorgf.threekt.materials.GLSLVersion
import com.aitorgf.threekt.math.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertNull

class FogTest {
    @Test
    fun fogCopiesItsColorAndUsesThreeJsDefaults() {
        val color = Color(0x050505)
        val fog = Fog(color)
        assertEquals(color, fog.color)
        assertNotSame(color, fog.color)
        assertEquals(1.0, fog.near)
        assertEquals(1000.0, fog.far)
        val clone = Fog(color, 2000.0, 3500.0).clone()
        assertEquals(2000.0, clone.near)
        assertEquals(3500.0, clone.far)
        assertNull(Scene().fog)
    }

    @Test
    fun glslVersionsMatchThreeJsConstants() {
        assertEquals("100", GLSLVersion.GLSL1.version)
        assertEquals("300 es", GLSLVersion.GLSL3.version)
    }
}
