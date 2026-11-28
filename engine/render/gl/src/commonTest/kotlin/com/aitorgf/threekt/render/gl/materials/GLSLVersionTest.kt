package com.aitorgf.threekt.render.gl.materials

import kotlin.test.Test
import kotlin.test.assertEquals

class GLSLVersionTest {
    @Test
    fun glslVersionsMatchThreeJsConstants() {
        assertEquals("100", GLSLVersion.GLSL1.version)
        assertEquals("300 es", GLSLVersion.GLSL3.version)
    }
}
