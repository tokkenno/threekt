package com.aitorgf.threekt.examples

import com.aitorgf.threekt.cameras.PerspectiveCamera
import com.aitorgf.threekt.controls.InputListener
import com.aitorgf.threekt.controls.InputSurface
import com.aitorgf.threekt.controls.KeyInput
import com.aitorgf.threekt.controls.PointerInput
import com.aitorgf.threekt.controls.WheelInput
import com.aitorgf.threekt.core.Intersection
import com.aitorgf.threekt.core.Raycaster
import com.aitorgf.threekt.geometries.BoxGeometry
import com.aitorgf.threekt.geometries.PlaneGeometry
import com.aitorgf.threekt.helpers.GridHelper
import com.aitorgf.threekt.lights.AmbientLight
import com.aitorgf.threekt.lights.DirectionalLight
import com.aitorgf.threekt.materials.MeshBasicMaterial
import com.aitorgf.threekt.materials.MeshPhongMaterial
import com.aitorgf.threekt.math.Color
import com.aitorgf.threekt.math.Euler
import com.aitorgf.threekt.math.Vector2
import com.aitorgf.threekt.math.Vector3
import com.aitorgf.threekt.objects.Mesh
import com.aitorgf.threekt.render.gl.WebGLRenderer
import com.aitorgf.threekt.render.gl.webgl.GLContext
import com.aitorgf.threekt.scenes.Scene
import com.aitorgf.threekt.textures.Texture
import javax.imageio.ImageIO
import kotlin.math.PI

/** Desktop voxel editor. Click to place a voxel or Shift-click to remove it. */
class webgl_interactive_voxelpainter(context: GLContext, private val input: InputSurface) : Example, InputListener {
    private val scene = Scene().apply { background = Color(0xf0f0f0) }
    private val camera = PerspectiveCamera(45.0, 1.0, 1.0, 10000.0)
    private val box = BoxGeometry(50.0, 50.0, 50.0)
    private val preview = Mesh(box, MeshBasicMaterial(Color(0xff0000)).apply {
        opacity = 0.5
        transparent = true
    }).apply { visible = false }
    private val voxelMaterial = MeshPhongMaterial(Color(0xfeb74c)).apply {
        specular.setHex(0)
        val image = checkNotNull(javaClass.getResourceAsStream("/textures/square-outline-textured.png"))
            .use { checkNotNull(ImageIO.read(it)) { "Invalid voxel texture." } }
        map = object : Texture() { override val image = image }
    }
    private val plane = Mesh(PlaneGeometry(1000.0, 1000.0), MeshBasicMaterial()).apply {
        quaternion.setFromEuler(Euler(-PI / 2, 0.0, 0.0))
        visible = false
    }
    private val objects = mutableListOf(plane)
    private val raycaster = Raycaster()
    private val pointer = Vector2()
    private val snap = Vector3()
    private val up = Vector3(0.0, 1.0, 0.0)
    private val renderer = WebGLRenderer(context)

    init {
        scene.add(preview)
        scene.add(plane)
        scene.add(GridHelper(1000.0, 20))
        scene.add(AmbientLight(0x606060, 3.0))
        scene.add(DirectionalLight(0xffffff, 3.0).apply {
            position.set(1.0, 0.75, 0.5).normalize()
        })
        camera.position.set(500.0, 800.0, 1300.0)
        camera.lookAt(scene.position)
        input.addInputListener(this)
    }

    override fun resize(width: Int, height: Int, pixelRatio: Double) {
        camera.aspect = width.toDouble() / height
        camera.updateProjectionMatrix()
        renderer.setSize(width, height, pixelRatio)
    }

    override fun render(seconds: Double) {
        renderer.render(scene, camera)
    }

    override fun dispose() {
        input.removeInputListener(this)
        renderer.dispose()
    }

    override fun onPointerMove(input: PointerInput) {
        updatePreview(intersect(input))
    }

    override fun onPointerDown(input: PointerInput) {
        if (input.button != 0) return
        val hit = intersect(input) ?: return
        if (input.shiftKey) {
            if (hit.obj === plane) return
            val voxel = hit.obj as Mesh
            scene.remove(voxel)
            objects.remove(voxel)
        } else {
            val voxel = Mesh(box, voxelMaterial)
            voxel.position.copy(snap(hit))
            objects.add(voxel)
            scene.add(voxel)
        }
        updatePreview(intersect(input))
    }

    override fun onPointerUp(input: PointerInput) = Unit
    override fun onWheel(input: WheelInput) = false
    override fun onContextMenu() = false
    override fun onKeyDown(input: KeyInput) = false

    private fun intersect(event: PointerInput): Intersection? {
        val x = event.x - input.left
        val y = event.y - input.top
        if (x < 0.0 || y < 0.0 || x >= input.width || y >= input.height) return null
        pointer.set(2 * x / input.width - 1, 1 - 2 * y / input.height)
        raycaster.setFromCamera(pointer, camera)
        return raycaster.intersectObjects(objects.toTypedArray(), recursive = false).firstOrNull()
    }

    private fun snap(hit: Intersection): Vector3 {
        snap.copy(hit.point)
        val normal = if (hit.obj === plane) up else hit.face?.normal
        if (normal != null) snap.add(normal)
        return snap.divideScalar(50.0).floor().multiplyScalar(50.0).addScalar(25.0)
    }

    private fun updatePreview(hit: Intersection?) {
        preview.visible = hit != null
        if (hit != null) preview.position.copy(snap(hit))
    }
}
