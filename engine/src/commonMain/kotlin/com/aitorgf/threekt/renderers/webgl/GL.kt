package com.aitorgf.threekt.renderers.webgl

import kotlin.js.JsExport

/**
 * WebGL2 and OpenGL ES 3.0 enumeration values used by Threekt. Both APIs share these values, so a
 * [GLContext] passes them to the platform unchanged.
 */
@JsExport
object GL {
    const val NO_ERROR = 0
    const val POINTS = 0
    const val ZERO = 0
    const val LINES = 1
    const val ONE = 1
    const val LINE_LOOP = 2
    const val LINE_STRIP = 3
    const val TRIANGLES = 4
    const val DEPTH_BUFFER_BIT = 0x0100
    const val LEQUAL = 0x0203
    const val GEQUAL = 0x0206
    const val SRC_COLOR = 0x0300
    const val ONE_MINUS_SRC_COLOR = 0x0301
    const val SRC_ALPHA = 0x0302
    const val ONE_MINUS_SRC_ALPHA = 0x0303
    const val DST_COLOR = 0x0306
    const val FRONT = 0x0404
    const val BACK = 0x0405
    const val CW = 0x0900
    const val CCW = 0x0901
    const val CULL_FACE = 0x0B44
    const val DEPTH_TEST = 0x0B71
    const val VIEWPORT = 0x0BA2
    const val BLEND = 0x0BE2
    const val SCISSOR_TEST = 0x0C11
    const val MAX_TEXTURE_SIZE = 0x0D33
    const val TEXTURE_2D = 0x0DE1
    const val BYTE = 0x1400
    const val UNSIGNED_BYTE = 0x1401
    const val SHORT = 0x1402
    const val INT = 0x1404
    const val UNSIGNED_INT = 0x1405
    const val FLOAT = 0x1406
    const val RGBA = 0x1908
    const val NEAREST = 0x2600
    const val LINEAR = 0x2601
    const val LINEAR_MIPMAP_LINEAR = 0x2703
    const val TEXTURE_MAG_FILTER = 0x2800
    const val TEXTURE_MAX_LEVEL = 0x813D
    const val TEXTURE_MIN_FILTER = 0x2801
    const val TEXTURE_WRAP_S = 0x2802
    const val TEXTURE_WRAP_T = 0x2803
    const val REPEAT = 0x2901
    const val COLOR_BUFFER_BIT = 0x4000
    const val FUNC_ADD = 0x8006
    const val RGBA8 = 0x8058
    const val TEXTURE_WRAP_R = 0x8072
    const val CLAMP_TO_EDGE = 0x812F
    const val DEPTH_COMPONENT24 = 0x81A6
    const val MIRRORED_REPEAT = 0x8370
    const val TEXTURE0 = 0x84C0
    const val TEXTURE_MAX_ANISOTROPY_EXT = 0x84FE
    const val MAX_TEXTURE_MAX_ANISOTROPY_EXT = 0x84FF
    const val TEXTURE_CUBE_MAP = 0x8513
    const val TEXTURE_BINDING_CUBE_MAP = 0x8514
    const val TEXTURE_CUBE_MAP_POSITIVE_X = 0x8515
    const val MAX_CUBE_MAP_TEXTURE_SIZE = 0x851C
    const val RGBA32F = 0x8814
    const val ARRAY_BUFFER = 0x8892
    const val ELEMENT_ARRAY_BUFFER = 0x8893
    const val STREAM_DRAW = 0x88E0
    const val STATIC_DRAW = 0x88E4
    const val FRAGMENT_SHADER = 0x8B30
    const val VERTEX_SHADER = 0x8B31
    const val FLOAT_VEC2 = 0x8B50
    const val FLOAT_VEC3 = 0x8B51
    const val FLOAT_VEC4 = 0x8B52
    const val INT_VEC2 = 0x8B53
    const val BOOL = 0x8B56
    const val FLOAT_MAT3 = 0x8B5B
    const val FLOAT_MAT4 = 0x8B5C
    const val SAMPLER_2D = 0x8B5E
    const val COMPILE_STATUS = 0x8B81
    const val LINK_STATUS = 0x8B82
    const val ACTIVE_UNIFORMS = 0x8B86
    const val ACTIVE_ATTRIBUTES = 0x8B89
    const val TEXTURE_2D_ARRAY = 0x8C1A
    const val SRGB8_ALPHA8 = 0x8C43
    const val DRAW_FRAMEBUFFER_BINDING = 0x8CA6
    const val RENDERBUFFER_BINDING = 0x8CA7
    const val READ_FRAMEBUFFER = 0x8CA8
    const val DRAW_FRAMEBUFFER = 0x8CA9
    const val FRAMEBUFFER_COMPLETE = 0x8CD5
    const val COLOR_ATTACHMENT0 = 0x8CE0
    const val DEPTH_ATTACHMENT = 0x8D00
    const val FRAMEBUFFER = 0x8D40
    const val RENDERBUFFER = 0x8D41
    const val SAMPLER_2D_ARRAY = 0x8DC1
}
