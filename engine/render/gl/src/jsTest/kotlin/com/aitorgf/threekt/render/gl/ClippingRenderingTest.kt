package com.aitorgf.threekt.render.gl

import com.aitorgf.threekt.cameras.OrthographicCamera
import com.aitorgf.threekt.geometries.PlaneGeometry
import com.aitorgf.threekt.materials.MeshBasicMaterial
import com.aitorgf.threekt.math.Plane
import com.aitorgf.threekt.math.Vector3
import com.aitorgf.threekt.objects.Mesh
import com.aitorgf.threekt.render.gl.webgl.WebGL2Context
import com.aitorgf.threekt.scenes.Scene
import kotlin.test.Test
import kotlin.test.assertEquals

class ClippingRenderingTest {
    @Test
    fun combinesGlobalPlanesWithLocalUnionOrIntersection() {
        val canvas: dynamic = js("document.createElement('canvas')")
        val context = WebGL2Context(canvas)
        val gl = context.gl
        val renderer = WebGLRenderer(context)
        val camera = OrthographicCamera(-1.0, 1.0, 1.0, -1.0, 0.0, 2.0)
        camera.position.set(1.0, 0.0, 1.0)
        val material = MeshBasicMaterial()
        val scene = Scene()
        scene.add(Mesh(PlaneGeometry(2.0, 2.0), material).apply { position.x = 1.0 })

        fun redAt(x: Int, y: Int): Int {
            val pixels: dynamic = js("new Uint8Array(4)")
            gl.readPixels(x, y, 1, 1, gl.RGBA, gl.UNSIGNED_BYTE, pixels)
            return pixels[0].unsafeCast<Int>()
        }

        try {
            renderer.setSize(16, 16)
            material.clippingPlanes = arrayOf(Plane(Vector3(1.0, 0.0, 0.0), -1.0), Plane(Vector3(0.0, 1.0, 0.0)))
            renderer.clippingPlanes = arrayOf(Plane(Vector3(-1.0, 0.0, 0.0), 1.75))
            renderer.render(scene, camera)
            assertEquals(255, redAt(4, 4))
            assertEquals(0, redAt(15, 8))

            renderer.localClippingEnabled = true
            renderer.render(scene, camera)
            assertEquals(0, redAt(4, 12))
            assertEquals(0, redAt(12, 4))
            assertEquals(255, redAt(12, 12))

            material.clipIntersection = true
            renderer.render(scene, camera)
            assertEquals(0, redAt(4, 4))
            assertEquals(255, redAt(4, 12))
            assertEquals(255, redAt(12, 4))
            assertEquals(0, redAt(15, 8))
            assertEquals(gl.NO_ERROR, gl.getError())
        } finally {
            renderer.dispose()
        }
    }
}
