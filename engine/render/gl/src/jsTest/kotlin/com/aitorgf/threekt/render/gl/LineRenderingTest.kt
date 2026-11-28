package com.aitorgf.threekt.render.gl

import com.aitorgf.threekt.cameras.OrthographicCamera
import com.aitorgf.threekt.core.BufferGeometry
import com.aitorgf.threekt.core.Float32BufferAttribute
import com.aitorgf.threekt.materials.LineBasicMaterial
import com.aitorgf.threekt.materials.MeshBasicMaterial
import com.aitorgf.threekt.math.Color
import com.aitorgf.threekt.objects.Line
import com.aitorgf.threekt.objects.LineSegments
import com.aitorgf.threekt.objects.Mesh
import com.aitorgf.threekt.render.gl.webgl.WebGL2Context
import com.aitorgf.threekt.scenes.Scene
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LineRenderingTest {
    /** Renders with an orthographic camera that maps x and y in [-1, 1] onto a 32 × 32 canvas. */
    private fun withRenderer(block: (renderer: WebGLRenderer, gl: dynamic, scene: Scene, camera: OrthographicCamera) -> Unit) {
        val canvas: dynamic = js("document.createElement('canvas')")
        canvas.width = 32
        canvas.height = 32
        val gl = assertNotNull(canvas.getContext("webgl2"), "A WebGL2 context is required.")
        val renderer = WebGLRenderer(WebGL2Context(canvas))
        try {
            block(renderer, gl, Scene(), OrthographicCamera().apply { position.z = 1.0 })
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

    private fun positions(vararg values: Float) = Float32BufferAttribute(values, 3)

    @Test
    fun lineStripsConnectEveryVertexAndSegmentsOnlyPairs() = withRenderer { renderer, gl, scene, camera ->
        // A horizontal line through the row y = 16.5 followed by a vertical one through x = 16.5.
        val geometry = BufferGeometry().setAttribute(
            "position", positions(-1f, 1f / 32, 0f, 1f, 1f / 32, 0f, 1f / 32, -1f, 0f, 1f / 32, 1f, 0f),
        )
        val line = Line(geometry, LineBasicMaterial(0xff0000))
        scene.add(line)
        renderer.render(scene, camera)
        assertNear(listOf(255, 0, 0, 255), read(gl, 4, 16))
        assertNear(listOf(255, 0, 0, 255), read(gl, 16, 4))
        // The strip also joins (1, y) to (x, -1), crossing the lower right quadrant.
        assertNear(listOf(255, 0, 0, 255), read(gl, 24, 8), tolerance = 255)

        scene.remove(line)
        scene.add(LineSegments(geometry, LineBasicMaterial(0x00ff00)))
        renderer.render(scene, camera)
        assertNear(listOf(0, 255, 0, 255), read(gl, 4, 16))
        assertNear(listOf(0, 255, 0, 255), read(gl, 16, 4))
    }

    @Test
    fun vertexColorsAreLinearAndMultiplyTheMaterialColor() = withRenderer { renderer, gl, scene, camera ->
        val geometry = BufferGeometry()
            .setAttribute("position", positions(-1f, 1f / 32, 0f, 1f, 1f / 32, 0f))
            .setAttribute("color", Float32BufferAttribute(floatArrayOf(0.5f, 1f, 0f, 0.5f, 1f, 0f), 3))
        val material = LineBasicMaterial()
        scene.add(Line(geometry, material))
        renderer.render(scene, camera)
        // Without vertexColors the attribute is ignored.
        assertNear(listOf(255, 255, 255, 255), read(gl, 16, 16))
        material.vertexColors = true
        renderer.render(scene, camera)
        // Linear 0.5 is encoded as sRGB 188.
        assertNear(listOf(188, 255, 0, 255), read(gl, 16, 16))
        material.color.set(1.0, 0.5, 1.0)
        renderer.render(scene, camera)
        assertNear(listOf(188, 188, 0, 255), read(gl, 16, 16))
    }

    @Test
    fun morphTargetsBlendPositionsOnTheGpu() = withRenderer { renderer, gl, scene, camera ->
        // The base triangle lies outside the view; the morph target covers its center.
        val geometry = BufferGeometry().setAttribute("position", positions(2f, 2f, 0f, 3f, 2f, 0f, 2f, 3f, 0f))
        geometry.setMorphAttribute("position", arrayOf(positions(-1f, -1f, 0f, 3f, -1f, 0f, -1f, 3f, 0f)))
        val mesh = Mesh(geometry, MeshBasicMaterial(0x0000ff))
        scene.add(mesh)
        val influences = mesh.morphTargetInfluences!!
        renderer.render(scene, camera)
        assertNear(listOf(0, 0, 0, 255), read(gl, 16, 16))
        influences[0] = 1.0
        renderer.render(scene, camera)
        assertNear(listOf(0, 0, 255, 255), read(gl, 16, 16))

        // Relative targets are offsets from the base positions. As in Three.js, the morph texture of a
        // geometry is built once, so the relative targets go in a new geometry.
        val relative = BufferGeometry().setAttribute("position", positions(2f, 2f, 0f, 3f, 2f, 0f, 2f, 3f, 0f))
        relative.morphTargetsRelative = true
        relative.setMorphAttribute("position", arrayOf(positions(-3f, -3f, 0f, 0f, -3f, 0f, -3f, 0f, 0f)))
        mesh.geometry = relative
        mesh.updateMorphTargets()
        mesh.morphTargetInfluences!![0] = 1.0
        renderer.render(scene, camera)
        assertNear(listOf(0, 0, 255, 255), read(gl, 16, 16))
        mesh.morphTargetInfluences!![0] = 0.25
        renderer.render(scene, camera)
        assertNear(listOf(0, 0, 0, 255), read(gl, 16, 16))
    }

    @Test
    fun indexedSegmentsDrawThePairsTheIndicesSelect() = withRenderer { renderer, gl, scene, camera ->
        // Vertices of a horizontal and a vertical line through the center; the indices pick one of them.
        val geometry = BufferGeometry().setAttribute(
            "position", positions(-1f, 1f / 32, 0f, 1f, 1f / 32, 0f, 1f / 32, -1f, 0f, 1f / 32, 1f, 0f),
        )
        geometry.setIndex(intArrayOf(2, 3))
        scene.add(LineSegments(geometry, LineBasicMaterial(0xffffff)))
        renderer.render(scene, camera)
        assertNear(listOf(255, 255, 255, 255), read(gl, 16, 4))
        assertNear(listOf(0, 0, 0, 255), read(gl, 4, 16))

        geometry.setIndex(intArrayOf(2, 3, 0, 1))
        geometry.setDrawRange(2, 2)
        renderer.render(scene, camera)
        assertNear(listOf(0, 0, 0, 255), read(gl, 16, 4))
        assertNear(listOf(255, 255, 255, 255), read(gl, 4, 16))
    }
}
