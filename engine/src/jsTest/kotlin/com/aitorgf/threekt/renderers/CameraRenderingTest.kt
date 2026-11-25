package com.aitorgf.threekt.renderers

import com.aitorgf.threekt.cameras.ArrayCamera
import com.aitorgf.threekt.cameras.CubeCamera
import com.aitorgf.threekt.cameras.PerspectiveCamera
import com.aitorgf.threekt.geometries.Box
import com.aitorgf.threekt.materials.MeshBasicMaterial
import com.aitorgf.threekt.math.Color
import com.aitorgf.threekt.objects.Mesh
import com.aitorgf.threekt.renderers.webgl.WebGL2Context
import com.aitorgf.threekt.scenes.Scene
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class CameraRenderingTest {
    @Test
    fun webGLRespectsWebGpuClipDepthAndReversedDepth() {
        val gl = createContext()
        val renderer = WebGLRenderer(WebGL2Context(gl.canvas))
        try {
            val scene = Scene()
            scene.add(Mesh(Box(), MeshBasicMaterial(0xff0000)).apply { position.z = -2.0 })
            scene.add(Mesh(Box(), MeshBasicMaterial(0x00ff00)).apply { position.z = -4.0 })
            val nearOccluder = Mesh(Box(0.25, 0.25, 0.25), MeshBasicMaterial(0xffff00))
            nearOccluder.position.z = -0.75
            scene.add(nearOccluder)
            val camera = PerspectiveCamera(near = 1.0, far = 10.0)

            for (system in CoordinateSystem.entries) {
                camera.coordinateSystem = system
                for (reversed in listOf(false, true)) {
                    camera.reversedDepth = reversed
                    camera.updateProjectionMatrix()
                    renderer.render(scene, camera)
                    assertEquals(0xff0000, readRgb(gl, 32, 16), "$system reversedDepth=$reversed")
                }
            }
            assertEquals(gl.NO_ERROR, gl.getError())
        } finally {
            renderer.dispose()
        }
    }

    @Test
    fun cubeCaptureRestoresTheViewportAndFramebufferEvenWhenTheTargetIsDisposed() {
        val gl = createContext()
        val context = WebGL2Context(gl.canvas)
        val renderer = WebGLRenderer(context)
        val framebuffer = gl.createFramebuffer()
        val target = WebGLCubeRenderTarget(context, 16)
        try {
            gl.bindFramebuffer(gl.DRAW_FRAMEBUFFER, framebuffer)
            gl.viewport(3, 5, 19, 23)
            val scene = Scene()
            val camera = CubeCamera()

            renderer.renderCube(scene, camera, target)
            assertEquals(framebuffer, gl.getParameter(gl.DRAW_FRAMEBUFFER_BINDING))
            assertEquals(listOf(3, 5, 19, 23), readViewport(gl))

            target.dispose()
            assertFalse(gl.isTexture(target.texture).unsafeCast<Boolean>())
            assertFailsWith<IllegalStateException> { renderer.renderCube(scene, camera, target) }
            assertEquals(framebuffer, gl.getParameter(gl.DRAW_FRAMEBUFFER_BINDING))
            assertEquals(listOf(3, 5, 19, 23), readViewport(gl))
            assertEquals(gl.NO_ERROR, gl.getError())
        } finally {
            gl.deleteFramebuffer(framebuffer)
            target.dispose()
            renderer.dispose()
        }
    }

    @Test
    fun webGLRendersDistinctCubeFacesAndArrayViewports() {
        val gl = createContext()
        val context = WebGL2Context(gl.canvas)
        val renderer = WebGLRenderer(context)
        val target = WebGLCubeRenderTarget(context, 32)
        try {
            val scene = Scene()
            scene.add(Mesh(Box(), MeshBasicMaterial(0xff0000)).apply { position.x = 3.0 })
            scene.add(Mesh(Box(), MeshBasicMaterial(0x00ff00)).apply { position.z = -3.0 })
            val cube = CubeCamera()
            val positiveX = CubeCamera.Face.PositiveX
            val negativeZ = CubeCamera.Face.NegativeZ

            renderer.renderCube(scene, cube, target)
            assertEquals(null, gl.getParameter(gl.DRAW_FRAMEBUFFER_BINDING))
            for ((face, expected) in listOf(positiveX to 0xff0000, negativeZ to 0x00ff00)) {
                target.bindFace(face)
                gl.bindFramebuffer(gl.READ_FRAMEBUFFER, gl.getParameter(gl.DRAW_FRAMEBUFFER_BINDING))
                assertEquals(expected, readRgb(gl, 16, 16))
            }

            gl.bindFramebuffer(gl.FRAMEBUFFER, null)
            val array = ArrayCamera(listOf(
                ArrayCamera.View(cube.getCamera(positiveX), ArrayCamera.Viewport(0, 0, 32, 32)),
                ArrayCamera.View(cube.getCamera(negativeZ), ArrayCamera.Viewport(32, 0, 32, 32)),
            ))
            renderer.render(scene, array)
            assertEquals(0xff0000, readRgb(gl, 16, 16))
            assertEquals(0x00ff00, readRgb(gl, 48, 16))
            assertEquals(gl.NO_ERROR, gl.getError())
        } finally {
            target.dispose()
            renderer.dispose()
        }
    }

    @Test
    fun scissorRestrictsClearsInCssPixelsAndRestoresFullDrawingWhenDisabled() {
        val gl = createContext()
        val renderer = WebGLRenderer(WebGL2Context(gl.canvas))
        try {
            renderer.setSize(32, 16, 2.0)
            val scene = Scene().apply { background = Color(0xff0000) }
            val camera = PerspectiveCamera()
            renderer.render(scene, camera)
            renderer.setScissor(16, 0, 16, 16)
            renderer.setScissorTest(true)

            scene.background = Color(0x0000ff)
            renderer.render(scene, camera)
            assertEquals(0xff0000, readRgb(gl, 31, 16))
            assertEquals(0x0000ff, readRgb(gl, 32, 16))

            renderer.setScissorTest(false)
            renderer.render(scene, camera)
            assertEquals(0x0000ff, readRgb(gl, 31, 16))
            assertEquals(gl.NO_ERROR, gl.getError())
        } finally {
            renderer.dispose()
        }
    }

    private fun createContext(): dynamic {
        val canvas: dynamic = js("document.createElement('canvas')")
        canvas.width = 64
        canvas.height = 32
        return assertNotNull(canvas.getContext("webgl2"), "A WebGL2 context is required.")
    }

    private fun readViewport(gl: dynamic): List<Int> {
        val viewport = gl.getParameter(gl.VIEWPORT)
        return List(4) { index -> viewport[index].unsafeCast<Int>() }
    }

    private fun readRgb(gl: dynamic, x: Int, y: Int): Int {
        val pixels: dynamic = js("new Uint8Array(4)")
        gl.readPixels(x, y, 1, 1, gl.RGBA, gl.UNSIGNED_BYTE, pixels)
        val red = pixels[0].unsafeCast<Int>()
        val green = pixels[1].unsafeCast<Int>()
        val blue = pixels[2].unsafeCast<Int>()
        return (red shl 16) or (green shl 8) or blue
    }
}
