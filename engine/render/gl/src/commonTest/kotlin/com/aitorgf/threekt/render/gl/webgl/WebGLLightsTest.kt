package com.aitorgf.threekt.render.gl.webgl

import com.aitorgf.threekt.lights.AmbientLight
import com.aitorgf.threekt.lights.DirectionalLight
import com.aitorgf.threekt.lights.Light
import com.aitorgf.threekt.math.Color
import com.aitorgf.threekt.math.Matrix4
import com.aitorgf.threekt.scenes.Scene
import kotlin.test.Test
import kotlin.test.assertEquals

class WebGLLightsTest {
    @Test
    fun webGLLightsSumAmbientLightsAndPointDirectionalLightsAtTheirTarget() {
        val scene = Scene()
        val ambient = AmbientLight(Color(0.5, 0.25, 0.0), 2.0)
        val second = AmbientLight(Color(0.0, 0.25, 0.5))
        val directional = DirectionalLight(Color(1.0, 0.5, 0.0), 3.0).apply { position.set(1.0, 1.0, 1.0) }
        directional.target.position.set(1.0, 0.0, 1.0)
        directional.target.updateMatrixWorld()
        listOf(ambient, second, directional).forEach { scene.add(it) }
        scene.updateMatrixWorld()
        val lights = listOf<Light>(ambient, second, directional)

        val uniforms = WebGLLights()
        uniforms.setup(lights)
        assertEquals(Color(1.0, 0.75, 0.5), uniforms.ambient)
        assertEquals(Color(3.0, 1.5, 0.0), uniforms.directional.single().color)

        // The view matrix rotates the world a quarter turn about Z, so world +Y becomes view -X.
        val view = Matrix4().makeRotationZ(-kotlin.math.PI / 2)
        uniforms.setupView(lights, view)
        val direction = uniforms.directional.single().direction
        assertEquals(1.0, direction.x, 1e-12)
        assertEquals(0.0, direction.y, 1e-12)
        assertEquals(1.0, direction.length(), 1e-12)

        uniforms.setup(listOf(ambient))
        assertEquals(0, uniforms.directional.size)
    }
}
