package com.aitorgf.threekt.examples

import com.aitorgf.threekt.cameras.PerspectiveCamera
import com.aitorgf.threekt.core.BufferGeometry
import com.aitorgf.threekt.core.Float32BufferAttribute
import com.aitorgf.threekt.core.Uint8BufferAttribute
import com.aitorgf.threekt.core.Uniform
import com.aitorgf.threekt.render.gl.materials.RawShaderMaterial
import com.aitorgf.threekt.materials.Side
import com.aitorgf.threekt.math.Color
import com.aitorgf.threekt.math.Euler
import com.aitorgf.threekt.objects.Mesh
import com.aitorgf.threekt.render.gl.WebGLRenderer
import com.aitorgf.threekt.render.gl.webgl.GLContext
import com.aitorgf.threekt.scenes.Scene
import kotlin.random.Random

/** Port of Three.js examples/webgl_buffergeometry_rawshader.html, without the Stats panel. */
class WebglBuffergeometryRawshader(context: GLContext) : Example {
    private val camera = PerspectiveCamera(50.0, 1.0, 1.0, 10.0).apply { position.z = 2.0 }
    private val scene = Scene().apply { background = Color(0x101010) }
    private val time = Uniform(1.0)
    private val mesh: Mesh
    private val rotation = Euler()
    private val renderer = WebGLRenderer(context)

    init {
        // Number of triangles times three vertices per triangle.
        val vertexCount = 200 * 3
        val positions = FloatArray(vertexCount * 3) { Random.nextFloat() - 0.5f }
        val colors = ByteArray(vertexCount * 4) { (Random.nextDouble() * 255).toInt().toByte() }

        val geometry = BufferGeometry()
        geometry.setAttribute("position", Float32BufferAttribute(positions, 3))
        // Normalized bytes map to 0.0–1.0 in the shader.
        geometry.setAttribute("color", Uint8BufferAttribute(colors, 4, normalized = true))

        val material = RawShaderMaterial(VERTEX_SHADER, FRAGMENT_SHADER)
        material.uniforms.set("time", time)
        material.side = Side.Double
        material.transparent = true

        mesh = Mesh(geometry, material)
        scene.add(mesh)
    }

    override fun resize(width: Int, height: Int, pixelRatio: Double) {
        camera.aspect = width.toDouble() / height
        camera.updateProjectionMatrix()
        renderer.setSize(width, height, pixelRatio)
    }

    override fun render(seconds: Double) {
        val milliseconds = seconds * 1000
        mesh.quaternion.setFromEuler(rotation.set(0.0, milliseconds * 0.0005, 0.0))
        time.value = milliseconds * 0.005
        renderer.render(scene, camera)
    }

    override fun dispose() {
        renderer.dispose()
    }

    private companion object {
        val VERTEX_SHADER = """
            precision mediump float;
            precision mediump int;

            uniform mat4 modelViewMatrix; // optional
            uniform mat4 projectionMatrix; // optional

            attribute vec3 position;
            attribute vec4 color;

            varying vec3 vPosition;
            varying vec4 vColor;

            void main() {
                vPosition = position;
                vColor = color;
                gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);
            }
        """.trimIndent()

        val FRAGMENT_SHADER = """
            precision mediump float;
            precision mediump int;

            uniform float time;

            varying vec3 vPosition;
            varying vec4 vColor;

            void main() {
                vec4 color = vec4(vColor);
                color.r += sin(vPosition.x * 10.0 + time) * 0.5;
                gl_FragColor = color;
            }
        """.trimIndent()
    }
}
