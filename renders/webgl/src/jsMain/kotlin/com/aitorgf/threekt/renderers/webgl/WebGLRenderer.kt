package com.aitorgf.threekt.renderers.webgl

import com.aitorgf.threekt.core.Camera
import com.aitorgf.threekt.core.RenderTarget
import com.aitorgf.threekt.core.Renderer
import com.aitorgf.threekt.core.Scene
import kotlinx.browser.document
import com.aitorgf.threekt.math.MutableVector4
import org.khronos.webgl.WebGLContextAttributes
import org.khronos.webgl.WebGLRenderingContext
import org.w3c.dom.HTMLCanvasElement
import com.aitorgf.threekt.events.EventEmitter
import com.aitorgf.threekt.events.Observable

@ExperimentalJsExport
@JsExport
class WebGLRenderer(
    canvas: HTMLCanvasElement? = null,
    context: WebGLRenderingContext? = null,
    alpha: Boolean = false,
    depth: Boolean = true,
    stencil: Boolean = true,
    antialias: Boolean = false,
    premultipliedAlpha: Boolean = true,
    preserveDrawingBuffer: Boolean = false,
    powerPreference: WebGLPowerPreference = WebGLPowerPreference.Default,
    failIfMajorPerformanceCaveat: Boolean = false
) : Renderer {
    companion object {
        private fun createCanvas(): HTMLCanvasElement {
            val canvas = document.createElementNS("http://www.w3.org/1999/xhtml", "canvas") as HTMLCanvasElement
            canvas.style.display = "block"
            return canvas
        }

        private fun createContext(canvas: HTMLCanvasElement, vararg arguments: Any?): WebGLRenderingContext? {
            val contextNames = arrayOf("webgl2", "webgl", "experimental-webgl")
            contextNames.forEach {
                val context = canvas.getContext(it, *arguments)
                if (context != null) {
                    return context as WebGLRenderingContext
                }
            }

            return null
        }
    }

    val domElement: HTMLCanvasElement = canvas ?: createCanvas()

    private val onContextLostEmitter = EventEmitter()
    private val onContextRestoreEmitter = EventEmitter()

    val onContextLost: Observable
        get() = onContextLostEmitter
    val onContextRestore: Observable
        get() = onContextRestoreEmitter

    lateinit var gl: WebGLRenderingContext

    lateinit var extensions: WebGLExtensions
    lateinit var capabilities: WebGLCapabilities
    lateinit var utils: WebGLUtils
    lateinit var state: WebGLState
    lateinit var info: WebGLInfo
    lateinit var properties: WebGLProperties
    lateinit var animation: WebGLAnimation

    override var pixelRatio: Double = 1.0
        set(value) {
            field = value
            this.resize(this.width, this.height)
        }

    override var width: Int = this.domElement.width
        private set

    override var height: Int = this.domElement.height
        private set

    private val viewport = MutableVector4(0.0, 0.0, width.toDouble(), height.toDouble())
    private val currentViewport = MutableVector4()

    private var currentRenderTarget: RenderTarget? = null

    private var onAnimationFrameCallback: AnimationLoop? = null

    private var isContextLost: Boolean = false

    override fun init() {
        this.gl = createContext(this.domElement, WebGLContextAttributes())
            ?: throw Exception("Context for WebGLRenderer can't be initialized")

        this.extensions = WebGLExtensions(this.gl)
        this.capabilities = WebGLCapabilities(this.gl, this.extensions)
        this.extensions.check(this.capabilities.isWebGL2)
        this.utils = WebGLUtils(this.gl, this.extensions, this.capabilities)
        this.state = WebGLState(this.gl, this.extensions)
        this.info = WebGLInfo()
        this.properties = WebGLProperties()

        this.animation = WebGLAnimation()
        this.animation.setAnimationLoop {
            val localAnimationCallback = onAnimationFrameCallback
            if (localAnimationCallback != null) {
                localAnimationCallback(it)
            } else throw WebGLException("Can't init animation frame callback")
        }


        TODO("Not yet implemented")
    }

    override fun dispose() {
        TODO("Not yet implemented")
    }

    override fun render(scene: Scene, camera: Camera) {
        if (isContextLost) return

        // update scene graph
        if (scene.autoUpdate) {
            scene.updateMatrixWorld()
        }

        // update camera matrices and frustum
        if (camera.parent == null) {
            camera.updateMatrixWorld()
        }

        scene.beforeRender(this, scene, camera, this.currentRenderTarget)

        currentRenderState = renderStates.get(scene, renderStateStack.length)
        currentRenderState.init()

        renderStateStack.push(currentRenderState)

        _projScreenMatrix.multiplyMatrices(camera.projectionMatrix, camera.matrixWorldInverse)
        _frustum.setFromProjectionMatrix(_projScreenMatrix)

        _localClippingEnabled = this.localClippingEnabled
        _clippingEnabled = clipping.init(this.clippingPlanes, _localClippingEnabled, camera)

        currentRenderList = renderLists.get(scene, renderListStack.length)
        currentRenderList.init()

        renderListStack.push(currentRenderList)

        projectObject(scene, camera, 0, _this.sortObjects)

        currentRenderList.finish()

        if (_this.sortObjects) {
            currentRenderList.sort(_opaqueSort, _transparentSort)
        }

        //

        if (_clippingEnabled) {
            clipping.beginShadows()
        }

        val shadowsArray = currentRenderState.state.shadowsArray

        shadowMap.render(shadowsArray, scene, camera)

        currentRenderState.setupLights()
        currentRenderState.setupLightsView(camera)

        if (_clippingEnabled) {
            clipping.endShadows()
        }

        //

        if (this.info.autoReset) {
            this.info.reset()
        }

        background.render(currentRenderList, scene, camera, forceClear)

        // render scene
        val opaqueObjects = currentRenderList.opaque
        val transparentObjects = currentRenderList.transparent

        if (opaqueObjects.length > 0) {
            renderObjects(opaqueObjects, scene, camera)
        }

        if (transparentObjects.length > 0) {
            renderObjects(transparentObjects, scene, camera)
        }

        if (currentRenderTarget != null) {
            // Generate mipmap if we're using any kind of mipmap filtering
            textures.updateRenderTargetMipmap(currentRenderTarget)

            // resolve multisample renderbuffers to a single-sample texture if necessary
            textures.updateMultisampleRenderTarget(currentRenderTarget)
        }

        //

        scene.afterRender(this, scene, camera)

        // Ensure depth buffer writing is enabled so it can be cleared on next render

        state.buffers.depth.setTest(true)
        state.buffers.depth.setMask(true)
        state.buffers.color.setMask(true)

        state.setPolygonOffset(false)

        // _gl.finish()

        bindingStates.resetDefaultState()
        _currentMaterialId = -1
        _currentCamera = null

        renderStateStack.pop()

        if (renderStateStack.length > 0) {
            currentRenderState = renderStateStack[renderStateStack.length - 1]
        } else {
            currentRenderState = null
        }

        renderListStack.pop()

        if (renderListStack.length > 0) {
            currentRenderList = renderListStack[renderListStack.length - 1]
        } else {
            currentRenderList = null
        }
    }

    fun setAnimationLoop(cb: AnimationLoop?) {
        this.onAnimationFrameCallback = cb
        if (cb == null) {
            this.animation.stop()
        } else this.animation.start()
    }

    override fun resize(width: Int, height: Int) {
        this.width = width
        this.height = height

        this.domElement.width = kotlin.math.floor(width * this.pixelRatio).toInt()
        this.domElement.height = kotlin.math.floor(height * this.pixelRatio).toInt()

        this.setViewport(0, 0, width, height)
    }

    fun resizeWithStyle(width: Int, height: Int) {
        this.resize(width, height)

        this.domElement.style.width = "${width}px"
        this.domElement.style.height = "${height}px"
    }

    private fun setViewport(x: Int, y: Int, width: Int, height: Int) {
        this.viewport.set(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble())
        state.setViewport(this.currentViewport.set(this.viewport).multiply(this.pixelRatio).floor())
    }
}
