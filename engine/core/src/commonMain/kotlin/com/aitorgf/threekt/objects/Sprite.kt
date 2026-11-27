package com.aitorgf.threekt.objects

import com.aitorgf.threekt.core.BufferGeometry
import com.aitorgf.threekt.core.InterleavedBuffer
import com.aitorgf.threekt.core.InterleavedBufferAttribute
import com.aitorgf.threekt.core.Object3D
import com.aitorgf.threekt.materials.SpriteMaterial
import com.aitorgf.threekt.math.Vector2
import com.aitorgf.threekt.math.Frustum
import com.aitorgf.threekt.cameras.PerspectiveCamera
import com.aitorgf.threekt.core.Intersection
import com.aitorgf.threekt.core.Raycaster
import com.aitorgf.threekt.math.Matrix4
import com.aitorgf.threekt.math.Triangle
import com.aitorgf.threekt.math.Vector3
import kotlin.math.cos
import kotlin.math.sin
import kotlin.js.JsExport

/**
 * A unit quad that always faces the camera, drawn with a [SpriteMaterial], as Three.js `Sprite`. Its X and
 * Y scale set its size in world units. Sprites share one interleaved quad geometry. They do not cast
 * shadows.
 */
@JsExport
class Sprite(var material: SpriteMaterial = SpriteMaterial()) : Object3D() {
    /** Shared quad with positions in `[-0.5, 0.5]` and UVs in `[0, 1]`. */
    val geometry: BufferGeometry get() = sharedGeometry

    /**
     * Point of the quad, in UV coordinates, placed at the sprite's position. `(0.5, 0.5)` centers it and
     * `(0, 0)` places its lower-left corner there.
     */
    val center = Vector2(0.5, 0.5)

    /** Copies [center] and shares the material of [source] when it is a sprite. Returns this sprite. */
    override fun copy(source: Object3D, recursive: Boolean): Sprite {
        super.copy(source, recursive)
        if (source is Sprite) {
            center.copy(source.center)
            material = source.material
        }
        return this
    }

    override fun clone(recursive: Boolean): Sprite = Sprite().copy(this, recursive)

    private companion object {
        val sharedGeometry: BufferGeometry by lazy {
            val buffer = InterleavedBuffer(
                floatArrayOf(
                    -0.5f, -0.5f, 0f, 0f, 0f,
                    0.5f, -0.5f, 0f, 1f, 0f,
                    0.5f, 0.5f, 0f, 1f, 1f,
                    -0.5f, 0.5f, 0f, 0f, 1f,
                ),
                5,
            )
            BufferGeometry()
                .setIndex(intArrayOf(0, 1, 2, 0, 2, 3))
                .setAttribute("position", InterleavedBufferAttribute(buffer, 3, 0, false))
                .setAttribute("uv", InterleavedBufferAttribute(buffer, 2, 3, false))
        }
    }

    /** Tests the bounding sphere of this sprite against [frustum], as the renderer does for culling. */
    @JsExport.Ignore
    override fun intersectsFrustum(frustum: Frustum): Boolean = frustum.intersectsSprite(this)

    /**
     * Adds a hit when the ray crosses the camera-facing quad of this sprite, with the UV at the hit point.
     * Requires [Raycaster.camera], which [Raycaster.setFromCamera] sets; throws [IllegalStateException]
     * without it, where Three.js logs an error and then fails.
     */
    override fun raycast(raycaster: Raycaster, intersects: MutableList<Intersection>): Boolean {
        val camera = checkNotNull(raycaster.camera) { "Raycaster.camera needs to be set in order to raycast against sprites." }
        worldScale.setFromMatrixScale(matrixWorld)
        viewWorldMatrix.copy(camera.matrixWorld)
        modelViewMatrix.multiplyMatrices(camera.matrixWorldInverse, matrixWorld)
        mvPosition.setFromMatrixPosition(modelViewMatrix)
        if (camera is PerspectiveCamera && !material.sizeAttenuation) worldScale.multiplyScalar(-mvPosition.z)
        val rotation = material.rotation
        val rotated = rotation != 0.0
        val sin = if (rotated) sin(rotation) else 0.0
        val cos = if (rotated) cos(rotation) else 1.0
        transformVertex(vA.set(-0.5, -0.5, 0.0), center, sin, cos)
        transformVertex(vB.set(0.5, -0.5, 0.0), center, sin, cos)
        transformVertex(vC.set(0.5, 0.5, 0.0), center, sin, cos)
        uvA.set(0.0, 0.0)
        uvB.set(1.0, 0.0)
        uvC.set(1.0, 1.0)
        var intersect = raycaster.ray.intersectTriangle(vA, vB, vC, false, intersectPoint)
        if (intersect == null) {
            transformVertex(vB.set(-0.5, 0.5, 0.0), center, sin, cos)
            uvB.set(0.0, 1.0)
            intersect = raycaster.ray.intersectTriangle(vA, vC, vB, false, intersectPoint)
            if (intersect == null) return true
        }
        val distance = raycaster.ray.origin.distanceTo(intersectPoint)
        if (distance < raycaster.near || distance > raycaster.far) return true
        intersects.add(Intersection(distance, intersectPoint.copy(), this).also {
            it.uv = Triangle.getInterpolation(intersectPoint, vA, vB, vC, uvA, uvB, uvC, Vector2())
        })
        return true
    }

    private fun transformVertex(vertexPosition: Vector3, center: Vector2, sin: Double, cos: Double) {
        alignedPosition.set(vertexPosition.x - center.x + 0.5, vertexPosition.y - center.y + 0.5)
        alignedPosition.x *= worldScale.x
        alignedPosition.y *= worldScale.y
        val x = cos * alignedPosition.x - sin * alignedPosition.y
        val y = sin * alignedPosition.x + cos * alignedPosition.y
        vertexPosition.copy(mvPosition)
        vertexPosition.x += x
        vertexPosition.y += y
        vertexPosition.applyMatrix4(viewWorldMatrix)
    }

    private val worldScale = Vector3()
    private val mvPosition = Vector3()
    private val alignedPosition = Vector2()
    private val viewWorldMatrix = Matrix4()
    private val vA = Vector3()
    private val vB = Vector3()
    private val vC = Vector3()
    private val uvA = Vector2()
    private val uvB = Vector2()
    private val uvC = Vector2()
    private val intersectPoint = Vector3()
}
