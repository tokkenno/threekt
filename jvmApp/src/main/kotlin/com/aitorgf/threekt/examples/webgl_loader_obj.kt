package com.aitorgf.threekt.examples

import com.aitorgf.threekt.cameras.PerspectiveCamera
import com.aitorgf.threekt.controls.InputSurface
import com.aitorgf.threekt.controls.OrbitControls
import com.aitorgf.threekt.lights.AmbientLight
import com.aitorgf.threekt.lights.DirectionalLight
import com.aitorgf.threekt.loaders.MTLLoader
import com.aitorgf.threekt.loaders.OBJLoader
import com.aitorgf.threekt.renderers.WebGLRenderer
import com.aitorgf.threekt.renderers.webgl.GLContext
import com.aitorgf.threekt.scenes.Scene
import com.aitorgf.threekt.textures.Texture
import java.io.InputStream
import javax.imageio.ImageIO

/** Loads the original OBJ, MTL and diffuse maps using the same scene and orbit controls as the web port. */
class webgl_loader_obj(context: GLContext, input: InputSurface) : Example {
    private val camera = PerspectiveCamera(45.0, 1.0, 0.1, 20.0).apply { position.z = 2.5 }
    private val scene = Scene()
    private val renderer: WebGLRenderer
    private val controls: OrbitControls

    init {
        val materials = MTLLoader().parse(readText("male02.mtl"))
        for (file in materials.textureFiles) {
            val bitmap = openResource(file).use { checkNotNull(ImageIO.read(it)) { "Invalid image: $file" } }
            materials.setTexture(file, object : Texture() { override val image = bitmap })
        }
        val model = OBJLoader().setMaterials(materials).parse(readText("male02.obj"))
        model.position.y = -0.95
        model.scale.set(0.01, 0.01, 0.01)

        scene.add(AmbientLight(0xffffff))
        scene.add(DirectionalLight(0xffffff, 2.0).apply { position.set(1.0, 1.0, 2.0) })
        scene.add(model)

        renderer = WebGLRenderer(context)
        controls = OrbitControls(camera, input).apply {
            enableDamping = true
            minDistance = 2.0
            maxDistance = 5.0
        }
    }

    override fun resize(width: Int, height: Int, pixelRatio: Double) {
        camera.aspect = width.toDouble() / height
        camera.updateProjectionMatrix()
        renderer.setSize(width, height, pixelRatio)
    }

    override fun render(seconds: Double) {
        controls.update()
        renderer.render(scene, camera)
    }

    override fun dispose() {
        controls.dispose()
        renderer.dispose()
    }

    private fun readText(file: String): String = openResource(file).bufferedReader().use { it.readText() }

    private fun openResource(file: String): InputStream =
        checkNotNull(javaClass.getResourceAsStream("/models/male02/$file")) { "Missing OBJ resource: $file" }
}
