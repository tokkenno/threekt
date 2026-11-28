package com.aitorgf.threekt.lights

import com.aitorgf.threekt.materials.MeshPhongMaterial
import com.aitorgf.threekt.math.Color
import com.aitorgf.threekt.math.Vector3
import kotlin.test.Test
import kotlin.test.assertEquals

class LightsTest {
    @Test
    fun lightsAndPhongMaterialsUseThreeJsDefaults() {
        val directional = DirectionalLight()
        assertEquals(Vector3(0.0, 1.0, 0.0), directional.position)
        assertEquals(1.0, directional.intensity)
        assertEquals(Vector3(), directional.target.position)
        assertEquals(Color(0xcccccc), AmbientLight(0xcccccc).color)
        val material = MeshPhongMaterial()
        assertEquals(Color(0x111111), material.specular)
        assertEquals(30.0, material.shininess)
        assertEquals(Color(0.0, 0.0, 0.0), material.emissive)
        assertEquals(true, material.fog)
    }
}
