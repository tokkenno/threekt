package com.aitorgf.threekt.renderers

import com.aitorgf.threekt.cameras.OrthographicCamera
import com.aitorgf.threekt.core.BufferGeometry
import com.aitorgf.threekt.core.Float32BufferAttribute
import com.aitorgf.threekt.core.Usage
import com.aitorgf.threekt.materials.Blending
import com.aitorgf.threekt.materials.MeshBasicMaterial
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

class BlendingRenderingTest {
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

    private fun read(gl: dynamic): List<Int> {
        val pixels: dynamic = js("new Uint8Array(4)")
        gl.readPixels(16, 16, 1, 1, gl.RGBA, gl.UNSIGNED_BYTE, pixels)
        return List(4) { pixels[it].unsafeCast<Int>() }
    }

    private fun assertNear(expected: List<Int>, actual: List<Int>, tolerance: Int = 3) {
        assertTrue(expected.indices.all { abs(expected[it] - actual[it]) <= tolerance }, "Expected $expected, got $actual")
    }

    /** A triangle that covers the whole view of an orthographic camera at z = 1. */
    private fun cover(z: Float = 0f) = BufferGeometry().setAttribute(
        "position", Float32BufferAttribute(floatArrayOf(-1f, -1f, z, 3f, -1f, z, -1f, 3f, z), 3),
    )

    @Test
    fun additiveBlendingAddsEvenWithoutTransparency() = withRenderer { renderer, gl ->
        val scene = Scene()
        scene.add(Mesh(cover(), MeshBasicMaterial(0xff0000)))
        val blue = MeshBasicMaterial(0x0000ff).apply { blending = Blending.Additive; depthTest = false }
        scene.add(Mesh(cover(0.5f), blue).apply { renderOrder = 1 })
        val camera = OrthographicCamera().apply { position.z = 1.0 }
        renderer.render(scene, camera)
        assertNear(listOf(255, 0, 255, 255), read(gl))

        // Normal blending without transparency replaces the color.
        blue.blending = Blending.Normal
        renderer.render(scene, camera)
        assertNear(listOf(0, 0, 255, 255), read(gl))

        blue.blending = Blending.Subtractive
        assertFailsWith<IllegalArgumentException> { renderer.render(scene, camera) }
        blue.premultipliedAlpha = true
        renderer.render(scene, camera)
        assertNear(listOf(255, 0, 0, 255), read(gl))
    }

    @Test
    fun dynamicAttributesAreUpdatedInPlace() = withRenderer { renderer, gl ->
        val positions = Float32BufferAttribute(floatArrayOf(2f, 2f, 0f, 3f, 2f, 0f, 2f, 3f, 0f), 3)
            .setUsage(Usage.DynamicDraw) as Float32BufferAttribute
        val scene = Scene()
        scene.add(Mesh(BufferGeometry().setAttribute("position", positions), MeshBasicMaterial(Color(0.0, 1.0, 0.0))))
        val camera = OrthographicCamera().apply { position.z = 1.0 }
        renderer.render(scene, camera)
        assertNear(listOf(0, 0, 0, 255), read(gl))
        floatArrayOf(-1f, -1f, 0f, 3f, -1f, 0f, -1f, 3f, 0f).copyInto(positions.array)
        positions.markUpdated()
        renderer.render(scene, camera)
        assertNear(listOf(0, 255, 0, 255), read(gl))
    }
}
