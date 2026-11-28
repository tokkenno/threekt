package com.aitorgf.threekt.render.gl

import com.aitorgf.threekt.cameras.OrthographicCamera
import com.aitorgf.threekt.core.BufferAttribute
import com.aitorgf.threekt.core.BufferGeometry
import com.aitorgf.threekt.core.Float16BufferAttribute
import com.aitorgf.threekt.core.Float32BufferAttribute
import com.aitorgf.threekt.core.Uint16BufferAttribute
import com.aitorgf.threekt.core.Uint32BufferAttribute
import com.aitorgf.threekt.extras.DataUtils
import com.aitorgf.threekt.materials.MeshBasicMaterial
import com.aitorgf.threekt.math.Color
import com.aitorgf.threekt.objects.Mesh
import com.aitorgf.threekt.render.gl.webgl.WebGL2Context
import com.aitorgf.threekt.scenes.Scene
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BufferAttributeRenderingTest {
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

    private fun read(gl: dynamic, x: Int = 16, y: Int = 16): List<Int> {
        val pixels: dynamic = js("new Uint8Array(4)")
        gl.readPixels(x, y, 1, 1, gl.RGBA, gl.UNSIGNED_BYTE, pixels)
        return List(4) { pixels[it].unsafeCast<Int>() }
    }

    private fun assertNear(expected: List<Int>, actual: List<Int>, tolerance: Int = 3) {
        assertTrue(expected.indices.all { abs(expected[it] - actual[it]) <= tolerance }, "Expected $expected, got $actual")
    }

    // A quad covering the view; the first triangle covers the center only with the right indices.
    private val corners = floatArrayOf(-1f, -1f, 0f, 1f, -1f, 0f, 1f, 1f, 0f, -1f, 1f, 0f)

    private fun draw(renderer: WebGLRenderer, geometry: BufferGeometry, gl: dynamic): List<Int> {
        val scene = Scene()
        scene.add(Mesh(geometry, MeshBasicMaterial(Color(0.0, 1.0, 0.0))))
        renderer.render(scene, OrthographicCamera().apply { position.z = 1.0 })
        return read(gl)
    }

    @Test
    fun sixteenAndThirtyTwoBitIndicesSelectTheirElementType() = withRenderer { renderer, gl ->
        val positions = Float32BufferAttribute(corners, 3)
        val indices: List<BufferAttribute> = listOf(
            Uint16BufferAttribute(shortArrayOf(0, 1, 2, 0, 2, 3), 1),
            Uint32BufferAttribute(intArrayOf(0, 1, 2, 0, 2, 3), 1),
        )
        for (index in indices) {
            val geometry = BufferGeometry().setAttribute("position", positions).setIndex(index)
            assertNear(listOf(0, 255, 0, 255), draw(renderer, geometry, gl))
        }
    }

    @Test
    fun halfFloatPositionsAreDrawn() = withRenderer { renderer, gl ->
        val half = ShortArray(corners.size) { DataUtils.toHalfFloat(corners[it].toDouble()).toShort() }
        val geometry = BufferGeometry().setAttribute("position", Float16BufferAttribute(half, 3))
            .setIndex(intArrayOf(0, 1, 2, 0, 2, 3))
        assertNear(listOf(0, 255, 0, 255), draw(renderer, geometry, gl))
    }

    @Test
    fun updateRangesUploadOnlyTheirElements() = withRenderer { renderer, gl ->
        // Triangle 0-1-2 covers the lower-right half of the view and 0-2-3 the upper-left half.
        val index = Uint16BufferAttribute(ShortArray(6), 1)
        val geometry = BufferGeometry().setAttribute("position", Float32BufferAttribute(corners, 3)).setIndex(index)
        val green = listOf(0, 255, 0, 255)
        val black = listOf(0, 0, 0, 255)
        draw(renderer, geometry, gl)
        assertNear(black, read(gl, 24, 8))
        assertNear(black, read(gl, 8, 24))

        shortArrayOf(0, 1, 2, 0, 2, 3).copyInto(index.array)
        index.addUpdateRange(1, 2)
        index.markUpdated()
        draw(renderer, geometry, gl)
        // Only the first triangle's indices were uploaded.
        assertNear(green, read(gl, 24, 8))
        assertNear(black, read(gl, 8, 24))
        assertEquals(0, index.updateRanges.size)

        index.addUpdateRange(4, 2)
        index.markUpdated()
        draw(renderer, geometry, gl)
        assertNear(green, read(gl, 8, 24))
    }
}
