package com.aitorgf.threekt.materials

import com.aitorgf.threekt.core.Uniform
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
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

    @Test
    fun rawShaderUniformsAreNamedAndMutable() {
        val time = Uniform(1.0)
        val material = RawShaderMaterial("vertex", "fragment")
        material.uniforms.set("time", time).set("scale", Uniform(2.0))
        assertSame(time, material.uniforms.get("time"))
        assertContentEquals(arrayOf("time", "scale"), material.uniforms.names)
        time.value = 3.0
        assertEquals(3.0, material.uniforms.get("time")?.value)
        material.uniforms.remove("time")
        assertNull(material.uniforms.get("time"))
    }
}
