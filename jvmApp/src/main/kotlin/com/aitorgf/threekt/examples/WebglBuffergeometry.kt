package com.aitorgf.threekt.examples

import com.aitorgf.threekt.cameras.PerspectiveCamera
import com.aitorgf.threekt.core.BufferGeometry
import com.aitorgf.threekt.core.Float32BufferAttribute
import com.aitorgf.threekt.lights.AmbientLight
import com.aitorgf.threekt.lights.DirectionalLight
import com.aitorgf.threekt.materials.MeshPhongMaterial
import com.aitorgf.threekt.materials.Side
import com.aitorgf.threekt.math.Color
import com.aitorgf.threekt.math.Euler
import com.aitorgf.threekt.math.Vector3
import com.aitorgf.threekt.objects.Mesh
import com.aitorgf.threekt.renderers.WebGLRenderer
import com.aitorgf.threekt.renderers.webgl.GLContext
import com.aitorgf.threekt.scenes.Fog
import com.aitorgf.threekt.scenes.Scene
import kotlin.random.Random

/**
 * Port of Three.js examples/webgl_buffergeometry.html, without the Stats panel. Threekt keeps attribute
 * arrays after upload, so the upstream `onUpload( disposeArray )` calls are omitted.
 */
class WebglBuffergeometry(context: GLContext) : Example {
    private val camera = PerspectiveCamera(27.0, 1.0, 1.0, 3500.0).apply { position.z = 2750.0 }
    private val scene = Scene()
    private val mesh: Mesh
    private val rotation = Euler()
    private val renderer = WebGLRenderer(context)

    init {
        scene.background = Color(0x050505)
        scene.fog = Fog(Color(0x050505), 2000.0, 3500.0)

        //

        scene.add(AmbientLight(0xcccccc))

        val light1 = DirectionalLight(0xffffff, 1.5)
        light1.position.set(1.0, 1.0, 1.0)
        scene.add(light1)

        val light2 = DirectionalLight(0xffffff, 4.5)
        light2.position.set(0.0, -1.0, 0.0)
        scene.add(light2)

        //

        val triangles = 160000

        val geometry = BufferGeometry()

        val positions = FloatArray(triangles * 3 * 3)
        val normals = FloatArray(triangles * 3 * 3)
        val colors = FloatArray(triangles * 3 * 4)

        val color = Color()

        val n = 800.0
        val n2 = n / 2 // triangles spread in the cube
        val d = 12.0
        val d2 = d / 2 // individual triangle size

        val pA = Vector3()
        val pB = Vector3()
        val pC = Vector3()

        val cb = Vector3()
        val ab = Vector3()

        fun random() = Random.nextDouble()

        for (i in 0 until triangles) {
            // positions

            val x = random() * n - n2
            val y = random() * n - n2
            val z = random() * n - n2

            val ax = x + random() * d - d2
            val ay = y + random() * d - d2
            val az = z + random() * d - d2

            val bx = x + random() * d - d2
            val by = y + random() * d - d2
            val bz = z + random() * d - d2

            val cx = x + random() * d - d2
            val cy = y + random() * d - d2
            val cz = z + random() * d - d2

            doubleArrayOf(ax, ay, az, bx, by, bz, cx, cy, cz).forEachIndexed { k, value -> positions[i * 9 + k] = value.toFloat() }

            // flat face normals

            pA.set(ax, ay, az)
            pB.set(bx, by, bz)
            pC.set(cx, cy, cz)

            cb.subVectors(pC, pB)
            ab.subVectors(pA, pB)
            cb.cross(ab)

            cb.normalize()

            for (vertex in 0 until 3) {
                normals[i * 9 + vertex * 3] = cb.x.toFloat()
                normals[i * 9 + vertex * 3 + 1] = cb.y.toFloat()
                normals[i * 9 + vertex * 3 + 2] = cb.z.toFloat()
            }

            // colors

            val vx = (x / n) + 0.5
            val vy = (y / n) + 0.5
            val vz = (z / n) + 0.5

            color.setRGB(vx, vy, vz)

            val alpha = random().toFloat()

            for (vertex in 0 until 3) {
                colors[i * 12 + vertex * 4] = color.r.toFloat()
                colors[i * 12 + vertex * 4 + 1] = color.g.toFloat()
                colors[i * 12 + vertex * 4 + 2] = color.b.toFloat()
                colors[i * 12 + vertex * 4 + 3] = alpha
            }
        }

        geometry.setAttribute("position", Float32BufferAttribute(positions, 3))
        geometry.setAttribute("normal", Float32BufferAttribute(normals, 3))
        geometry.setAttribute("color", Float32BufferAttribute(colors, 4))

        geometry.computeBoundingSphere()

        val material = MeshPhongMaterial(0xd5d5d5)
        material.specular.setHex(0xffffff)
        material.shininess = 250.0
        material.side = Side.Double
        material.vertexColors = true
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
        val time = System.currentTimeMillis() * 0.001

        mesh.quaternion.setFromEuler(rotation.set(time * 0.25, time * 0.5, 0.0))

        renderer.render(scene, camera)
    }

    override fun dispose() {
        renderer.dispose()
    }
}
