package com.aitorgf.threekt.renderers

import com.aitorgf.threekt.cameras.OrthographicCamera
import com.aitorgf.threekt.core.Float32BufferAttribute
import com.aitorgf.threekt.core.InstancedBufferAttribute
import com.aitorgf.threekt.core.InstancedBufferGeometry
import com.aitorgf.threekt.core.InstancedInterleavedBuffer
import com.aitorgf.threekt.core.InterleavedBufferAttribute
import com.aitorgf.threekt.geometries.PlaneGeometry
import com.aitorgf.threekt.lights.PointLight
import com.aitorgf.threekt.materials.GLSLVersion
import com.aitorgf.threekt.materials.MeshBasicMaterial
import com.aitorgf.threekt.materials.MeshLambertMaterial
import com.aitorgf.threekt.materials.MeshNormalMaterial
import com.aitorgf.threekt.materials.RawShaderMaterial
import com.aitorgf.threekt.materials.SpriteMaterial
import com.aitorgf.threekt.math.Color
import com.aitorgf.threekt.math.Matrix4
import com.aitorgf.threekt.objects.InstancedMesh
import com.aitorgf.threekt.objects.Mesh
import com.aitorgf.threekt.objects.Sprite
import com.aitorgf.threekt.renderers.webgl.WebGL2Context
import com.aitorgf.threekt.scenes.Scene
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class InstancingRenderingTest {
    private fun withRenderer(block: (renderer: WebGLRenderer, gl: dynamic) -> Unit) {
        val canvas: dynamic = js("document.createElement('canvas')")
        val gl = assertNotNull(canvas.getContext("webgl2"), "A WebGL2 context is required.")
        val renderer = WebGLRenderer(WebGL2Context(canvas))
        try {
            renderer.setSize(32, 32)
            block(renderer, gl)
            assertEquals(gl.NO_ERROR, gl.getError())
        } finally {
            renderer.dispose()
        }
    }

    private fun read(gl: dynamic, x: Int, y: Int): List<Int> {
        val pixels: dynamic = js("new Uint8Array(4)")
        gl.readPixels(x, y, 1, 1, gl.RGBA, gl.UNSIGNED_BYTE, pixels)
        return List(4) { pixels[it].unsafeCast<Int>() }
    }

    private fun assertNear(expected: List<Int>, actual: List<Int>, tolerance: Int = 3) {
        assertTrue(expected.indices.all { abs(expected[it] - actual[it]) <= tolerance }, "Expected $expected, got $actual")
    }

    private val black = listOf(0, 0, 0, 255)
    private val camera = OrthographicCamera().apply { position.z = 1.0 }

    @Test
    fun instancedMeshesPlaceAndTintEachInstance() = withRenderer { renderer, gl ->
        val mesh = InstancedMesh(PlaneGeometry(0.5, 0.5), MeshBasicMaterial(), 2)
        mesh.setMatrixAt(0, Matrix4().makeTranslation(-0.5, 0.0, 0.0))
        mesh.setMatrixAt(1, Matrix4().makeTranslation(0.5, 0.0, 0.0))
        mesh.setColorAt(0, Color(1.0, 0.0, 0.0))
        mesh.setColorAt(1, Color(0.0, 0.0, 1.0))
        val scene = Scene().apply { add(mesh) }
        renderer.render(scene, camera)
        assertNear(listOf(255, 0, 0, 255), read(gl, 8, 16))
        assertNear(listOf(0, 0, 255, 255), read(gl, 24, 16))
        assertNear(black, read(gl, 16, 16))

        mesh.count = 1
        renderer.render(scene, camera)
        assertNear(black, read(gl, 24, 16))
    }

    @Test
    fun instancedAttributesAdvancePerInstanceAndLimitTheCount() = withRenderer { renderer, gl ->
        val geometry = InstancedBufferGeometry()
        geometry.setAttribute("position", PlaneGeometry(0.5, 0.5).getAttribute("position")!!)
        geometry.setIndex(PlaneGeometry(0.5, 0.5).index)
        val records = InstancedInterleavedBuffer(floatArrayOf(-0.5f, 0f, 1f, 0.5f, 0f, 0f), 3)
        geometry.setAttribute("offset", InterleavedBufferAttribute(records, 2, 0))
        geometry.setAttribute("red", InterleavedBufferAttribute(records, 1, 2))
        geometry.setAttribute("unused", InstancedBufferAttribute(FloatArray(2), 1))
        val material = RawShaderMaterial(
            """
            in vec3 position; in vec2 offset; in float red; out float vRed;
            uniform mat4 modelViewMatrix; uniform mat4 projectionMatrix;
            void main() { vRed = red; gl_Position = projectionMatrix * modelViewMatrix * vec4( position.xy + offset, 0.0, 1.0 ); }
            """.trimIndent(),
            """
            precision highp float; in float vRed; out vec4 color;
            void main() { color = vec4( vRed, 0.0, 1.0 - vRed, 1.0 ); }
            """.trimIndent(),
        ).apply { glslVersion = GLSLVersion.GLSL3 }
        val scene = Scene().apply { add(Mesh(geometry, material)) }
        renderer.render(scene, camera)
        assertNear(listOf(255, 0, 0, 255), read(gl, 8, 16))
        assertNear(listOf(0, 0, 255, 255), read(gl, 24, 16))

        geometry.instanceCount = 1
        renderer.render(scene, camera)
        assertNear(black, read(gl, 24, 16))
    }

    @Test
    fun spritesFaceTheCameraAndHonorTheirCenter() = withRenderer { renderer, gl ->
        val sprite = Sprite(SpriteMaterial(Color(0.0, 1.0, 0.0))).apply {
            scale.set(0.5, 0.5, 1.0)
            center.set(0.0, 0.0)
        }
        val scene = Scene().apply { add(sprite) }
        renderer.render(scene, camera)
        assertNear(listOf(0, 255, 0, 255), read(gl, 20, 20))
        assertNear(black, read(gl, 12, 12))
    }

    @Test
    fun pointLightsLightLambertSurfacesAndNormalsAreUnencoded() = withRenderer { renderer, gl ->
        val scene = Scene()
        val plane = Mesh(PlaneGeometry(2.0, 2.0), MeshLambertMaterial())
        scene.add(plane)
        scene.add(PointLight(Color(1.0, 1.0, 1.0), kotlin.math.PI, 0.0, 0.0).apply { position.z = 1.0 })
        renderer.render(scene, camera)
        // Irradiance π · dotNL(1) times the Lambert BRDF 1/π gives white at the center.
        assertNear(listOf(255, 255, 255, 255), read(gl, 16, 16), tolerance = 4)
        val edge = read(gl, 1, 16)
        assertTrue(edge[0] in 150..250, "Expected an attenuated edge, got $edge")

        plane.material = MeshNormalMaterial()
        renderer.render(scene, camera)
        assertNear(listOf(128, 128, 255, 255), read(gl, 16, 16))
    }
}
