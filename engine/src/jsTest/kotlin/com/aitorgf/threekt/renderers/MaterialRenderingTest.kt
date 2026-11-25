package com.aitorgf.threekt.renderers

import com.aitorgf.threekt.cameras.PerspectiveCamera
import com.aitorgf.threekt.core.BufferGeometry
import com.aitorgf.threekt.core.Float32BufferAttribute
import com.aitorgf.threekt.core.Uint8BufferAttribute
import com.aitorgf.threekt.core.Uniform
import com.aitorgf.threekt.materials.GLSLVersion
import com.aitorgf.threekt.materials.MeshBasicMaterial
import com.aitorgf.threekt.materials.RawShaderMaterial
import com.aitorgf.threekt.materials.Side
import com.aitorgf.threekt.math.Color
import com.aitorgf.threekt.objects.Mesh
import com.aitorgf.threekt.renderers.webgl.WebGL2Context
import com.aitorgf.threekt.scenes.Scene
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MaterialRenderingTest {
    private val vertexShader = """
        precision mediump float;
        uniform mat4 modelViewMatrix;
        uniform mat4 projectionMatrix;
        attribute vec3 position;
        attribute vec4 color;
        varying vec4 vColor;
        void main() {
            vColor = color;
            gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);
        }
    """.trimIndent()

    private val fragmentShader = """
        precision mediump float;
        uniform float scale;
        uniform vec3 tint;
        varying vec4 vColor;
        void main() {
            gl_FragColor = vec4(vColor.rgb * scale * tint, vColor.a);
        }
    """.trimIndent()

    /** A counterclockwise triangle facing +Z that covers the center of the view from z = 3. */
    private fun triangle(): BufferGeometry = BufferGeometry().setAttribute(
        "position", Float32BufferAttribute(floatArrayOf(-2f, -2f, 0f, 2f, -2f, 0f, 0f, 2f, 0f), 3),
    )

    private fun quad(size: Float): BufferGeometry = BufferGeometry().setAttribute(
        "position",
        Float32BufferAttribute(floatArrayOf(-size, -size, 0f, size, -size, 0f, size, size, 0f, -size, -size, 0f, size, size, 0f, -size, size, 0f), 3),
    )

    private fun withRenderer(block: (renderer: WebGLRenderer, gl: dynamic) -> Unit) {
        val canvas: dynamic = js("document.createElement('canvas')")
        canvas.width = 32
        canvas.height = 32
        val gl = assertNotNull(canvas.getContext("webgl2"), "A WebGL2 context is required.")
        val renderer = WebGLRenderer(WebGL2Context(canvas))
        try {
            block(renderer, gl)
            assertEquals(gl.NO_ERROR, gl.getError())
        } finally {
            renderer.dispose()
        }
    }

    private fun readRgba(gl: dynamic): List<Int> {
        val pixels: dynamic = js("new Uint8Array(4)")
        gl.readPixels(16, 16, 1, 1, gl.RGBA, gl.UNSIGNED_BYTE, pixels)
        return List(4) { pixels[it].unsafeCast<Int>() }
    }

    private fun assertNear(expected: List<Int>, actual: List<Int>, tolerance: Int = 3) {
        assertTrue(expected.indices.all { abs(expected[it] - actual[it]) <= tolerance }, "Expected $expected, got $actual")
    }

    @Test
    fun rawShadersReadNamedAttributesBuiltInMatricesAndUniforms() = withRenderer { renderer, gl ->
        val geometry = triangle()
        // Opaque cyan-ish color: 0, 200, 255 with full alpha.
        val color = ByteArray(12) { index -> byteArrayOf(0, 200.toByte(), 255.toByte(), 255.toByte())[index % 4] }
        geometry.setAttribute("color", Uint8BufferAttribute(color, 4, normalized = true))
        val scale = Uniform(0.5)
        val material = RawShaderMaterial(vertexShader, fragmentShader)
        material.uniforms.set("scale", scale).set("tint", Uniform(Color(1.0, 1.0, 1.0)))
        val scene = Scene()
        scene.add(Mesh(geometry, material))
        val camera = PerspectiveCamera().apply { position.z = 3.0 }

        renderer.render(scene, camera)
        assertNear(listOf(0, 100, 128, 255), readRgba(gl))

        scale.value = 1.0
        renderer.render(scene, camera)
        assertNear(listOf(0, 200, 255, 255), readRgba(gl))

        // Changing the bytes has no effect until the attribute is marked as updated.
        color.fill(255.toByte())
        renderer.render(scene, camera)
        assertNear(listOf(0, 200, 255, 255), readRgba(gl))
        geometry.getAttribute("color")!!.markUpdated()
        renderer.render(scene, camera)
        assertNear(listOf(255, 255, 255, 255), readRgba(gl))
    }

    @Test
    fun uniformsWithTheWrongTypeAreRejected() = withRenderer { renderer, _ ->
        val material = RawShaderMaterial(vertexShader, fragmentShader)
        material.uniforms.set("scale", Uniform(Color(1.0, 1.0, 1.0))).set("tint", Uniform(Color(1.0, 1.0, 1.0)))
        val scene = Scene()
        scene.add(Mesh(triangle(), material))
        val error = assertFailsWith<IllegalArgumentException> {
            renderer.render(scene, PerspectiveCamera().apply { position.z = 3.0 })
        }
        assertTrue(error.message!!.contains("scale"))
    }

    @Test
    fun sideSelectsWhichWindingIsCulled() = withRenderer { renderer, gl ->
        val material = MeshBasicMaterial(0xff0000)
        val mesh = Mesh(triangle(), material)
        val scene = Scene()
        scene.add(mesh)
        val front = PerspectiveCamera().apply { position.z = 3.0 }
        val back = PerspectiveCamera().apply { position.z = -3.0; lookAt(com.aitorgf.threekt.math.Vector3()) }

        renderer.render(scene, front)
        assertNear(listOf(255, 0, 0, 255), readRgba(gl))
        renderer.render(scene, back)
        assertNear(listOf(0, 0, 0, 255), readRgba(gl))

        material.side = Side.Back
        renderer.render(scene, front)
        assertNear(listOf(0, 0, 0, 255), readRgba(gl))
        renderer.render(scene, back)
        assertNear(listOf(255, 0, 0, 255), readRgba(gl))

        material.side = Side.Double
        renderer.render(scene, front)
        assertNear(listOf(255, 0, 0, 255), readRgba(gl))

        // A negative scale mirrors the mesh, which turns its front faces away from the camera.
        material.side = Side.Front
        mesh.scale.x = -1.0
        renderer.render(scene, front)
        assertNear(listOf(255, 0, 0, 255), readRgba(gl))
    }

    @Test
    fun transparentMeshesBlendOverOpaqueOnesRegardlessOfSceneOrder() = withRenderer { renderer, gl ->
        val scene = Scene()
        scene.background = Color(0.0, 0.0, 0.0)
        val glass = Mesh(quad(1f), MeshBasicMaterial(0xff0000).apply { transparent = true; opacity = 0.5 })
        val wall = Mesh(quad(4f), MeshBasicMaterial(0x0000ff)).apply { position.z = -1.0 }
        // The transparent quad is added first but must still be drawn after the opaque wall behind it.
        scene.add(glass)
        scene.add(wall)
        renderer.render(scene, PerspectiveCamera().apply { position.z = 3.0 })
        assertNear(listOf(128, 0, 128, 255), readRgba(gl))
    }

    @Test
    fun backgroundAndClearColorFillTheView() = withRenderer { renderer, gl ->
        val scene = Scene()
        val camera = PerspectiveCamera()
        renderer.render(scene, camera)
        assertNear(listOf(0, 0, 0, 255), readRgba(gl))

        renderer.setClearColor(Color(0x00ff00), 1.0)
        renderer.render(scene, camera)
        assertNear(listOf(0, 255, 0, 255), readRgba(gl))
        assertEquals(1.0, renderer.getClearAlpha())

        // 0x101010 is stored as linear color and written back as sRGB.
        scene.background = Color(0x101010)
        renderer.render(scene, camera)
        assertNear(listOf(16, 16, 16, 255), readRgba(gl), tolerance = 1)
    }

    @Test
    fun glsl3ShadersCanDrawFromTheDrawRangeWithoutAttributes() = withRenderer { renderer, gl ->
        // A triangle that covers the whole view, generated from gl_VertexID.
        val material = RawShaderMaterial(
            """
            uniform mat4 modelViewMatrix;
            uniform mat4 projectionMatrix;
            void main() {
                vec2 corner = vec2(gl_VertexID == 1 ? 3.0 : -1.0, gl_VertexID == 2 ? 3.0 : -1.0);
                gl_Position = vec4(corner, 0.0, 1.0);
            }
            """.trimIndent(),
            """
            precision mediump float;
            uniform float seed;
            out vec4 fColor;
            void main() {
                fColor = vec4(seed, 1.0, 0.0, 1.0);
            }
            """.trimIndent(),
        )
        material.glslVersion = GLSLVersion.GLSL3
        material.uniforms.set("seed", Uniform(1.0))
        val geometry = BufferGeometry()
        val scene = Scene()
        scene.add(Mesh(geometry, material))
        val camera = PerspectiveCamera()

        // Without attributes or indices, the default unbounded range draws nothing.
        renderer.render(scene, camera)
        assertNear(listOf(0, 0, 0, 255), readRgba(gl))

        geometry.setDrawRange(0, 3)
        renderer.render(scene, camera)
        assertNear(listOf(255, 255, 0, 255), readRgba(gl))
    }

    @Test
    fun drawRangeIsClampedToVerticesAndIndices() = withRenderer { renderer, gl ->
        // A tiny triangle off to the side followed by one that covers the center.
        val geometry = BufferGeometry().setAttribute(
            "position",
            Float32BufferAttribute(floatArrayOf(-3f, -3f, 0f, -2.9f, -3f, 0f, -3f, -2.9f, 0f, -2f, -2f, 0f, 2f, -2f, 0f, 0f, 2f, 0f), 3),
        )
        val scene = Scene()
        scene.add(Mesh(geometry, MeshBasicMaterial(0xff0000)))
        val camera = PerspectiveCamera().apply { position.z = 3.0 }

        geometry.setDrawRange(0, 3)
        renderer.render(scene, camera)
        assertNear(listOf(0, 0, 0, 255), readRgba(gl))
        // The count runs past the last vertex and is clamped to the second triangle.
        geometry.setDrawRange(3, 100)
        renderer.render(scene, camera)
        assertNear(listOf(255, 0, 0, 255), readRgba(gl))

        geometry.setIndex(intArrayOf(3, 4, 5, 0, 1, 2))
        geometry.setDrawRange(3, 3)
        renderer.render(scene, camera)
        assertNear(listOf(0, 0, 0, 255), readRgba(gl))
        geometry.setDrawRange(0, 3)
        renderer.render(scene, camera)
        assertNear(listOf(255, 0, 0, 255), readRgba(gl))
    }
}
