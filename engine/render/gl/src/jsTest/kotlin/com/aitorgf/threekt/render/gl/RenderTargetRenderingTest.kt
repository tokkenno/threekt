package com.aitorgf.threekt.render.gl

import com.aitorgf.threekt.cameras.OrthographicCamera
import com.aitorgf.threekt.geometries.PlaneGeometry
import com.aitorgf.threekt.materials.MeshBasicMaterial
import com.aitorgf.threekt.math.Color
import com.aitorgf.threekt.objects.Mesh
import com.aitorgf.threekt.render.gl.webgl.WebGL2Context
import com.aitorgf.threekt.scenes.Scene
import com.aitorgf.threekt.textures.DataTexture
import com.aitorgf.threekt.render.gl.textures.FramebufferTexture
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RenderTargetRenderingTest {
    @Test
    fun floatRenderTargetStoresLinearColorsAndSurvivesResize() {
        val canvas: dynamic = js("document.createElement('canvas')")
        val context = WebGL2Context(canvas)
        val gl = context.gl
        val renderer = WebGLRenderer(context)
        val target = WebGLRenderTarget(8, 8, RenderTargetType.Float, sampleDepth = true)
        val camera = OrthographicCamera(-1.0, 1.0, 1.0, -1.0, 0.0, 2.0).apply { position.z = 1.0 }
        val scene = Scene()
        val material = MeshBasicMaterial(Color(0.25, 0.5, 0.75))
        scene.add(Mesh(PlaneGeometry(2.0, 2.0), material))
        try {
            renderer.setSize(16, 16)
            renderer.setRenderTarget(target)
            renderer.render(scene, camera)
            val result = FloatArray(4)
            renderer.readRenderTargetPixels(target, 4, 4, 1, 1, result)
            assertEquals(0.25f, result[0], 0.0001f)
            assertEquals(0.5f, result[1], 0.0001f)
            assertEquals(0.75f, result[2], 0.0001f)
            target.setSize(4, 4)
            renderer.render(scene, camera)
            renderer.readRenderTargetPixels(target, 2, 2, 1, 1, result)
            assertEquals(0.25f, result[0], 0.0001f)
            renderer.setRenderTarget(null)
            material.map = target.texture
            material.color.set(1.0, 1.0, 1.0)
            renderer.render(scene, camera)
            val pixels: dynamic = js("new Uint8Array(4)")
            gl.readPixels(8, 8, 1, 1, gl.RGBA, gl.UNSIGNED_BYTE, pixels)
            assertTrue(pixels[0].unsafeCast<Int>() in 136..138)
            assertEquals(gl.NO_ERROR, gl.getError())
        } finally {
            renderer.dispose()
        }
    }

    @Test
    fun framebufferAndTextureCopiesPreserveTheUpdatedTexels() {
        val canvas: dynamic = js("document.createElement('canvas')")
        val context = WebGL2Context(canvas)
        val gl = context.gl
        val renderer = WebGLRenderer(context)
        val camera = OrthographicCamera(-1.0, 1.0, 1.0, -1.0, 0.0, 2.0).apply { position.z = 1.0 }
        val scene = Scene()
        val material = MeshBasicMaterial()
        val quad = Mesh(PlaneGeometry(2.0, 2.0), material)
        scene.add(quad)
        try {
            renderer.setSize(8, 8)
            val red = DataTexture(byteArrayOf(-1, 0, 0, -1), 1, 1)
            val copy = FramebufferTexture(1, 1)
            material.map = red
            renderer.render(scene, camera)
            renderer.copyFramebufferToTexture(copy, 4, 4)
            material.map = copy
            renderer.render(scene, camera)
            val pixels: dynamic = js("new Uint8Array(4)")
            gl.readPixels(4, 4, 1, 1, gl.RGBA, gl.UNSIGNED_BYTE, pixels)
            assertEquals(255, pixels[0].unsafeCast<Int>())
            assertEquals(0, pixels[1].unsafeCast<Int>())

            renderer.copyTextureToTexture(DataTexture(byteArrayOf(0, -1, 0, -1), 1, 1), copy, 0, 0)
            val second = MeshBasicMaterial(map = copy)
            quad.material = second
            renderer.render(scene, camera)
            gl.readPixels(4, 4, 1, 1, gl.RGBA, gl.UNSIGNED_BYTE, pixels)
            assertEquals(0, pixels[0].unsafeCast<Int>())
            assertEquals(255, pixels[1].unsafeCast<Int>())
            assertEquals(1, renderer.programCount)
            material.dispose()
            assertEquals(1, renderer.programCount)
            second.dispose()
            assertEquals(0, renderer.programCount)
            assertEquals(gl.NO_ERROR, gl.getError())
        } finally {
            renderer.dispose()
        }
    }
}
