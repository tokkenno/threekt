package com.aitorgf.threekt.renderers.webgl

import org.khronos.webgl.WebGLRenderingContext

@ExperimentalJsExport
@JsExport
class WebGLUtils(
    val gl: WebGLRenderingContext,
    val extensions: WebGLExtensions,
    val capabilities: WebGLCapabilities
) {
    fun convert(p: Any?): Any? {
        if (p == UnsignedByteType) return WebGLRenderingContext.UNSIGNED_BYTE;
        if (p == UnsignedShort4444Type) return WebGLRenderingContext.UNSIGNED_SHORT_4_4_4_4;
        if (p == UnsignedShort5551Type) return WebGLRenderingContext.UNSIGNED_SHORT_5_5_5_1;
        if (p == UnsignedShort565Type) return WebGLRenderingContext.UNSIGNED_SHORT_5_6_5;

        if (p == ByteType) return WebGLRenderingContext.BYTE;
        if (p == ShortType) return WebGLRenderingContext.SHORT;
        if (p == UnsignedShortType) return WebGLRenderingContext.UNSIGNED_SHORT;
        if (p == IntType) return WebGLRenderingContext.INT;
        if (p == UnsignedIntType) return WebGLRenderingContext.UNSIGNED_INT;
        if (p == FloatType) return WebGLRenderingContext.FLOAT;

        if (p == HalfFloatType) {

            if (isWebGL2) return WebGLRenderingContext.HALF_FLOAT;

            extension = extensions.get('OES_texture_half_float');

            if (extension !== null) {

                return extension.HALF_FLOAT_OES;

            } else {

                return null;

            }

        }

        if (p == AlphaFormat) return WebGLRenderingContext.ALPHA;
        if (p == RGBFormat) return WebGLRenderingContext.RGB;
        if (p == RGBAFormat) return WebGLRenderingContext.RGBA;
        if (p == LuminanceFormat) return WebGLRenderingContext.LUMINANCE;
        if (p == LuminanceAlphaFormat) return WebGLRenderingContext.LUMINANCE_ALPHA;
        if (p == DepthFormat) return WebGLRenderingContext.DEPTH_COMPONENT;
        if (p == DepthStencilFormat) return WebGLRenderingContext.DEPTH_STENCIL;
        if (p == RedFormat) return WebGLRenderingContext.RED;

        // WebGL2 formats.

        if (p == RedIntegerFormat) return WebGLRenderingContext.RED_INTEGER;
        if (p == RGFormat) return WebGLRenderingContext.RG;
        if (p == RGIntegerFormat) return WebGLRenderingContext.RG_INTEGER;
        if (p == RGBIntegerFormat) return WebGLRenderingContext.RGB_INTEGER;
        if (p == RGBAIntegerFormat) return WebGLRenderingContext.RGBA_INTEGER;

        if (p == RGB_S3TC_DXT1_Format || p == RGBA_S3TC_DXT1_Format ||
            p == RGBA_S3TC_DXT3_Format || p == RGBA_S3TC_DXT5_Format
        ) {

            extension = extensions.get('WEBGL_compressed_texture_s3tc');

            if (extension != null) {
                if (p == RGB_S3TC_DXT1_Format) return extension.COMPRESSED_RGB_S3TC_DXT1_EXT;
                if (p == RGBA_S3TC_DXT1_Format) return extension.COMPRESSED_RGBA_S3TC_DXT1_EXT;
                if (p == RGBA_S3TC_DXT3_Format) return extension.COMPRESSED_RGBA_S3TC_DXT3_EXT;
                if (p == RGBA_S3TC_DXT5_Format) return extension.COMPRESSED_RGBA_S3TC_DXT5_EXT;
            } else {
                return null;
            }
        }

        if (p == RGB_PVRTC_4BPPV1_Format || p == RGB_PVRTC_2BPPV1_Format ||
            p == RGBA_PVRTC_4BPPV1_Format || p == RGBA_PVRTC_2BPPV1_Format
        ) {

            extension = extensions.get('WEBGL_compressed_texture_pvrtc');

            if (extension !== null) {
                if (p == RGB_PVRTC_4BPPV1_Format) return extension.COMPRESSED_RGB_PVRTC_4BPPV1_IMG;
                if (p == RGB_PVRTC_2BPPV1_Format) return extension.COMPRESSED_RGB_PVRTC_2BPPV1_IMG;
                if (p == RGBA_PVRTC_4BPPV1_Format) return extension.COMPRESSED_RGBA_PVRTC_4BPPV1_IMG;
                if (p == RGBA_PVRTC_2BPPV1_Format) return extension.COMPRESSED_RGBA_PVRTC_2BPPV1_IMG;
            } else {
                return null;
            }
        }

        if (p == RGB_ETC1_Format) {
            extension = extensions.get('WEBGL_compressed_texture_etc1');

            if (extension !== null) {
                return extension.COMPRESSED_RGB_ETC1_WEBGL;
            } else {
                return null;
            }
        }

        if (p == RGB_ETC2_Format || p == RGBA_ETC2_EAC_Format) {
            extension = extensions.get('WEBGL_compressed_texture_etc');

            if (extension !== null) {
                if (p == RGB_ETC2_Format) return extension.COMPRESSED_RGB8_ETC2;
                if (p == RGBA_ETC2_EAC_Format) return extension.COMPRESSED_RGBA8_ETC2_EAC;
            }
        }

        if (p == RGBA_ASTC_4x4_Format || p == RGBA_ASTC_5x4_Format || p == RGBA_ASTC_5x5_Format ||
            p == RGBA_ASTC_6x5_Format || p == RGBA_ASTC_6x6_Format || p == RGBA_ASTC_8x5_Format ||
            p == RGBA_ASTC_8x6_Format || p == RGBA_ASTC_8x8_Format || p == RGBA_ASTC_10x5_Format ||
            p == RGBA_ASTC_10x6_Format || p == RGBA_ASTC_10x8_Format || p == RGBA_ASTC_10x10_Format ||
            p == RGBA_ASTC_12x10_Format || p == RGBA_ASTC_12x12_Format ||
            p == SRGB8_ALPHA8_ASTC_4x4_Format || p == SRGB8_ALPHA8_ASTC_5x4_Format || p == SRGB8_ALPHA8_ASTC_5x5_Format ||
            p == SRGB8_ALPHA8_ASTC_6x5_Format || p == SRGB8_ALPHA8_ASTC_6x6_Format || p == SRGB8_ALPHA8_ASTC_8x5_Format ||
            p == SRGB8_ALPHA8_ASTC_8x6_Format || p == SRGB8_ALPHA8_ASTC_8x8_Format || p == SRGB8_ALPHA8_ASTC_10x5_Format ||
            p == SRGB8_ALPHA8_ASTC_10x6_Format || p == SRGB8_ALPHA8_ASTC_10x8_Format || p == SRGB8_ALPHA8_ASTC_10x10_Format ||
            p == SRGB8_ALPHA8_ASTC_12x10_Format || p == SRGB8_ALPHA8_ASTC_12x12_Format
        ) {

            extension = extensions.get('WEBGL_compressed_texture_astc');

            if (extension !== null) {
                // TODO Complete?
                return p;
            } else {
                return null;
            }
        }

        if (p == RGBA_BPTC_Format) {
            extension = extensions.get('EXT_texture_compression_bptc');

            if (extension !== null) {
                // TODO Complete?
                return p;
            } else {
                return null;
            }

        }

        if (p == UnsignedInt248Type) {
            if (isWebGL2) return WebGLRenderingContext.UNSIGNED_INT_24_8;

            extension = extensions.get('WEBGL_depth_texture');

            if (extension !== null) {
                return extension.UNSIGNED_INT_24_8_WEBGL;
            } else {
                return null;
            }
        }
    }
}
