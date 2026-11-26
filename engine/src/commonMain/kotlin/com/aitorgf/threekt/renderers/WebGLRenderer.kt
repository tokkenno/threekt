package com.aitorgf.threekt.renderers

import com.aitorgf.threekt.cameras.ArrayCamera
import com.aitorgf.threekt.cameras.Camera
import com.aitorgf.threekt.cameras.CubeCamera
import com.aitorgf.threekt.cameras.OrthographicCamera
import com.aitorgf.threekt.core.DispatchedEvent
import com.aitorgf.threekt.core.BufferGeometry
import com.aitorgf.threekt.core.GpuType
import com.aitorgf.threekt.core.BufferAttribute
import com.aitorgf.threekt.core.InstancedBufferAttribute
import com.aitorgf.threekt.core.InstancedBufferGeometry
import com.aitorgf.threekt.core.InstancedInterleavedBuffer
import com.aitorgf.threekt.core.InterleavedBufferAttribute
import com.aitorgf.threekt.core.Object3D
import com.aitorgf.threekt.materials.Blending
import com.aitorgf.threekt.materials.LineBasicMaterial
import com.aitorgf.threekt.materials.LineDashedMaterial
import com.aitorgf.threekt.materials.Material
import com.aitorgf.threekt.materials.MeshBasicMaterial
import com.aitorgf.threekt.materials.MeshLambertMaterial
import com.aitorgf.threekt.materials.MeshNormalMaterial
import com.aitorgf.threekt.materials.SpriteMaterial
import com.aitorgf.threekt.materials.MeshPhongMaterial
import com.aitorgf.threekt.materials.PointsMaterial
import com.aitorgf.threekt.materials.RawShaderMaterial
import com.aitorgf.threekt.materials.ShaderMaterial
import com.aitorgf.threekt.materials.Side
import com.aitorgf.threekt.math.Color
import com.aitorgf.threekt.math.Matrix3
import com.aitorgf.threekt.math.Frustum
import com.aitorgf.threekt.math.Matrix4
import com.aitorgf.threekt.math.Vector3
import com.aitorgf.threekt.objects.InstancedMesh
import com.aitorgf.threekt.objects.Line
import com.aitorgf.threekt.objects.Sprite
import com.aitorgf.threekt.objects.LineLoop
import com.aitorgf.threekt.objects.LineSegments
import com.aitorgf.threekt.objects.Mesh
import com.aitorgf.threekt.objects.Points
import com.aitorgf.threekt.renderers.shaders.ShaderLib
import com.aitorgf.threekt.renderers.webgl.GL
import com.aitorgf.threekt.renderers.webgl.GLContext
import com.aitorgf.threekt.renderers.webgl.WebGLRenderTargets
import com.aitorgf.threekt.textures.DataTexture
import com.aitorgf.threekt.textures.FramebufferTexture
import com.aitorgf.threekt.renderers.webgl.WebGLAttributes
import com.aitorgf.threekt.renderers.webgl.WebGLCapabilities
import com.aitorgf.threekt.renderers.webgl.WebGLGeometries
import com.aitorgf.threekt.renderers.webgl.WebGLLights
import com.aitorgf.threekt.renderers.webgl.WebGLMorphtargets
import com.aitorgf.threekt.renderers.webgl.WebGLProgram
import com.aitorgf.threekt.renderers.webgl.WebGLRenderList
import com.aitorgf.threekt.renderers.webgl.WebGLTextures
import com.aitorgf.threekt.renderers.webgl.WebGLUniforms
import com.aitorgf.threekt.scenes.Fog
import com.aitorgf.threekt.scenes.FogExp2
import com.aitorgf.threekt.scenes.Scene
import com.aitorgf.threekt.scenes.SceneFog
import com.aitorgf.threekt.textures.Texture
import kotlin.js.JsExport
import kotlin.math.roundToInt

/**
 * Draws meshes and lines into the drawing buffer of a WebGL2-compatible [context], such as
 * `WebGL2Context` for a browser canvas or `GLES30Context` on the JVM. Three.js takes a parameters object
 * with a canvas instead. [MeshBasicMaterial] and [LineBasicMaterial] are unlit and support a color
 * texture, vertex colors, and position morph targets, [PointsMaterial] draws sized points,
 * [SpriteMaterial] draws camera-facing sprites, [MeshNormalMaterial] shows normals, and
 * [MeshPhongMaterial] and [MeshLambertMaterial] are lit by ambient, hemisphere, directional and point
 * lights. Built-in materials apply the scene's fog. [RawShaderMaterial] uses its own shaders.
 * [InstancedMesh]es and [InstancedBufferGeometry] are drawn with instanced draw calls. Opaque objects are
 * drawn before transparent ones, which are blended back to front.
 */
@JsExport
class WebGLRenderer(context: GLContext) {
    private val gl = context

    /** Limits and optional features of this renderer's WebGL2 context. */
    val capabilities = WebGLCapabilities(gl)

    private val renderTargets = WebGLRenderTargets(gl)
    private val textures = WebGLTextures(gl, capabilities, renderTargets)
    private val attributes = WebGLAttributes(gl)
    private val geometries = WebGLGeometries(attributes)
    private val uniforms = WebGLUniforms(gl, textures)
    private val morphtargets = WebGLMorphtargets(gl, uniforms)
    private val lights = WebGLLights()
    private val programs = mutableMapOf<Pair<String, String>, WebGLProgram>()
    private val programUsers = mutableMapOf<Pair<String, String>, Int>()
    private val materialPrograms = mutableMapOf<Material, MutableSet<Pair<String, String>>>()
    private val materialListeners = mutableMapOf<Material, (DispatchedEvent) -> Unit>()

    /** Number of cached linked shader programs. */
    val programCount: Int get() = programs.size

    private var renderTarget: WebGLRenderTarget? = null

