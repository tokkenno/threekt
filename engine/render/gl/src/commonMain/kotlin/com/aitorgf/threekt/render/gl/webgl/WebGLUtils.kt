package com.aitorgf.threekt.render.gl.webgl

import com.aitorgf.threekt.core.Usage
import com.aitorgf.threekt.materials.BlendEquation
import com.aitorgf.threekt.materials.BlendFactor
import com.aitorgf.threekt.textures.TextureFilter
import com.aitorgf.threekt.textures.Wrapping

/**
 * Translates the renderer-neutral enums of the core into WebGL 2 and OpenGL ES 3.0 values, as Three.js
 * `WebGLUtils.convert()` translates its numeric constants. Three.js builds it per context to consult
 * extensions; every value Threekt translates is part of WebGL 2 itself, so it is a stateless object.
 */
internal object WebGLUtils {
    fun convert(usage: Usage): Int = when (usage) {
        Usage.StaticDraw -> GL.STATIC_DRAW
        Usage.DynamicDraw -> GL.DYNAMIC_DRAW
        Usage.StreamDraw -> GL.STREAM_DRAW
        Usage.StaticRead -> GL.STATIC_READ
        Usage.DynamicRead -> GL.DYNAMIC_READ
        Usage.StreamRead -> GL.STREAM_READ
        Usage.StaticCopy -> GL.STATIC_COPY
        Usage.DynamicCopy -> GL.DYNAMIC_COPY
        Usage.StreamCopy -> GL.STREAM_COPY
    }

    fun convert(factor: BlendFactor): Int = when (factor) {
        BlendFactor.Zero -> GL.ZERO
        BlendFactor.One -> GL.ONE
        BlendFactor.SrcColor -> GL.SRC_COLOR
        BlendFactor.OneMinusSrcColor -> GL.ONE_MINUS_SRC_COLOR
        BlendFactor.SrcAlpha -> GL.SRC_ALPHA
        BlendFactor.OneMinusSrcAlpha -> GL.ONE_MINUS_SRC_ALPHA
        BlendFactor.DstAlpha -> GL.DST_ALPHA
        BlendFactor.OneMinusDstAlpha -> GL.ONE_MINUS_DST_ALPHA
        BlendFactor.DstColor -> GL.DST_COLOR
        BlendFactor.OneMinusDstColor -> GL.ONE_MINUS_DST_COLOR
        BlendFactor.SrcAlphaSaturate -> GL.SRC_ALPHA_SATURATE
    }

    fun convert(equation: BlendEquation): Int = when (equation) {
        BlendEquation.Add -> GL.FUNC_ADD
        BlendEquation.Subtract -> GL.FUNC_SUBTRACT
        BlendEquation.ReverseSubtract -> GL.FUNC_REVERSE_SUBTRACT
        BlendEquation.Min -> GL.MIN
        BlendEquation.Max -> GL.MAX
    }

    fun convert(filter: TextureFilter): Int = when (filter) {
        TextureFilter.Nearest -> GL.NEAREST
        TextureFilter.Linear -> GL.LINEAR
        TextureFilter.NearestMipmapNearest -> GL.NEAREST_MIPMAP_NEAREST
        TextureFilter.LinearMipmapNearest -> GL.LINEAR_MIPMAP_NEAREST
        TextureFilter.NearestMipmapLinear -> GL.NEAREST_MIPMAP_LINEAR
        TextureFilter.LinearMipmapLinear -> GL.LINEAR_MIPMAP_LINEAR
    }

    fun convert(wrapping: Wrapping): Int = when (wrapping) {
        Wrapping.Repeat -> GL.REPEAT
        Wrapping.ClampToEdge -> GL.CLAMP_TO_EDGE
        Wrapping.MirroredRepeat -> GL.MIRRORED_REPEAT
    }
}
