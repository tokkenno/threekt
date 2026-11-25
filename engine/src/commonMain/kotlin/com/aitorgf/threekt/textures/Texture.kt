package com.aitorgf.threekt.textures

import com.aitorgf.threekt.math.Matrix3
import com.aitorgf.threekt.math.Vector2
import com.aitorgf.threekt.math.color.ColorSpace
import kotlin.js.JsExport

/**
 * Texture data whose source is supplied by a platform implementation, together with the sampling and
 * UV-transform settings that renderers apply when the texture is drawn.
 */
@JsExport
abstract class Texture {
    /**
     * Platform image that holds the pixels, such as an `HTMLImageElement` or canvas in the browser, or
     * null when the texture has no source yet.
     */
    open val image: Any? get() = null

    /**
     * Encoding of the source pixels. Threekt defaults to sRGB for existing image textures.
     * Use null for data textures that should be sampled without decoding.
     */
    var colorSpace: ColorSpace? = ColorSpace.SRGB

    /** Incremented by [markUpdated] so that renderers know to upload the pixels again. */
    var version = 0
        private set

    /**
     * Marks the source pixels as changed so that they are uploaded before the next draw. Replaces the
     * Three.js `needsUpdate = true` setter.
     */
    fun markUpdated() {
        version++
    }

    /** Horizontal (U) wrapping mode. Renderers apply changes on the next draw. */
    var wrapS = Wrapping.ClampToEdge
    /** Vertical (V) wrapping mode. Renderers apply changes on the next draw. */
    var wrapT = Wrapping.ClampToEdge

    /**
     * Number of samples taken along the axis through the pixel with the highest texel density. Values
     * above the renderer's maximum are clamped when the texture is drawn; `1` disables anisotropic
     * filtering.
     */
    var anisotropy = 1

    /** UV offset added after [repeat] and [rotation]. A value of `1` shifts the image by its full size. */
    val offset = Vector2(0.0, 0.0)
    /** Number of times the image repeats along U and V. Use [Wrapping.Repeat] to tile it. */
    val repeat = Vector2(1.0, 1.0)
    /** Pivot of [rotation] in UV space, where `(0.5, 0.5)` is the center of the image. */
    val center = Vector2(0.0, 0.0)
    /** Counterclockwise rotation of the image around [center], in radians. */
    var rotation = 0.0

    /**
     * Whether [updateMatrix] rebuilds [matrix] from [offset], [repeat], [rotation], and [center].
     * Disable it to set [matrix] directly, for example with [Matrix3.setUvTransform].
     */
    var matrixAutoUpdate = true
    /** UV transform applied to the geometry's texture coordinates. */
    val matrix = Matrix3()

    /** Rebuilds [matrix] from the offset, repeat, rotation, and center settings. */
    fun updateMatrix() {
        matrix.setUvTransform(offset.x, offset.y, repeat.x, repeat.y, rotation, center.x, center.y)
    }
}
