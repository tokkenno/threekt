package com.aitorgf.threekt.objects

import com.aitorgf.threekt.core.BufferGeometry
import com.aitorgf.threekt.core.InterleavedBuffer
import com.aitorgf.threekt.core.InterleavedBufferAttribute
import com.aitorgf.threekt.core.Object3D
import com.aitorgf.threekt.materials.SpriteMaterial
import com.aitorgf.threekt.math.Vector2
import com.aitorgf.threekt.math.Frustum
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
}