    /** Selects the destination for subsequent draws. Null selects the canvas drawing buffer. */
    fun setRenderTarget(target: WebGLRenderTarget?) {
        val binding = target?.let(renderTargets::get)
        gl.bindFramebuffer(GL.DRAW_FRAMEBUFFER, binding?.framebuffer)
        renderTarget = target
        updateScissor()
    }

    fun getRenderTarget(): WebGLRenderTarget? = renderTarget

    /** Reads a bottom-left-origin RGBA rectangle from a floating-point color attachment. */
    fun readRenderTargetPixels(target: WebGLRenderTarget, x: Int, y: Int, width: Int, height: Int, buffer: FloatArray) {
        require(target.type == RenderTargetType.Float)
        require(width > 0 && height > 0 && x in 0..target.width - width && y in 0..target.height - height)
        require(width.toLong() * height * 4 <= buffer.size)
        val previous = gl.getParameterHandle(GL.READ_FRAMEBUFFER_BINDING)
        try {
            gl.bindFramebuffer(GL.READ_FRAMEBUFFER, renderTargets.get(target).framebuffer)
            gl.readPixelsFloat(x, y, width, height, buffer)
        } finally {
            gl.bindFramebuffer(GL.READ_FRAMEBUFFER, previous)
        }
    }

    /** Copies a rectangle from the active drawing framebuffer into [texture]. */
    fun copyFramebufferToTexture(texture: FramebufferTexture, x: Int, y: Int) {
        val target = renderTarget
        require(x in 0..(target?.width ?: gl.drawingBufferWidth) - texture.width)
        require(y in 0..(target?.height ?: gl.drawingBufferHeight) - texture.height)
        val previous = gl.getParameterHandle(GL.READ_FRAMEBUFFER_BINDING)
        try {
            gl.bindFramebuffer(GL.READ_FRAMEBUFFER, target?.let(renderTargets::get)?.framebuffer)
            textures.bind(texture, 0)
            gl.copyTexSubImage2D(x, y, texture.width, texture.height)
        } finally {
            gl.bindFramebuffer(GL.READ_FRAMEBUFFER, previous)
        }
    }

    /** Uploads the pixels of [source] into level zero of [destination] at a bottom-left offset. */
    fun copyTextureToTexture(source: DataTexture, destination: Texture, x: Int, y: Int) {
        require(x >= 0 && y >= 0 && destination !is RenderTargetTexture)
        textures.bind(destination, 0)
        gl.texSubImage2DBytes(x, y, source.width, source.height, source.data)
        if (destination.generateMipmaps) gl.generateMipmap(GL.TEXTURE_2D)
    }
    private val enabledAttributes = mutableSetOf<Int>()
    private val renderList = WebGLRenderList()

    /**
     * Whether [render] clears the color and depth buffers before drawing. Turn it off to draw several
     * scenes on top of each other, clearing manually with [clear] or [clearDepth]. A scene background
     * color still clears the color, as in Three.js.
     */
    var autoClear = true

    private val clearColor = Color(0.0, 0.0, 0.0)
    private var clearAlpha = 1.0
    private val outputColor = Color()

    private var pixelRatio = 1.0
    private var viewport: IntArray? = null
    private var scissorTest = false
    private var scissor: IntArray? = null
    private val scissorPixels = IntArray(4)
    /** Height in CSS pixels from [setSize], or null before the first call. */
    private var height: Int? = null
    private val viewProjection = Matrix4()
    private val projection = Matrix4()
    private val projScreenMatrix = Matrix4()
    private val frustum = Frustum()
    private val cameraPosition = Vector3()

    /** Sets the drawing-buffer dimensions from CSS pixels and the supplied [pixelRatio]. */
    fun setSize(width: Int, height: Int, pixelRatio: Double = 1.0) {
        require(width > 0 && height > 0 && pixelRatio.isFinite() && pixelRatio > 0.0)
        this.pixelRatio = pixelRatio
        this.height = height
        val pixelWidth = (width * pixelRatio).roundToInt().coerceAtLeast(1)
        val pixelHeight = (height * pixelRatio).roundToInt().coerceAtLeast(1)
        if (gl.drawingBufferWidth != pixelWidth || gl.drawingBufferHeight != pixelHeight) {
            gl.resizeDrawingBuffer(pixelWidth, pixelHeight)
        }
        updateScissor()
    }

    /** Sets the viewport for a single camera in CSS pixels from the lower-left corner. */
    fun setViewport(x: Int, y: Int, width: Int, height: Int) {
        require(width >= 0 && height >= 0)
        val bounds = viewport ?: IntArray(4).also { viewport = it }
        bounds[0] = x
        bounds[1] = y
        bounds[2] = width
        bounds[3] = height
    }

    /** Restores the full drawing buffer viewport for a single camera. */
    fun resetViewport() {
        viewport = null
    }

    /** Sets the scissor rectangle in CSS pixels, measured from the lower-left corner. */
    fun setScissor(x: Int, y: Int, width: Int, height: Int) {
        require(width >= 0 && height >= 0)
        val bounds = scissor ?: IntArray(4).also { scissor = it }
        bounds[0] = x
        bounds[1] = y
        bounds[2] = width
        bounds[3] = height
        updateScissor()
    }

    /** Limits drawing and clearing to the scissor rectangle when [enabled]. */
    fun setScissorTest(enabled: Boolean) {
        scissorTest = enabled
        if (enabled) {
            updateScissor()
            gl.enable(GL.SCISSOR_TEST)
        } else {
            gl.disable(GL.SCISSOR_TEST)
        }
    }

    private fun updateScissor() {
        val target = renderTarget
        if (target != null) {
            gl.scissor(0, 0, target.width, target.height)
            return
        }
        val bounds = scissor
        if (bounds == null) {
            if (!scissorTest) return
            scissorPixels[0] = 0
            scissorPixels[1] = 0
            scissorPixels[2] = gl.drawingBufferWidth
            scissorPixels[3] = gl.drawingBufferHeight
        } else {
            for (i in bounds.indices) scissorPixels[i] = (bounds[i] * pixelRatio).roundToInt()
        }
        gl.scissor(scissorPixels[0], scissorPixels[1], scissorPixels[2], scissorPixels[3])
    }

