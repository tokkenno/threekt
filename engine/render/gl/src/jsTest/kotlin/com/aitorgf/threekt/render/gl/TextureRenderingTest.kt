package com.aitorgf.threekt.render.gl

import com.aitorgf.threekt.cameras.PerspectiveCamera
import com.aitorgf.threekt.geometries.BoxGeometry
import com.aitorgf.threekt.materials.MeshBasicMaterial
import com.aitorgf.threekt.objects.Mesh
import com.aitorgf.threekt.render.gl.webgl.WebGL2Context
import com.aitorgf.threekt.scenes.Scene
import com.aitorgf.threekt.textures.ImageTexture
import com.aitorgf.threekt.textures.Wrapping
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TextureRenderingTest {
    @Test
    fun basicMaterialUsesItsTextureAndCanSwitchBackToSolidColor() {
        val canvas: dynamic = js("document.createElement('canvas')")
        canvas.width = 32
        canvas.height = 32
        val gl = assertNotNull(canvas.getContext("webgl2"), "A WebGL2 context is required.")
        val source: dynamic = js("document.createElement('canvas')")
        source.width = 2
        source.height = 2
        val drawing = source.getContext("2d")
        drawing.fillStyle = "#ff0000"
        drawing.fillRect(0, 0, 2, 2)

        val renderer = WebGLRenderer(WebGL2Context(gl.canvas))
        try {
            val scene = Scene()
            val camera = PerspectiveCamera().apply { position.z = 3.0 }
            val texture = ImageTexture(source)
            val material = MeshBasicMaterial(map = texture)
            scene.add(Mesh(BoxGeometry(), material))
            renderer.render(scene, camera)
            val pixels: dynamic = js("new Uint8Array(4)")
            gl.readPixels(16, 16, 1, 1, gl.RGBA, gl.UNSIGNED_BYTE, pixels)
            assertEquals(255, pixels[0].unsafeCast<Int>())
            assertEquals(0, pixels[1].unsafeCast<Int>())

            drawing.fillStyle = "#0000ff"
            drawing.fillRect(0, 0, 2, 2)
            renderer.render(scene, camera)
            gl.readPixels(16, 16, 1, 1, gl.RGBA, gl.UNSIGNED_BYTE, pixels)
            assertEquals(255, pixels[0].unsafeCast<Int>())
            assertEquals(0, pixels[2].unsafeCast<Int>())
            texture.markUpdated()
            renderer.render(scene, camera)
            gl.readPixels(16, 16, 1, 1, gl.RGBA, gl.UNSIGNED_BYTE, pixels)
            assertEquals(0, pixels[0].unsafeCast<Int>())
            assertEquals(255, pixels[2].unsafeCast<Int>())

            material.map = null
            material.color.setHex(0x00ff00)
            renderer.render(scene, camera)
            gl.readPixels(16, 16, 1, 1, gl.RGBA, gl.UNSIGNED_BYTE, pixels)
            assertEquals(0, pixels[0].unsafeCast<Int>())
            assertEquals(255, pixels[1].unsafeCast<Int>())
            assertEquals(gl.NO_ERROR, gl.getError())
        } finally {
            renderer.dispose()
        }
    }

    @Test
    fun uvOffsetAndWrappingSelectTheSampledTexel() {
        val canvas: dynamic = js("document.createElement('canvas')")
        canvas.width = 32
        canvas.height = 32
        val gl = assertNotNull(canvas.getContext("webgl2"), "A WebGL2 context is required.")
        // Left texel red, right texel blue.
        val source: dynamic = js("document.createElement('canvas')")
        source.width = 2
        source.height = 1
        val drawing = source.getContext("2d")
        drawing.fillStyle = "#ff0000"
        drawing.fillRect(0, 0, 1, 1)
        drawing.fillStyle = "#0000ff"
        drawing.fillRect(1, 0, 1, 1)

        val renderer = WebGLRenderer(WebGL2Context(canvas))
        try {
            val scene = Scene()
            val camera = PerspectiveCamera().apply { position.z = 3.0 }
            val texture = ImageTexture(source)
            texture.anisotropy = renderer.capabilities.getMaxAnisotropy()
            scene.add(Mesh(BoxGeometry(), MeshBasicMaterial(map = texture)))
            val pixels: dynamic = js("new Uint8Array(4)")
            fun sample(): Pair<Int, Int> {
                renderer.render(scene, camera)
                gl.readPixels(16, 16, 1, 1, gl.RGBA, gl.UNSIGNED_BYTE, pixels)
                return pixels[0].unsafeCast<Int>() to pixels[2].unsafeCast<Int>()
            }

            texture.offset.x = 0.25
            val (shiftedRed, shiftedBlue) = sample()
            assertTrue(shiftedBlue > 200 && shiftedRed < 50, "Offset should sample the right texel.")

            texture.offset.x = 0.75
            val (clampedRed, clampedBlue) = sample()
            assertTrue(clampedBlue > 200 && clampedRed < 50, "Clamping should keep the edge texel.")

            texture.wrapS = Wrapping.Repeat
            val (repeatedRed, repeatedBlue) = sample()
            // The sample lies close to the wrapped edge, so linear filtering blends in a little of the
            // right texel, which the sRGB output encoding amplifies.
            assertTrue(repeatedRed > 200 && repeatedBlue < 100, "Repeat wrapping should restart at the left texel.")
            assertEquals(gl.NO_ERROR, gl.getError())
        } finally {
            renderer.dispose()
        }
    }

    @Test
    fun maxAnisotropyIsAtLeastOne() {
        val canvas: dynamic = js("document.createElement('canvas')")
        val renderer = WebGLRenderer(WebGL2Context(canvas))
        try {
            assertTrue(renderer.capabilities.getMaxAnisotropy() >= 1)
        } finally {
            renderer.dispose()
        }
    }
}
