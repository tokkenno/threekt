package com.aitorgf.threekt.render.gl

import com.aitorgf.threekt.cameras.OrthographicCamera
import com.aitorgf.threekt.core.BufferGeometry
import com.aitorgf.threekt.core.Float32BufferAttribute
import com.aitorgf.threekt.materials.Material
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

class MultiMaterialRenderingTest {
    private fun read(gl: dynamic, x: Int, y: Int): List<Int> {
        val pixels: dynamic = js("new Uint8Array(4)")
        gl.readPixels(x, y, 1, 1, gl.RGBA, gl.UNSIGNED_BYTE, pixels)
        return List(4) { pixels[it].unsafeCast<Int>() }
    }

    private fun assertNear(expected: List<Int>, actual: List<Int>) {
        assertTrue(expected.indices.all { abs(expected[it] - actual[it]) <= 3 }, "Expected $expected, got $actual")
    }

    @Test
    fun eachDrawGroupUsesItsMaterialAndMissingMaterialsAreSkipped() {
        val canvas: dynamic = js("document.createElement('canvas')")
        val gl = assertNotNull(canvas.getContext("webgl2"), "A WebGL2 context is required.")
        val renderer = WebGLRenderer(WebGL2Context(canvas))
        try {
            renderer.setSize(32, 32)
            // Three quads side by side, two triangles each, non-indexed.
            val positions = mutableListOf<Float>()
            for (q in 0 until 3) {
                val x0 = -1f + q * 2f / 3
                val x1 = x0 + 2f / 3
                positions.addAll(listOf(x0, -1f, 0f, x1, -1f, 0f, x1, 1f, 0f, x0, -1f, 0f, x1, 1f, 0f, x0, 1f, 0f))
            }
            val geometry = BufferGeometry().setAttribute("position", Float32BufferAttribute(positions.toFloatArray(), 3))
            geometry.addGroup(0, 6, 1)
            geometry.addGroup(6, 6, 0)
            geometry.addGroup(12, 6, 5) // No material at index 5.
            val materials = arrayOf<Material>(MeshBasicMaterial(Color(0.0, 1.0, 0.0)), MeshBasicMaterial(Color(1.0, 0.0, 0.0)))
            val scene = Scene().apply { add(Mesh(geometry, materials)) }
            renderer.render(scene, OrthographicCamera().apply { position.z = 1.0 })
            assertNear(listOf(255, 0, 0, 255), read(gl, 5, 16))
            assertNear(listOf(0, 255, 0, 255), read(gl, 16, 16))
            assertNear(listOf(0, 0, 0, 255), read(gl, 27, 16))
            assertEquals(gl.NO_ERROR, gl.getError())
        } finally {
            renderer.dispose()
        }
    }
}