    /**
     * Sets the color and [alpha] used to clear views of scenes without a [Scene.background]. [color] is in
     * the working color space. The default is opaque black, as in Three.js without the `alpha` option.
     */
    fun setClearColor(color: Color, alpha: Double = 1.0) {
        clearColor.copy(color)
        clearAlpha = alpha
    }

    /** Copies the clear color into [target] and returns it. */
    fun getClearColor(target: Color): Color = target.copy(clearColor)

    /** Returns the alpha used with the clear color. */
    fun getClearAlpha(): Double = clearAlpha

    /**
     * Updates [scene] and draws its meshes into the drawing buffer using [camera].
     * An [ArrayCamera] draws each subcamera in its own viewport.
     */
    fun render(scene: Scene, camera: Camera) {
        // Update the scene graph, and the camera when it is not part of it, as Three.js does.
        if (scene.matrixWorldAutoUpdate) scene.updateMatrixWorld()
        if (camera.parent == null && camera.matrixWorldAutoUpdate) camera.updateMatrixWorld()
        val target = renderTarget
        if (target != null) {
            gl.bindFramebuffer(GL.DRAW_FRAMEBUFFER, renderTargets.get(target).framebuffer)
            updateScissor()
            renderView(scene, camera, 0, 0, target.width, target.height)
        } else if (camera is ArrayCamera) {
            renderArray(scene, camera)
        } else {
            val bounds = viewport
            if (bounds == null) {
                renderView(scene, camera, 0, 0, gl.drawingBufferWidth, gl.drawingBufferHeight)
            } else {
                renderView(
                    scene, camera, (bounds[0] * pixelRatio).roundToInt(), (bounds[1] * pixelRatio).roundToInt(),
                    (bounds[2] * pixelRatio).roundToInt(), (bounds[3] * pixelRatio).roundToInt(),
                )
            }
        }
    }

    /** Renders all six views into [target] and restores the previous drawing framebuffer and viewport. */
    internal fun renderCube(scene: Scene, camera: CubeCamera, target: WebGLCubeRenderTarget) {
        require(target.gl === gl) { "The cube render target belongs to a different WebGL context." }
        val framebuffer = gl.getParameterHandle(GL.DRAW_FRAMEBUFFER_BINDING)
        val viewport = gl.getParameterInts(GL.VIEWPORT, IntArray(4))
        if (scene.matrixWorldAutoUpdate) scene.updateMatrixWorld()
        camera.coordinateSystem = CoordinateSystem.WebGL
        if (scissorTest) gl.disable(GL.SCISSOR_TEST)
        try {
            camera.update { face, faceCamera ->
                target.bindFace(face)
                renderView(scene, faceCamera, 0, 0, target.size, target.size)
            }
        } finally {
            gl.bindFramebuffer(GL.DRAW_FRAMEBUFFER, framebuffer)
            if (scissorTest) gl.enable(GL.SCISSOR_TEST)
            gl.viewport(viewport[0], viewport[1], viewport[2], viewport[3])
        }
    }

    private fun renderArray(scene: Scene, camera: ArrayCamera) {
        clear(scene, colorOnly = true)
        gl.enable(GL.SCISSOR_TEST)
        try {
            for ((subcamera, viewport) in camera.views) {
                val (x, y, width, height) = viewport
                subcamera.updateWorldMatrix(updateParents = true, updateChildren = false)
                if (scissorTest) {
                    val left = maxOf(x, scissorPixels[0])
                    val bottom = maxOf(y, scissorPixels[1])
                    val right = minOf(x + width, scissorPixels[0] + scissorPixels[2])
                    val top = minOf(y + height, scissorPixels[1] + scissorPixels[3])
                    gl.scissor(left, bottom, maxOf(0, right - left), maxOf(0, top - bottom))
                } else {
                    gl.scissor(x, y, width, height)
                }
                renderView(scene, subcamera, x, y, width, height)
            }
        } finally {
            setScissorTest(scissorTest)
        }
    }

    /**
     * Clears the [color], [depth] or both buffers of the current viewport to the clear color and a depth
     * of one.
     */
    fun clear(color: Boolean = true, depth: Boolean = true) {
        renderTarget?.let {
            gl.bindFramebuffer(GL.DRAW_FRAMEBUFFER, renderTargets.get(it).framebuffer)
            updateScissor()
        }
        var mask = 0
        if (color) {
            if (renderTarget == null) outputColor.copyLinearToSRGB(clearColor) else outputColor.copy(clearColor)
            gl.clearColor(outputColor.r, outputColor.g, outputColor.b, clearAlpha)
            mask = mask or GL.COLOR_BUFFER_BIT
        }
        if (depth) {
            gl.depthMask(true)
            gl.clearDepth(1.0)
            mask = mask or GL.DEPTH_BUFFER_BIT
        }
        if (mask != 0) gl.clear(mask)
    }

    /** Clears only the depth buffer, so the next scene draws over the current image. */
    fun clearDepth() = clear(color = false, depth = true)

    /** Clears to the scene background, or to the clear color when the scene has none. */
    private fun clear(scene: Scene, colorOnly: Boolean = false, reversedDepth: Boolean = false) {
        val background = scene.background
        // Colors are stored in linear working space, while the drawing buffer holds sRGB values.
        if (background != null) {
            if (renderTarget == null) outputColor.copyLinearToSRGB(background) else outputColor.copy(background)
            gl.clearColor(outputColor.r, outputColor.g, outputColor.b, 1.0)
        } else {
            if (renderTarget == null) outputColor.copyLinearToSRGB(clearColor) else outputColor.copy(clearColor)
            gl.clearColor(outputColor.r, outputColor.g, outputColor.b, clearAlpha)
        }
        if (colorOnly) {
            gl.clear(GL.COLOR_BUFFER_BIT)
        } else {
            gl.depthMask(true)
            gl.clearDepth(if (reversedDepth) 0.0 else 1.0)
            gl.clear(GL.COLOR_BUFFER_BIT or GL.DEPTH_BUFFER_BIT)
        }
    }

