package com.aitorgf.threekt.render.gl

import com.aitorgf.threekt.cameras.OrthographicCamera
import com.aitorgf.threekt.cameras.PerspectiveCamera
import com.aitorgf.threekt.core.BufferGeometry
import com.aitorgf.threekt.core.Float32BufferAttribute
import com.aitorgf.threekt.core.Usage
import com.aitorgf.threekt.core.InterleavedBuffer
import com.aitorgf.threekt.core.InterleavedBufferAttribute
import com.aitorgf.threekt.materials.MeshBasicMaterial
import com.aitorgf.threekt.materials.PointsMaterial
import com.aitorgf.threekt.math.Color
import com.aitorgf.threekt.objects.Mesh
import com.aitorgf.threekt.objects.Points
import com.aitorgf.threekt.render.gl.webgl.WebGL2Context
import com.aitorgf.threekt.scenes.Fog
import com.aitorgf.threekt.scenes.Scene
import com.aitorgf.threekt.textures.ImageTexture
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PointsRenderingTest {
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

    private fun point(z: Float = 0f) = BufferGeometry().setAttribute("position", Float32BufferAttribute(floatArrayOf(0f, 0f, z), 3))

    private val red = listOf(255, 0, 0, 255)
    private val black = listOf(0, 0, 0, 255)

    @Test
    fun interleavedPositionsAndNormalizedColorsUseTheirOwnByteLayouts() = withRenderer { renderer, gl ->
        val positions = InterleavedBuffer(floatArrayOf(9f, -0.5f, 0f, 0f, 9f, 0.5f, 0f, 0f), 4)
        val colorBytes = byteArrayOf(9, -1, 0, 0, 9, 0, -1, 0)
        val colors = InterleavedBuffer(colorBytes, 4)
        val color = InterleavedBufferAttribute(colors, 3, 1, normalized = true)
        val geometry = BufferGeometry()
            .setAttribute("position", InterleavedBufferAttribute(positions, 3, 1))
            .setAttribute("color", color)
        geometry.computeBoundingSphere()
        assertEquals(0.5, geometry.boundingSphere?.radius)
        assertEquals(1.0, color.getY(1))

        val material = PointsMaterial().apply { size = 8.0; vertexColors = true }
        val scene = Scene()
        scene.add(Points(geometry, material))
        val camera = OrthographicCamera().apply { position.z = 1.0 }
        renderer.render(scene, camera)
        assertNear(red, read(gl, 8, 16))
        assertNear(listOf(0, 255, 0, 255), read(gl, 24, 16))

        colorBytes[1] = 0
        colorBytes[3] = -1
        color.markUpdated()
        renderer.render(scene, camera)
        assertNear(listOf(0, 0, 255, 255), read(gl, 8, 16))
    }

    @Test
    fun pointsWithoutAttenuationKeepTheirPixelSize() = withRenderer { renderer, gl ->
        val material = PointsMaterial(0xff0000).apply { size = 8.0; sizeAttenuation = false }
        val scene = Scene()
        scene.add(Points(point(), material))
        renderer.render(scene, OrthographicCamera().apply { position.z = 1.0 })
        // The 8 × 8 point covers pixels 12 to 19 around the center.
        assertNear(red, read(gl, 13, 18))
        assertNear(black, read(gl, 10, 16))
        assertNear(black, read(gl, 16, 21))
    }

    @Test
    fun attenuatedPointsShrinkWithDistanceUnderPerspective() = withRenderer { renderer, gl ->
        val material = PointsMaterial(0xff0000).apply { size = 4.0 }
        val points = Points(point(), material)
        val scene = Scene()
        scene.add(points)
        val camera = PerspectiveCamera(near = 0.1, far = 100.0).apply { position.z = 2.0 }
        // scale = 16 (half of the 32 px height), so the size is 4 × 16 / 2 = 32 px: the whole view.
        renderer.render(scene, camera)
        assertNear(red, read(gl, 1, 1))
        // At a distance of 16 the point is 4 px wide.
        camera.position.z = 16.0
        renderer.render(scene, camera)
        assertNear(red, read(gl, 16, 16))
        assertNear(black, read(gl, 10, 16))
    }

    @Test
    fun interleavedPointsRespectOffsetsAndUpdateAllViews() = withRenderer { renderer, gl ->
        val data = floatArrayOf(-0.5f, 0f, 0f, 1f, 0f, 0f, 0.5f, 0f, 0f, 0f, 0f, 1f)
        val buffer = InterleavedBuffer(data, 6).setUsage(Usage.DynamicDraw)
        val positions = InterleavedBufferAttribute(buffer, 3, 0)
        val color = InterleavedBufferAttribute(buffer, 3, 3)
        val geometry = BufferGeometry().setAttribute("position", positions).setAttribute("color", color)
        val material = PointsMaterial().apply { size = 4.0; sizeAttenuation = false; vertexColors = true }
        val scene = Scene().apply { add(Points(geometry, material)) }
        val camera = OrthographicCamera().apply { position.z = 1.0 }

        renderer.render(scene, camera)
        assertNear(red, read(gl, 8, 16))
        assertNear(listOf(0, 0, 255, 255), read(gl, 24, 16))

        data[6] = 0f
        data[10] = 1f
        data[11] = 0f
        color.markUpdated()
        assertEquals(1, buffer.version)
        renderer.render(scene, camera)
        assertNear(red, read(gl, 8, 16))
        assertNear(black, read(gl, 24, 16))
        assertNear(listOf(0, 255, 0, 255), read(gl, 16, 16))
    }

    @Test
    fun pointMapsSpanEachPoint() = withRenderer { renderer, gl ->
        // Left half red, right half blue.
        val source: dynamic = js("document.createElement('canvas')")
        source.width = 2
        source.height = 1
        val drawing = source.getContext("2d")
        drawing.fillStyle = "#ff0000"
        drawing.fillRect(0, 0, 1, 1)
        drawing.fillStyle = "#0000ff"
        drawing.fillRect(1, 0, 1, 1)
        val material = PointsMaterial().apply { size = 32.0; sizeAttenuation = false; map = ImageTexture(source) }
        val scene = Scene()
        scene.add(Points(point(), material))
        renderer.render(scene, OrthographicCamera().apply { position.z = 1.0 })
        assertNear(red, read(gl, 2, 16), tolerance = 8)
        assertNear(listOf(0, 0, 255, 255), read(gl, 29, 16), tolerance = 8)
    }

    @Test
    fun linearFogBlendsInSrgbBetweenNearAndFar() = withRenderer { renderer, gl ->
        val material = MeshBasicMaterial(Color(0.0, 0.0, 0.0))
        val quad = BufferGeometry().setAttribute(
            "position", Float32BufferAttribute(floatArrayOf(-9f, -9f, 0f, 9f, -9f, 0f, 0f, 9f, 0f), 3),
        )
        val scene = Scene()
        scene.add(Mesh(quad, material))
        scene.fog = Fog(Color(1.0, 1.0, 1.0), 1.0, 3.0)
        val camera = PerspectiveCamera(near = 0.1, far = 10.0)

        camera.position.z = 0.5
        renderer.render(scene, camera)
        assertNear(black, read(gl, 16, 16))
        // Halfway between near and far, smoothstep gives 0.5.
        camera.position.z = 2.0
        renderer.render(scene, camera)
        assertNear(listOf(128, 128, 128, 255), read(gl, 16, 16))
        camera.position.z = 4.0
        renderer.render(scene, camera)
        assertNear(listOf(255, 255, 255, 255), read(gl, 16, 16))

        material.fog = false
        renderer.render(scene, camera)
        assertNear(black, read(gl, 16, 16))
    }
}
