package com.aitorgf.threekt.materials

import kotlin.js.JsExport

/** Settings shared by all materials that control face culling, blending, and depth testing. */
@JsExport
abstract class Material {
    /** Unique number of this material, assigned in creation order. */
    val id: Int = nextId++

    /** Faces to render. Defaults to [Side.Front]. */
    var side = Side.Front

    /**
     * Whether the material is blended with what is behind it. Transparent meshes are drawn after opaque
     * ones, from back to front.
     */
    var transparent = false

    /**
     * How fragments are combined with the colors behind them. [Blending.Normal] applies only when
     * [transparent] is set; the other modes always blend, as in Three.js.
     */
    var blending = Blending.Normal

    /** Equation for [Blending.Custom]. */
    var blendEquation = BlendEquation.Add

    /** Source multiplier for [Blending.Custom]. */
    var blendSrc = BlendFactor.SrcAlpha

    /** Destination multiplier for [Blending.Custom]. */
    var blendDst = BlendFactor.OneMinusSrcAlpha
        set(value) {
            require(value != BlendFactor.SrcAlphaSaturate)
            field = value
        }

    /**
     * Whether the shader output is premultiplied by its alpha. [Blending.Subtractive] and
     * [Blending.Multiply] require it.
     */
    var premultipliedAlpha = false

    /** Opacity in `[0, 1]`, used by built-in materials when [transparent] is set. */
    var opacity = 1.0

    /** Draws the three edges of each mesh triangle instead of its face. */
    var wireframe = false

    /** Built-in materials discard fragments whose alpha is below this threshold. Zero disables the test. */
    var alphaTest = 0.0

    /** Whether fragments are discarded when they lie behind the depth buffer. */
    var depthTest = true

    /** Whether drawn fragments write their depth. */
    var depthWrite = true

    /**
     * Whether the geometry's `color` attribute tints each vertex. Colors are in the working color space; a
     * fourth component, when present, multiplies the opacity.
     */
    var vertexColors = false

    private companion object {
        var nextId = 0
    }
}
