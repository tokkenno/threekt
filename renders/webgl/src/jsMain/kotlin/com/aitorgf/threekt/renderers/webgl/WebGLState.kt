package com.aitorgf.threekt.renderers.webgl

import com.aitorgf.threekt.math.MutableVector4
import com.aitorgf.threekt.math.Vector4
import org.khronos.webgl.*

@ExperimentalJsExport
@JsExport
class WebGLState(
    val gl: WebGLRenderingContext,
    val extensions: WebGLExtensions
) {
    lateinit var capabilities: WebGLStateCapabilities

    private var isWebGL2: Boolean = false

    private val emptyTexture2d = this.createTexture(
        WebGLRenderingContext.TEXTURE_2D,
        WebGLRenderingContext.TEXTURE_2D,
        1
    )

    private val emptyTextureCubeMap = this.createTexture(
        WebGLRenderingContext.TEXTURE_CUBE_MAP,
        WebGLRenderingContext.TEXTURE_CUBE_MAP_POSITIVE_X,
        6
    )

    private val currentScissor = MutableVector4()
    private val currentViewport = MutableVector4()

    lateinit private var colorBuffer: WebGLColorBuffer
    lateinit private var depthBuffer: WebGLDepthBuffer
    lateinit private var stencilBuffer: WebGLStencilBuffer

    private val currentBoundFramebuffers: MutableMap<Int, WebGLFramebuffer> = HashMap()
    private val currentBoundTextures: MutableMap<Int, WebGLTexture> = HashMap()
    private var maxTextures: Int = 0;
    private var xrFramebuffer: WebGLFramebuffer? = null
    private var currentBlendingEnabled: Boolean = false
    private var currentPremultipledAlpha: Boolean = false
    private var currentFlipSided: Boolean? = null
    private var currentProgram: WebGLProgram? = null
    private var currentLineWidth: Float? = null
    private var currentTextureSlot: Int? = null
    private var currentPolygonOffsetFactor: Float? = null
    private var currentPolygonOffsetUnits: Float? = null
    private var lineWidthAvailable: Boolean = false

    fun setViewport(viewport: Vector4) {
        if (!this.currentViewport.equals(viewport)) {
            this.gl.viewport(viewport.x.toInt(), viewport.y.toInt(), viewport.z.toInt(), viewport.w.toInt())
            this.currentViewport.set(viewport)
        }
    }

    fun init(context: WebGLRenderingContext, extensions: WebGLExtensions, capabilities: WebGLCapabilities) {
        this.maxTextures = gl.getParameter(WebGLRenderingContext.MAX_COMBINED_TEXTURE_IMAGE_UNITS) as Int
        // TODO
        this.reset()
    }

    fun reset() {
        gl.disable(WebGLRenderingContext.BLEND)
        gl.disable(WebGLRenderingContext.CULL_FACE)
        gl.disable(WebGLRenderingContext.DEPTH_TEST)
        gl.disable(WebGLRenderingContext.POLYGON_OFFSET_FILL)
        gl.disable(WebGLRenderingContext.SCISSOR_TEST)
        gl.disable(WebGLRenderingContext.STENCIL_TEST)
        gl.disable(WebGLRenderingContext.SAMPLE_ALPHA_TO_COVERAGE)

        gl.blendEquation(WebGLRenderingContext.FUNC_ADD)
        gl.blendFunc(WebGLRenderingContext.ONE, WebGLRenderingContext.ZERO)
        gl.blendFuncSeparate(
            WebGLRenderingContext.ONE,
            WebGLRenderingContext.ZERO,
            WebGLRenderingContext.ONE,
            WebGLRenderingContext.ZERO
        )

        gl.colorMask(true, true, true, true)
        gl.clearColor(0.0F, 0.0F, 0.0F, 0.0F)

        gl.depthMask(true)
        gl.depthFunc(WebGLRenderingContext.LESS)
        gl.clearDepth(1.0F)

        gl.stencilMask(0xffffffff.toInt())
        gl.stencilFunc(WebGLRenderingContext.ALWAYS, 0, 0xffffffff.toInt())
        gl.stencilOp(WebGLRenderingContext.KEEP, WebGLRenderingContext.KEEP, WebGLRenderingContext.KEEP)
        gl.clearStencil(0)

        gl.cullFace(WebGLRenderingContext.BACK)
        gl.frontFace(WebGLRenderingContext.CCW)

        gl.polygonOffset(0.0F, 0.0F)

        gl.activeTexture(WebGLRenderingContext.TEXTURE0)

        gl.bindFramebuffer(WebGLRenderingContext.FRAMEBUFFER, null)

        if (this.isWebGL2) {
            throw Exception("Without support for WebGL2 yet")
            /*gl.bindFramebuffer(WebGLRenderingContext.DRAW_FRAMEBUFFER, null)
            gl.bindFramebuffer(WebGLRenderingContext.READ_FRAMEBUFFER, null)*/
        }

        gl.useProgram(null)

        gl.lineWidth(1.0F)

        gl.scissor(0, 0, gl.canvas.width, gl.canvas.height)
        gl.viewport(0, 0, gl.canvas.width, gl.canvas.height)

        currentTextureSlot = null
        currentBoundTextures.clear()

        xrFramebuffer = null
        currentBoundFramebuffers.clear()

        currentProgram = null

        currentBlendingEnabled = false
        currentBlending = null
        currentBlendEquation = null
        currentBlendSrc = null
        currentBlendDst = null
        currentBlendEquationAlpha = null
        currentBlendSrcAlpha = null
        currentBlendDstAlpha = null
        currentPremultipledAlpha = false

        currentFlipSided = null
        currentCullFace = null

        currentLineWidth = null

        currentPolygonOffsetFactor = null
        currentPolygonOffsetUnits = null

        this.currentScissor.set(0.0, 0.0, gl.canvas.width.toDouble(), gl.canvas.height.toDouble())
        this.currentViewport.set(0.0, 0.0, gl.canvas.width.toDouble(), gl.canvas.height.toDouble())

        this.colorBuffer.reset()
        this.depthBuffer.reset()
        this.stencilBuffer.reset()
    }

    fun activeTexture(webglSlot: Int? = null) {
        val realWebGlSlot = webglSlot ?: WebGLRenderingContext.TEXTURE0 + maxTextures - 1

        if (currentTextureSlot != realWebGlSlot) {
            gl.activeTexture(realWebGlSlot);
            currentTextureSlot = realWebGlSlot;
        }
    }

    fun bindFramebuffer(target: Int, framebuffer: WebGLFramebuffer? = null) {
        val newFramebuffer = if (framebuffer == null && xrFramebuffer != null) {
            // use active XR framebuffer if available
            xrFramebuffer
        } else {
            framebuffer
        }

        if (newFramebuffer != null && currentBoundFramebuffers[target] != newFramebuffer) {
            gl.bindFramebuffer(target, newFramebuffer)
            currentBoundFramebuffers[target] = newFramebuffer
        }
    }

    fun bindTexture(webglType, webglTexture: WebGLTexture?) {
        if (currentTextureSlot == null) {
            this.activeTexture();
        }

        val boundTexture = currentBoundTextures[currentTextureSlot];

        if (boundTexture == null && currentTextureSlot != null) {
            boundTexture = { type: undefined, texture: undefined };
            currentBoundTextures[currentTextureSlot] = boundTexture;
        }

        if (boundTexture.type != webglType || boundTexture.texture != webglTexture) {
            gl.bindTexture(
                webglType, webglTexture ?: when (webglType) {
                    WebGLRenderingContext.TEXTURE_2D -> this.emptyTexture2d
                    WebGLRenderingContext.TEXTURE_CUBE_MAP -> this.emptyTextureCubeMap
                    else -> throw WebGLException("Unknown empty texture: $webglType")
                }
            );

            boundTexture.type = webglType;
            boundTexture.texture = webglTexture;
        }
    }

    fun bindXRFramebuffer(framebuffer: WebGLFramebuffer) {
        if (framebuffer != xrFramebuffer) {
            gl.bindFramebuffer(WebGLRenderingContext.FRAMEBUFFER, framebuffer)
            xrFramebuffer = framebuffer
        }
    }

    fun createTexture(type: Int, target: Int, count: Int): WebGLTexture {
        val data = Uint8Array(4)
        val texture = gl.createTexture()

        if (texture == null) {
            throw WebGLException("The texture can't be created")
        }

        gl.bindTexture(type, texture)
        gl.texParameteri(type, WebGLRenderingContext.TEXTURE_MIN_FILTER, WebGLRenderingContext.NEAREST)
        gl.texParameteri(type, WebGLRenderingContext.TEXTURE_MAG_FILTER, WebGLRenderingContext.NEAREST)

        for (i in 0..count) {
            gl.texImage2D(
                target + i, 0,
                WebGLRenderingContext.RGBA,
                1, 1, 0,
                WebGLRenderingContext.RGBA,
                WebGLRenderingContext.UNSIGNED_BYTE,
                data
            )
        }

        return texture
    }


    fun setBlending(
        blending,
        blendEquation,
        blendSrc,
        blendDst,
        blendEquationAlpha,
        blendSrcAlpha,
        blendDstAlpha,
        premultipliedAlpha
    ) {
        if (blending == NoBlending) {
            if (currentBlendingEnabled) {
                this.capabilities.disable(WebGLRenderingContext.BLEND)
                currentBlendingEnabled = false
            }
            return
        }

        if (!currentBlendingEnabled) {
            this.capabilities.enable(WebGLRenderingContext.BLEND)
            currentBlendingEnabled = true
        }

        if (blending != CustomBlending) {
            if (blending != currentBlending || premultipliedAlpha != currentPremultipledAlpha) {
                if (currentBlendEquation != AddEquation || currentBlendEquationAlpha != AddEquation) {
                    gl.blendEquation(WebGLRenderingContext.FUNC_ADD)
                    currentBlendEquation = AddEquation
                    currentBlendEquationAlpha = AddEquation
                }

                if (premultipliedAlpha) {
                    when (blending) {
                        NormalBlending -> gl.blendFuncSeparate(
                            WebGLRenderingContext.ONE,
                            WebGLRenderingContext.ONE_MINUS_SRC_ALPHA,
                            WebGLRenderingContext.ONE,
                            WebGLRenderingContext.ONE_MINUS_SRC_ALPHA
                        )
                        AdditiveBlending -> gl.blendFunc(
                            WebGLRenderingContext.ONE,
                            WebGLRenderingContext.ONE
                        )
                        SubtractiveBlending -> gl.blendFuncSeparate(
                            WebGLRenderingContext.ZERO,
                            WebGLRenderingContext.ZERO,
                            WebGLRenderingContext.ONE_MINUS_SRC_COLOR,
                            WebGLRenderingContext.ONE_MINUS_SRC_ALPHA
                        )
                        MultiplyBlending -> gl.blendFuncSeparate(
                            WebGLRenderingContext.ZERO,
                            WebGLRenderingContext.SRC_COLOR,
                            WebGLRenderingContext.ZERO,
                            WebGLRenderingContext.SRC_ALPHA
                        )
                        else -> throw WebGLException("Invalid blending: $blending")
                    }
                } else {
                    when (blending) {
                        NormalBlending -> gl.blendFuncSeparate(
                            WebGLRenderingContext.SRC_ALPHA,
                            WebGLRenderingContext.ONE_MINUS_SRC_ALPHA,
                            WebGLRenderingContext.ONE,
                            WebGLRenderingContext.ONE_MINUS_SRC_ALPHA
                        )
                        AdditiveBlending -> gl.blendFunc(
                            WebGLRenderingContext.SRC_ALPHA,
                            WebGLRenderingContext.ONE
                        )
                        SubtractiveBlending -> gl.blendFunc(
                            WebGLRenderingContext.ZERO,
                            WebGLRenderingContext.ONE_MINUS_SRC_COLOR
                        )
                        MultiplyBlending -> gl.blendFunc(
                            WebGLRenderingContext.ZERO,
                            WebGLRenderingContext.SRC_COLOR
                        )
                        else -> throw WebGLException("Invalid blending: $blending")
                    }
                }

                currentBlendSrc = null
                currentBlendDst = null
                currentBlendSrcAlpha = null
                currentBlendDstAlpha = null

                currentBlending = blending
                currentPremultipledAlpha = premultipliedAlpha
            }

            return
        }

        // custom blending

        blendEquationAlpha = blendEquationAlpha || blendEquation
        blendSrcAlpha = blendSrcAlpha || blendSrc
        blendDstAlpha = blendDstAlpha || blendDst

        if (blendEquation != currentBlendEquation || blendEquationAlpha != currentBlendEquationAlpha) {
            gl.blendEquationSeparate(equationToGL(blendEquation), equationToGL(blendEquationAlpha))
            currentBlendEquation = blendEquation
            currentBlendEquationAlpha = blendEquationAlpha
        }

        if (blendSrc != currentBlendSrc || blendDst != currentBlendDst || blendSrcAlpha != currentBlendSrcAlpha || blendDstAlpha != currentBlendDstAlpha) {
            gl.blendFuncSeparate(
                factorToGL(blendSrc),
                factorToGL(blendDst),
                factorToGL(blendSrcAlpha),
                factorToGL(blendDstAlpha)
            )

            currentBlendSrc = blendSrc
            currentBlendDst = blendDst
            currentBlendSrcAlpha = blendSrcAlpha
            currentBlendDstAlpha = blendDstAlpha
        }

        currentBlending = blending
        currentPremultipledAlpha = false
    }

    fun setCullFace(cullFace) {
        if (cullFace != CullFaceNone) {
            this.capabilities.enable(WebGLRenderingContext.CULL_FACE)
            if (cullFace != currentCullFace) {
                if (cullFace == CullFaceBack) {
                    gl.cullFace(WebGLRenderingContext.BACK)
                } else if (cullFace == CullFaceFront) {
                    gl.cullFace(WebGLRenderingContext.FRONT)
                } else {
                    gl.cullFace(WebGLRenderingContext.FRONT_AND_BACK)
                }
            }
        } else {
            this.capabilities.disable(WebGLRenderingContext.CULL_FACE)
        }

        currentCullFace = cullFace
    }

    fun setFlipSided(flipSided: Boolean) {
        if (currentFlipSided != flipSided) {
            if (flipSided) {
                gl.frontFace(WebGLRenderingContext.CW)
            } else {
                gl.frontFace(WebGLRenderingContext.CCW)
            }
            currentFlipSided = flipSided
        }
    }

    fun setMaterial(material, frontFaceCW) {
        if (material.side == DoubleSide) {
            this.capabilities.disable(WebGLRenderingContext.CULL_FACE)
        } else {
            this.capabilities.enable(WebGLRenderingContext.CULL_FACE)
        }

        var flipSided = (material.side == BackSide)
        if (frontFaceCW) {
            flipSided = !flipSided
        }

        this.setFlipSided(flipSided)

        if (material.blending == NormalBlending && material.transparent == false) {
            this.setBlending(NoBlending)
        } else {
            this.setBlending(
                material.blending,
                material.blendEquation,
                material.blendSrc,
                material.blendDst,
                material.blendEquationAlpha,
                material.blendSrcAlpha,
                material.blendDstAlpha,
                material.premultipliedAlpha
            )
        }

        depthBuffer.setFunc(material.depthFunc)
        depthBuffer.setTest(material.depthTest)
        depthBuffer.setMask(material.depthWrite)
        colorBuffer.setMask(material.colorWrite)

        val stencilWrite = material.stencilWrite
        stencilBuffer.setTest(stencilWrite)
        if (stencilWrite) {
            stencilBuffer.setMask(material.stencilWriteMask)
            stencilBuffer.setFunc(material.stencilFunc, material.stencilRef, material.stencilFuncMask)
            stencilBuffer.setOp(material.stencilFail, material.stencilZFail, material.stencilZPass)
        }

        setPolygonOffset(material.polygonOffset, material.polygonOffsetFactor, material.polygonOffsetUnits)

        if (material.alphaToCoverage) {
            this.capabilities.enable(WebGLRenderingContext.SAMPLE_ALPHA_TO_COVERAGE)
        } else {
            this.capabilities.disable(WebGLRenderingContext.SAMPLE_ALPHA_TO_COVERAGE)
        }
    }

    fun setLineWidth(width: Float) {
        if (width != currentLineWidth) {
            if (lineWidthAvailable) {
                gl.lineWidth(width)
            }
            currentLineWidth = width
        }
    }

    fun setPolygonOffset(polygonOffset: Boolean, factor: Float, units: Float) {
        if (polygonOffset) {
            this.capabilities.enable(WebGLRenderingContext.POLYGON_OFFSET_FILL)
            if (currentPolygonOffsetFactor != factor || currentPolygonOffsetUnits != units) {
                gl.polygonOffset(factor, units)
                currentPolygonOffsetFactor = factor
                currentPolygonOffsetUnits = units
            }
        } else {
            this.capabilities.disable(WebGLRenderingContext.POLYGON_OFFSET_FILL)
        }
    }

    fun useProgram(program: WebGLProgram): Boolean {
        if (currentProgram != program) {
            gl.useProgram(program)
            currentProgram = program
            return true
        }

        return false
    }

    fun setScissorTest(scissorTest: Boolean) {
        if (scissorTest) {
            this.capabilities.enable(WebGLRenderingContext.SCISSOR_TEST);
        } else {
            this.capabilities.disable(WebGLRenderingContext.SCISSOR_TEST);
        }
    }

    fun scissor(scissor: Vector4) {
        if (!currentScissor.equals(scissor)) {
            gl.scissor(scissor.x.toInt(), scissor.y.toInt(), scissor.z.toInt(), scissor.w.toInt());
            currentScissor.set(scissor);
        }
    }

    fun unbindTexture() {
        val boundTexture = currentBoundTextures[currentTextureSlot];

        if (boundTexture != null && boundTexture.type != null) {
            gl.bindTexture(boundTexture.type, null);

            boundTexture.type = null;
            boundTexture.texture = null;
        }
    }

    fun viewport(viewport: Vector4) {
        if (!currentViewport.equals(viewport)) {
            gl.viewport(viewport.x.toInt(), viewport.y.toInt(), viewport.z.toInt(), viewport.w.toInt());
            currentViewport.set(viewport);
        }
    }

    companion object {
        private fun equationToGl(equation) {
            return when (equation) {
                AddEquation -> WebGLRenderingContext.FUNC_ADD
                SubtractEquation -> WebGLRenderingContext.FUNC_SUBTRACT
                ReverseSubtractEquation -> WebGLRenderingContext.FUNC_REVERSE_SUBTRACT
            }
        }

        private fun factorToGL(factor) {
            return when (factor) {
                ZeroFactor -> WebGLRenderingContext.ZERO
                OneFactor -> WebGLRenderingContext.ONE
                SrcColorFactor -> WebGLRenderingContext.SRC_COLOR
                SrcAlphaFactor -> WebGLRenderingContext.SRC_ALPHA
                SrcAlphaSaturateFactor -> WebGLRenderingContext.SRC_ALPHA_SATURATE
                DstColorFactor -> WebGLRenderingContext.DST_COLOR
                DstAlphaFactor -> WebGLRenderingContext.DST_ALPHA
                OneMinusSrcColorFactor -> WebGLRenderingContext.ONE_MINUS_SRC_COLOR
                OneMinusSrcAlphaFactor -> WebGLRenderingContext.ONE_MINUS_SRC_ALPHA
                OneMinusDstColorFactor -> WebGLRenderingContext.ONE_MINUS_DST_COLOR
                OneMinusDstAlphaFactor -> WebGLRenderingContext.ONE_MINUS_DST_ALPHA
            }
        }
    }
}