    private fun renderView(scene: Scene, camera: Camera, x: Int, y: Int, width: Int, height: Int) {
        // WebGL clips depth to [-1, 1]; remap projections that target [0, 1] or reversed depth.
        projection.copy(camera.projectionMatrix)
        if (camera.coordinateSystem == CoordinateSystem.WebGPU || camera.reversedDepth) {
            val elements = projection.elements
            for (column in 0 until 16 step 4) {
                elements[column + 2] = 2.0 * elements[column + 2] - elements[column + 3]
            }
        }
        viewProjection.multiplyMatrices(projection, camera.matrixWorldInverse)
        projScreenMatrix.multiplyMatrices(camera.projectionMatrix, camera.matrixWorldInverse)
        frustum.setFromProjectionMatrix(projScreenMatrix, camera.coordinateSystem, camera.reversedDepth)
        cameraPosition.setFromMatrixPosition(camera.matrixWorld)

        gl.viewport(x, y, width, height)
        if (autoClear) {
            clear(scene, reversedDepth = camera.reversedDepth)
        } else if (scene.background != null) {
            clear(scene, colorOnly = true)
        }
        gl.depthFunc(if (camera.reversedDepth) GL.GEQUAL else GL.LEQUAL)

        renderList.build(scene, camera, viewProjection, frustum)
        lights.setup(renderList.lights)
        lights.setupView(renderList.lights, camera.matrixWorldInverse)
        for (item in renderList.opaque) renderObject(item.obj, item.geometry, item.material, scene, camera, item.group)
        for (item in renderList.transparent) renderObject(item.obj, item.geometry, item.material, scene, camera, item.group)
        gl.disable(GL.BLEND)
        gl.disable(GL.SAMPLE_ALPHA_TO_COVERAGE)
        gl.depthMask(true)
    }

    /**
     * Draws one object between its [Object3D.onBeforeRender] and [Object3D.onAfterRender] callbacks, after
     * setting its [Object3D.modelViewMatrix] and [Object3D.normalMatrix] for [camera], as Three.js does.
     */
    private fun renderObject(
        obj: Object3D, geometry: BufferGeometry, material: Material, scene: Scene, camera: Camera,
        group: BufferGeometry.Group? = null,
    ) {
        obj.onBeforeRender(this, scene, camera, geometry, material, group)
        obj.modelViewMatrix.multiplyMatrices(camera.matrixWorldInverse, obj.matrixWorld)
        obj.normalMatrix.getNormalMatrix(obj.modelViewMatrix)
        renderBufferDirect(obj, geometry, material, scene, camera, group)
        obj.onAfterRender(this, scene, camera, geometry, material, group)
    }

