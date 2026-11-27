package com.aitorgf.threekt.renderers

import com.aitorgf.threekt.cameras.OrthographicCamera
import com.aitorgf.threekt.core.BufferGeometry
import com.aitorgf.threekt.core.Float32BufferAttribute
import com.aitorgf.threekt.lights.AmbientLight
import com.aitorgf.threekt.lights.DirectionalLight
import com.aitorgf.threekt.materials.MeshPhongMaterial
import com.aitorgf.threekt.materials.Side
import com.aitorgf.threekt.math.Color
import com.aitorgf.threekt.math.Vector3
import com.aitorgf.threekt.objects.Mesh
import com.aitorgf.threekt.renderers.webgl.WebGL2Context
import com.aitorgf.threekt.scenes.Scene
import kotlin.math.PI
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PhongRenderingTest {
    private fun withScene(block: (renderer: WebGLRenderer, gl: dynamic, scene: Scene, material: MeshPhongMaterial, camera: OrthographicCamera) -> Unit) {
        val canvas: dynamic = js("document.createElement('canvas')")
        canvas.width = 32
        canvas.height = 32
        val gl = assertNotNull(canvas.getContext("webgl2"), "A WebGL2 context is required.")
        val renderer = WebGLRenderer(WebGL2Context(canvas))
        try {
            // A triangle facing +Z that covers the view, with matching normals.
            val geometry = BufferGeometry()
                .setAttribute("position", Float32BufferAttribute(floatArrayOf(-1f, -1f, 0f, 3f, -1f, 0f, -1f, 3f, 0f), 3))
                .setAttribute("normal", Float32BufferAttribute(floatArrayOf(0f, 0f, 1f, 0f, 0f, 1f, 0f, 0f, 1f), 3))
            val material = MeshPhongMaterial().apply { specular.set(0.0, 0.0, 0.0) }
            val scene = Scene()
            scene.add(Mesh(geometry, material))
            block(renderer, gl, scene, material, OrthographicCamera().apply { position.z = 1.0 })
            assertEquals(gl.NO_ERROR, gl.getError())
        } finally {
            renderer.dispose()
        }
    }

    private fun read(gl: dynamic): List<Int> {
        val pixels: dynamic = js("new Uint8Array(4)")
        gl.readPixels(16, 16, 1, 1, gl.RGBA, gl.UNSIGNED_BYTE, pixels)
        return List(4) { pixels[it].unsafeCast<Int>() }
    }

    private fun assertGray(expected: Int, actual: List<Int>, tolerance: Int = 2) {
        assertTrue((0..2).all { abs(actual[it] - expected) <= tolerance } && actual[3] == 255, "Expected gray $expected, got $actual")
    }

    @Test
    fun ambientLightIsLambertian() = withScene { renderer, gl, scene, _, camera ->
        renderer.render(scene, camera)
        assertGray(0, read(gl))
        val ambient = AmbientLight(Color(1.0, 1.0, 1.0), PI)
        scene.add(ambient)
        renderer.render(scene, camera)
        assertGray(255, read(gl))
        // Intensity 1 gives 1 / PI in linear space, 153 in sRGB.
        ambient.intensity = 1.0
        renderer.render(scene, camera)
        assertGray(153, read(gl))
    }

    @Test
    fun directionalLightsFollowLambertsCosineLaw() = withScene { renderer, gl, scene, _, camera ->
        val light = DirectionalLight(Color(1.0, 1.0, 1.0), PI).apply { position.set(0.0, 0.0, 1.0) }
        scene.add(light)
        renderer.render(scene, camera)
        assertGray(255, read(gl))
        // At 45 degrees, cos = 0.707 in linear space, 219 in sRGB.
        light.position.set(1.0, 0.0, 1.0)
        renderer.render(scene, camera)
        assertGray(219, read(gl))
        light.position.set(0.0, 0.0, -1.0)
        renderer.render(scene, camera)
        assertGray(0, read(gl))
    }

    @Test
    fun blinnPhongSpecularMatchesThreeJs() = withScene { renderer, gl, scene, material, camera ->
        material.color.set(0.0, 0.0, 0.0)
        material.specular.set(1.0, 1.0, 1.0)
        material.shininess = 2.0
        scene.add(DirectionalLight(Color(1.0, 1.0, 1.0), PI).apply { position.set(0.0, 0.0, 1.0) })
        // F = 1, G = 0.25, D = (2 * 0.5 + 1) / PI, irradiance = PI, so 0.5 in linear space, 188 in sRGB.
        renderer.render(scene, camera)
        assertGray(188, read(gl))
    }

    @Test
    fun doubleSidedMaterialsLightBackFacesWithFlippedNormals() = withScene { renderer, gl, scene, material, camera ->
        scene.add(DirectionalLight(Color(1.0, 1.0, 1.0), PI).apply { position.set(0.0, 0.0, -1.0) })
        camera.position.z = -1.0
        camera.lookAt(Vector3())
        renderer.render(scene, camera)
        assertGray(0, read(gl))
        material.side = Side.Double
        renderer.render(scene, camera)
        assertGray(255, read(gl))
        material.flatShading = true
        renderer.render(scene, camera)
        assertGray(255, read(gl))
    }
}