    /** Draws one object, as `renderBufferDirect` does in Three.js. */
    private fun renderBufferDirect(
        obj: Object3D, geometry: BufferGeometry, material: Material, scene: Scene, camera: Camera,
        group: BufferGeometry.Group? = null,
    ) {
        geometries.get(geometry)
        val position = geometry.getAttribute("position")
        val wireframe = obj is Mesh && material.wireframe
        val index = if (wireframe) geometries.getWireframeAttribute(geometry) else geometry.index
        val rangeFactor = if (wireframe) 2 else 1
        // Clamp the draw range to the available indices or vertices, as Three.js does.
        val drawRange = geometry.drawRange
        var drawStart = drawRange.start.toLong() * rangeFactor
        var drawEnd = drawStart + drawRange.count.toLong() * rangeFactor
        if (group != null) {
            // Intersect the draw range with the group, as Three.js does for multi-material meshes.
            drawStart = maxOf(drawStart, group.start.toLong() * rangeFactor)
            drawEnd = minOf(drawEnd, (group.start.toLong() + group.count) * rangeFactor)
        }
        if (index != null) {
            drawStart = maxOf(drawStart, 0)
            drawEnd = minOf(drawEnd, index.count.toLong())
        } else if (position != null) {
            drawStart = maxOf(drawStart, 0)
            drawEnd = minOf(drawEnd, position.count.toLong())
        }
        val drawCount = drawEnd - drawStart
        // A negative count draws nothing; an unbounded one cannot be drawn without attributes or indices.
        if (drawCount < 0 || drawCount >= Int.MAX_VALUE) return

        val instanceCount = when {
            obj is InstancedMesh -> obj.count
            geometry is InstancedBufferGeometry -> minOf(geometry.instanceCount, maxInstanceCount(geometry))
            else -> -1
        }
        if (instanceCount == 0) return

        val morphTargetInfluences = when (obj) {
            is Mesh -> obj.morphTargetInfluences
            is Line -> obj.morphTargetInfluences
            is Points -> obj.morphTargetInfluences
            else -> null
        }
        val morphTargetsCount = if (morphTargetInfluences != null) geometry.morphAttributes["position"]?.size ?: 0 else 0
        val fog = scene.fog
        val program = getProgram(material, obj, geometry, morphTargetsCount, fog)
        gl.useProgram(program.handle)
        setMaterialState(material, obj.matrixWorld.determinantAffine() < 0.0)

        uniforms.reset()
        setBuiltInUniforms(program, obj, camera)
        when (material) {
            is MeshBasicMaterial -> setBasicUniforms(program, geometry, material.color, material.opacity, material.map)
            is LineBasicMaterial -> setBasicUniforms(program, geometry, material.color, material.opacity, material.map)
            is PointsMaterial -> setPointsUniforms(program, material)
            is MeshPhongMaterial -> setPhongUniforms(program, geometry, material)
            is MeshLambertMaterial -> setLambertUniforms(program, geometry, material)
            is MeshNormalMaterial -> program.uniforms["opacity"]?.let { gl.uniform1f(it.location, material.opacity) }
            is SpriteMaterial -> setSpriteUniforms(program, geometry, obj as Sprite, material)
            is ShaderMaterial -> for (name in material.uniforms.names) {
                val info = program.uniforms[name] ?: continue
                uniforms.set(info, material.uniforms.get(name)!!.value)
            }
        }
        if (material is LineDashedMaterial) {
            program.uniforms["scale"]?.let { gl.uniform1f(it.location, material.scale) }
            program.uniforms["dashSize"]?.let { gl.uniform1f(it.location, material.dashSize) }
            program.uniforms["totalSize"]?.let { gl.uniform1f(it.location, material.dashSize + material.gapSize) }
        }
        if (material !is ShaderMaterial) program.uniforms["alphaTest"]?.let { gl.uniform1f(it.location, material.alphaTest) }
        if (fog != null) {
            program.uniforms["fogColor"]?.let {
                if (renderTarget == null) outputColor.copyLinearToSRGB(fog.color) else outputColor.copy(fog.color)
                gl.uniform3f(it.location, outputColor.r, outputColor.g, outputColor.b)
            }
            when (fog) {
                is Fog -> {
                    program.uniforms["fogNear"]?.let { gl.uniform1f(it.location, fog.near) }
                    program.uniforms["fogFar"]?.let { gl.uniform1f(it.location, fog.far) }
                }
                is FogExp2 -> program.uniforms["fogDensity"]?.let { gl.uniform1f(it.location, fog.density) }
            }
        }
        if (morphTargetsCount > 0) morphtargets.update(morphTargetInfluences!!, geometry, program)
        bindAttributes(program, obj, geometry)

        val mode = when (obj) {
            is LineSegments -> GL.LINES
            is LineLoop -> GL.LINE_LOOP
            is Line -> GL.LINE_STRIP
            is Points -> GL.POINTS
            else -> if (wireframe) GL.LINES else GL.TRIANGLES
        }
        if (wireframe) gl.lineWidth(pixelRatio)
        if (obj is Line && material is LineBasicMaterial) gl.lineWidth(material.linewidth * pixelRatio)
        if (index == null) {
            if (instanceCount < 0) gl.drawArrays(mode, drawStart.toInt(), drawCount.toInt())
            else gl.drawArraysInstanced(mode, drawStart.toInt(), drawCount.toInt(), instanceCount)
        } else {
            attributes.update(index, GL.ELEMENT_ARRAY_BUFFER)
            val indexBuffer = attributes.get(index)!!
            val offset = drawStart.toInt() * indexBuffer.bytesPerElement
            if (instanceCount < 0) gl.drawElements(mode, drawCount.toInt(), indexBuffer.type, offset)
            else gl.drawElementsInstanced(mode, drawCount.toInt(), indexBuffer.type, offset, instanceCount)
        }
    }

    /**
     * Returns how many instances the instanced attributes of [geometry] can supply, as the
     * `_maxInstanceCount` that Three.js `WebGLBindingStates` records, or [Int.MAX_VALUE] without any.
     */
    private fun maxInstanceCount(geometry: BufferGeometry): Int {
        var result = Int.MAX_VALUE
        for (name in geometry.attributeNames) {
            val attribute = geometry.getAttribute(name) ?: continue
            val meshPerAttribute = meshPerAttribute(attribute) ?: continue
            result = minOf(result, (meshPerAttribute.toLong() * attribute.count).coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
        }
        return result
    }

    /** Returns the instance divisor of [attribute], or null when it advances per vertex. */
    private fun meshPerAttribute(attribute: BufferAttribute): Int? = when (attribute) {
        is InstancedBufferAttribute -> attribute.meshPerAttribute
        is InterleavedBufferAttribute if attribute.data is InstancedInterleavedBuffer ->
            attribute.data.meshPerAttribute

        else -> null
    }

    /**
     * Returns the compiled program for [material], compiling it on first use. Built-in materials get
     * `#version 300 es` and the defines for their features, as Three.js `WebGLPrograms` does; raw shaders
     * get only the version line, when the material sets one. Programs are cached by their final source.
     */
    private fun getProgram(
        material: Material, obj: Object3D, geometry: BufferGeometry, morphTargetsCount: Int, fog: SceneFog?,
    ): WebGLProgram {
        val instancing = obj is InstancedMesh
        val instancingColor = obj is InstancedMesh && obj.instanceColor != null
        val instancingDefines = buildList {
            if (instancing) add("#define USE_INSTANCING")
            if (instancingColor) add("#define USE_INSTANCING_COLOR")
        }
        val lightDefines = listOf(
            "#define NUM_DIR_LIGHTS ${lights.directional.size}",
            "#define NUM_POINT_LIGHTS ${lights.point.size}",
            "#define NUM_HEMI_LIGHTS ${lights.hemisphere.size}",
        )
        fun sideDefines(flatShading: Boolean) = buildList {
            if (flatShading) add("#define FLAT_SHADED")
            if (material.side == Side.Double) add("#define DOUBLE_SIDED")
            if (material.side == Side.Back) add("#define FLIP_SIDED")
        }
        val (vertexGlsl, fragmentGlsl) = when (material) {
            is MeshBasicMaterial -> builtInSource(
                ShaderLib.basicVertex, ShaderLib.basicFragment, material, material.map, geometry, morphTargetsCount,
                fog.takeIf { material.fog }, instancingDefines,
            )
            is LineBasicMaterial -> builtInSource(
                ShaderLib.basicVertex, ShaderLib.basicFragment, material, material.map, geometry, morphTargetsCount,
                fog.takeIf { material.fog },
            )
            is PointsMaterial -> builtInSource(
                ShaderLib.pointsVertex, ShaderLib.pointsFragment, material, material.map, geometry, morphTargetsCount,
                fog.takeIf { material.fog },
                if (material.sizeAttenuation) listOf("#define USE_SIZEATTENUATION") else emptyList(),
            )
            is MeshPhongMaterial -> builtInSource(
                ShaderLib.phongVertex, ShaderLib.phongFragment, material, material.map, geometry, morphTargetsCount,
                fog.takeIf { material.fog },
                instancingDefines + lightDefines + sideDefines(material.flatShading) + "#define USE_SPECULAR",
            )
            is MeshLambertMaterial -> builtInSource(
                ShaderLib.phongVertex, ShaderLib.phongFragment, material, material.map, geometry, morphTargetsCount,
                fog.takeIf { material.fog }, instancingDefines + lightDefines + sideDefines(material.flatShading),
            )
            is MeshNormalMaterial -> builtInSource(
                ShaderLib.phongVertex, ShaderLib.normalFragment, material, null, geometry, morphTargetsCount,
                null, instancingDefines + sideDefines(material.flatShading),
            )
            is SpriteMaterial -> builtInSource(
                ShaderLib.spriteVertex, ShaderLib.spriteFragment, material, material.map, geometry, 0,
                fog.takeIf { material.fog },
                if (material.sizeAttenuation) listOf("#define USE_SIZEATTENUATION") else emptyList(),
            )
            is RawShaderMaterial -> {
                val versionString = material.glslVersion?.let { "#version ${it.version}\n" } ?: ""
                (versionString + material.vertexShader) to (versionString + material.fragmentShader)
            }
            is ShaderMaterial -> WebGLProgram.shaderMaterialSource(material, instancing, instancingColor)
            else -> throw IllegalArgumentException("WebGLRenderer cannot draw ${material::class.simpleName}.")
        }
        val key = vertexGlsl to fragmentGlsl
        val program = programs.getOrPut(key) { WebGLProgram(gl, vertexGlsl, fragmentGlsl) }
        val materialKeys = materialPrograms.getOrPut(material) {
            val listener: (DispatchedEvent) -> Unit = { releaseMaterial(material) }
            materialListeners[material] = listener
            material.addEventListener("dispose", listener)
            mutableSetOf()
        }
        if (materialKeys.add(key)) programUsers[key] = (programUsers[key] ?: 0) + 1
        return program
    }

    private fun releaseMaterial(material: Material) {
        materialListeners.remove(material)?.let { material.removeEventListener("dispose", it) }
        for (key in materialPrograms.remove(material).orEmpty()) {
            val remaining = programUsers.getValue(key) - 1
            if (remaining == 0) {
                programUsers.remove(key)
                programs.remove(key)?.dispose()
            } else programUsers[key] = remaining
        }
    }

    /** Prepends `#version 300 es` and the defines of the material's features, as Three.js `WebGLProgram` does. */
    private fun builtInSource(
        vertexShader: String, fragmentShader: String, material: Material, map: Texture?, geometry: BufferGeometry,
        morphTargetsCount: Int, fog: SceneFog?, extraDefines: List<String> = emptyList(),
    ): Pair<String, String> {
        val defines = buildList {
            addAll(extraDefines)
            if (renderTarget != null) add("#define LINEAR_OUTPUT")
            if (material.transparent == false && material.blending == Blending.Normal) add("#define OPAQUE")
            if (fog != null) add("#define USE_FOG")
            if (fog is FogExp2) add("#define FOG_EXP2")
            if (material.alphaTest > 0.0) add("#define USE_ALPHATEST")
            if (material.premultipliedAlpha) add("#define PREMULTIPLIED_ALPHA")
            if (map != null) add("#define USE_MAP")
            if (material is LineDashedMaterial) add("#define USE_DASH")
            if (material.vertexColors && geometry.hasAttribute("color")) add("#define USE_COLOR")
            if (morphTargetsCount > 0) {
                add("#define USE_MORPHTARGETS")
                add("#define MORPHTARGETS_COUNT $morphTargetsCount")
                add("#define MORPHTARGETS_TEXTURE_STRIDE 1")
            }
        }
        val prefix = (listOf("#version 300 es") + defines).joinToString("\n", postfix = "\n")
        return (prefix + vertexShader) to (prefix + fragmentShader)
    }

    /** Applies culling, blending, and depth settings. [mirrored] flips the winding of front faces. */
    private fun setMaterialState(material: Material, mirrored: Boolean) {
        if (material.alphaToCoverage) gl.enable(GL.SAMPLE_ALPHA_TO_COVERAGE)
        else gl.disable(GL.SAMPLE_ALPHA_TO_COVERAGE)
        if (material.side == Side.Double) {
            gl.disable(GL.CULL_FACE)
        } else {
            gl.enable(GL.CULL_FACE)
            gl.cullFace(if (material.side == Side.Front) GL.BACK else GL.FRONT)
        }
        gl.frontFace(if (mirrored) GL.CW else GL.CCW)

        val blending = if (material.blending == Blending.Normal && !material.transparent) Blending.None else material.blending
        if (blending == Blending.Custom) {
            gl.enable(GL.BLEND)
            gl.blendEquation(material.blendEquation.glValue)
            gl.blendFunc(material.blendSrc.glValue, material.blendDst.glValue)
        } else {
            setBlending(blending, material.premultipliedAlpha)
        }

        if (material.depthTest) gl.enable(GL.DEPTH_TEST) else gl.disable(GL.DEPTH_TEST)
        gl.depthMask(material.depthWrite)
    }

    /** Applies [blending] as Three.js `WebGLState.setBlending` does for the built-in modes. */
    private fun setBlending(blending: Blending, premultipliedAlpha: Boolean) {
        if (blending == Blending.None) {
            gl.disable(GL.BLEND)
            return
        }
        gl.enable(GL.BLEND)
        gl.blendEquation(GL.FUNC_ADD)
        if (premultipliedAlpha) {
            when (blending) {
                Blending.Normal -> gl.blendFuncSeparate(GL.ONE, GL.ONE_MINUS_SRC_ALPHA, GL.ONE, GL.ONE_MINUS_SRC_ALPHA)
                Blending.Additive -> gl.blendFunc(GL.ONE, GL.ONE)
                Blending.Subtractive -> gl.blendFuncSeparate(GL.ZERO, GL.ONE_MINUS_SRC_COLOR, GL.ZERO, GL.ONE)
                Blending.Multiply -> gl.blendFuncSeparate(GL.DST_COLOR, GL.ONE_MINUS_SRC_ALPHA, GL.ZERO, GL.ONE)
                Blending.None, Blending.Custom -> Unit
            }
        } else {
            when (blending) {
                Blending.Normal -> gl.blendFuncSeparate(GL.SRC_ALPHA, GL.ONE_MINUS_SRC_ALPHA, GL.ONE, GL.ONE_MINUS_SRC_ALPHA)
                Blending.Additive -> gl.blendFuncSeparate(GL.SRC_ALPHA, GL.ONE, GL.ONE, GL.ONE)
                Blending.Subtractive, Blending.Multiply -> throw IllegalArgumentException(
                    "$blending blending requires material.premultipliedAlpha = true.",
                )
                Blending.None, Blending.Custom -> Unit
            }
        }
    }

    /** Sets the camera and object matrices that the program declares. */
    private fun setBuiltInUniforms(program: WebGLProgram, obj: Object3D, camera: Camera) {
        val programUniforms = program.uniforms
        uniforms.setMatrix4(programUniforms["modelMatrix"], obj.matrixWorld)
        uniforms.setMatrix4(programUniforms["modelViewMatrix"], obj.modelViewMatrix)
        uniforms.setMatrix4(programUniforms["projectionMatrix"], projection)
        uniforms.setMatrix4(programUniforms["viewMatrix"], camera.matrixWorldInverse)
        programUniforms["normalMatrix"]?.let { uniforms.setMatrix3(it, obj.normalMatrix) }
        programUniforms["isOrthographic"]?.let { gl.uniform1i(it.location, if (camera is OrthographicCamera) 1 else 0) }
        programUniforms["cameraPosition"]?.let { gl.uniform3f(it.location, cameraPosition.x, cameraPosition.y, cameraPosition.z) }
    }

    /**
     * Sets the uniforms of the Phong shaders, as Three.js `refreshUniformsPhong` does, and the light
     * uniforms of the current view.
     */
    private fun setPhongUniforms(program: WebGLProgram, geometry: BufferGeometry, material: MeshPhongMaterial) {
        setBasicUniforms(program, geometry, material.color, material.opacity, material.map)
        val programUniforms = program.uniforms
        val specular = material.specular
        // Without directional lights the compiler removes the specular uniforms.
        programUniforms["specular"]?.let { gl.uniform3f(it.location, specular.r, specular.g, specular.b) }
        programUniforms["shininess"]?.let { gl.uniform1f(it.location, maxOf(material.shininess, 1e-4)) }
        val emissive = material.emissive
        val intensity = material.emissiveIntensity
        programUniforms["emissive"]?.let { gl.uniform3f(it.location, emissive.r * intensity, emissive.g * intensity, emissive.b * intensity) }
        setLightUniforms(program)
    }

    /** Sets the uniforms of the Lambert shaders, as Three.js `refreshUniformsLambert` does, and the lights. */
    private fun setLambertUniforms(program: WebGLProgram, geometry: BufferGeometry, material: MeshLambertMaterial) {
        setBasicUniforms(program, geometry, material.color, material.opacity, material.map)
        val emissive = material.emissive
        val intensity = material.emissiveIntensity
        program.uniforms["emissive"]?.let { gl.uniform3f(it.location, emissive.r * intensity, emissive.g * intensity, emissive.b * intensity) }
        setLightUniforms(program)
    }

    /** Sets the sprite color, map, rotation and center, as Three.js `refreshUniformsSprites` does. */
    private fun setSpriteUniforms(program: WebGLProgram, geometry: BufferGeometry, sprite: Sprite, material: SpriteMaterial) {
        setBasicUniforms(program, geometry, material.color, material.opacity, material.map)
        program.uniforms["rotation"]?.let { gl.uniform1f(it.location, material.rotation) }
        program.uniforms["center"]?.let { gl.uniform2f(it.location, sprite.center.x, sprite.center.y) }
    }

    /** Sets the ambient, directional, point and hemisphere light uniforms of the current view. */
    private fun setLightUniforms(program: WebGLProgram) {
        val programUniforms = program.uniforms
        val ambient = lights.ambient
        programUniforms["ambientLightColor"]?.let { gl.uniform3f(it.location, ambient.r, ambient.g, ambient.b) }
        for ((index, light) in lights.point.withIndex()) {
            programUniforms["pointLights[$index].position"]?.let {
                gl.uniform3f(it.location, light.position.x, light.position.y, light.position.z)
            }
            programUniforms["pointLights[$index].color"]?.let { gl.uniform3f(it.location, light.color.r, light.color.g, light.color.b) }
            programUniforms["pointLights[$index].distance"]?.let { gl.uniform1f(it.location, light.distance) }
            programUniforms["pointLights[$index].decay"]?.let { gl.uniform1f(it.location, light.decay) }
        }
        for ((index, light) in lights.directional.withIndex()) {
            programUniforms["directionalLights[$index].direction"]?.let {
                gl.uniform3f(it.location, light.direction.x, light.direction.y, light.direction.z)
            }
            programUniforms["directionalLights[$index].color"]?.let {
                gl.uniform3f(it.location, light.color.r, light.color.g, light.color.b)
            }
        }
        for ((index, light) in lights.hemisphere.withIndex()) {
            programUniforms["hemisphereLights[$index].direction"]?.let {
                gl.uniform3f(it.location, light.direction.x, light.direction.y, light.direction.z)
            }
            programUniforms["hemisphereLights[$index].skyColor"]?.let {
                gl.uniform3f(it.location, light.skyColor.r, light.skyColor.g, light.skyColor.b)
            }
            programUniforms["hemisphereLights[$index].groundColor"]?.let {
                gl.uniform3f(it.location, light.groundColor.r, light.groundColor.g, light.groundColor.b)
            }
        }
    }

    /** Sets the uniforms of the points shaders, as Three.js `refreshUniformsPoints` does. */
    private fun setPointsUniforms(program: WebGLProgram, material: PointsMaterial) {
        val programUniforms = program.uniforms
        val color = material.color
        programUniforms["diffuse"]?.let { gl.uniform3f(it.location, color.r, color.g, color.b) }
        programUniforms["opacity"]?.let { gl.uniform1f(it.location, material.opacity) }
        programUniforms["size"]?.let { gl.uniform1f(it.location, material.size * pixelRatio) }
        programUniforms["scale"]?.let {
            val cssHeight = height?.toDouble() ?: (gl.drawingBufferHeight / pixelRatio)
            gl.uniform1f(it.location, cssHeight * 0.5)
        }
        val map = material.map ?: return
        if (map.matrixAutoUpdate) map.updateMatrix()
        uniforms.setMatrix3(programUniforms["uvTransform"], map.matrix)
        programUniforms["map"]?.let { uniforms.set(it, map) }
    }

    // Built-in uniforms are optional: drivers remove the ones a variant does not use, for example
    // `opacity` when OPAQUE forces full alpha.

    /** Sets the uniforms of the basic shaders: color, opacity, and map with its UV transform. */
    private fun setBasicUniforms(program: WebGLProgram, geometry: BufferGeometry, color: Color, opacity: Double, map: Texture?) {
        val programUniforms = program.uniforms
        programUniforms["diffuse"]?.let { gl.uniform3f(it.location, color.r, color.g, color.b) }
        programUniforms["opacity"]?.let { gl.uniform1f(it.location, opacity) }
        if (map == null) return
        val uv = requireNotNull(geometry.getAttribute("uv")) { "A color texture requires UV coordinates." }
        require(uv.itemSize == 2 && uv.count == geometry.getAttribute("position")?.count) {
            "Each vertex must have two UV components."
        }
        if (map.matrixAutoUpdate) map.updateMatrix()
        uniforms.setMatrix3(programUniforms["mapTransform"], map.matrix)
        programUniforms["map"]?.let { uniforms.set(it, map) }
    }

    /**
     * Points each active attribute of [program] at the geometry attribute with the same name, or at the
     * instance matrices and colors of an [InstancedMesh], as Three.js `WebGLBindingStates` does. Instanced
     * attributes advance once per `meshPerAttribute` instances. Matrix attributes span consecutive
     * locations, one per column. Attributes the geometry lacks are disabled and read as `(0, 0, 0, 1)`.
     */
    private fun bindAttributes(program: WebGLProgram, obj: Object3D, geometry: BufferGeometry) {
        val used = mutableSetOf<Int>()
        for ((name, location) in program.attributes) {
            val attribute = when {
                name == "instanceMatrix" && obj is InstancedMesh -> obj.instanceMatrix
                name == "instanceColor" && obj is InstancedMesh -> obj.instanceColor
                else -> geometry.getAttribute(name)
            } ?: continue
            val locationSize = program.attributeLocationSizes[name] ?: 1
            val interleaved = attribute as? InterleavedBufferAttribute
            val storage = interleaved?.data?.storage ?: attribute
            val divisor = meshPerAttribute(attribute) ?: 0
            attributes.update(storage, GL.ARRAY_BUFFER)
            val buffer = attributes.get(storage)!!
            val type = buffer.type
            val elementSize = buffer.bytesPerElement
            if (locationSize == 1) {
                require(attribute.itemSize in 1..4) { "Attribute '$name' must have one to four components." }
                val stride = (interleaved?.data?.stride ?: 0) * elementSize
                val offset = (interleaved?.offset ?: 0) * elementSize
                gl.enableVertexAttribArray(location)
                if (type != GL.FLOAT && type != GL.HALF_FLOAT && attribute.gpuType == GpuType.Int) {
                    gl.vertexAttribIPointer(location, attribute.itemSize, type, stride, offset)
                } else {
                    gl.vertexAttribPointer(location, attribute.itemSize, type, attribute.normalized, stride, offset)
                }
                gl.vertexAttribDivisor(location, divisor)
                used.add(location)
            } else {
                // A matrix attribute is read one column per location, as in Three.js.
                val columnSize = attribute.itemSize / locationSize
                val stride = (interleaved?.data?.stride ?: attribute.itemSize) * elementSize
                val baseOffset = (interleaved?.offset ?: 0) * elementSize
                for (column in 0 until locationSize) {
                    gl.enableVertexAttribArray(location + column)
                    gl.vertexAttribPointer(
                        location + column, columnSize, type, attribute.normalized, stride,
                        baseOffset + column * columnSize * elementSize,
                    )
                    gl.vertexAttribDivisor(location + column, divisor)
                    used.add(location + column)
                }
            }
        }
        for (location in enabledAttributes - used) {
            gl.disableVertexAttribArray(location)
            gl.vertexAttribDivisor(location, 0)
        }
        enabledAttributes.clear()
        enabledAttributes.addAll(used)
    }

    /** Releases the shader programs, buffers, and textures owned by this renderer. */
    fun dispose() {
        for (location in enabledAttributes) gl.disableVertexAttribArray(location)
        enabledAttributes.clear()
        gl.useProgram(null)
        textures.dispose()
        geometries.dispose()
        attributes.dispose()
        morphtargets.dispose()
        materialPrograms.keys.toList().forEach(::releaseMaterial)
        setRenderTarget(null)
        renderTargets.dispose()
    }
}
