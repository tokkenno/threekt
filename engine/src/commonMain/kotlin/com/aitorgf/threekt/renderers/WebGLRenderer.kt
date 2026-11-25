package com.aitorgf.threekt.renderers

import com.aitorgf.threekt.cameras.ArrayCamera
import com.aitorgf.threekt.cameras.Camera
import com.aitorgf.threekt.cameras.CubeCamera
import com.aitorgf.threekt.cameras.OrthographicCamera
import com.aitorgf.threekt.core.BufferGeometry
import com.aitorgf.threekt.core.GpuType
import com.aitorgf.threekt.core.InterleavedBufferAttribute
import com.aitorgf.threekt.core.Object3D
import com.aitorgf.threekt.materials.Blending
import com.aitorgf.threekt.materials.LineBasicMaterial
import com.aitorgf.threekt.materials.Material
import com.aitorgf.threekt.materials.MeshBasicMaterial
import com.aitorgf.threekt.materials.MeshPhongMaterial
import com.aitorgf.threekt.materials.PointsMaterial
import com.aitorgf.threekt.materials.RawShaderMaterial
import com.aitorgf.threekt.materials.ShaderMaterial
import com.aitorgf.threekt.materials.Side
import com.aitorgf.threekt.math.Color
import com.aitorgf.threekt.math.Matrix3
import com.aitorgf.threekt.math.Matrix4
import com.aitorgf.threekt.math.Vector3
import com.aitorgf.threekt.objects.Line
import com.aitorgf.threekt.objects.LineLoop
import com.aitorgf.threekt.objects.LineSegments
import com.aitorgf.threekt.objects.Mesh
import com.aitorgf.threekt.objects.Points
import com.aitorgf.threekt.renderers.shaders.ShaderLib
import com.aitorgf.threekt.renderers.webgl.GL
import com.aitorgf.threekt.renderers.webgl.GLContext
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
 * texture, vertex colors, and position morph targets, [PointsMaterial] draws sized points, and
 * [MeshPhongMaterial] is lit by ambient and directional lights. Built-in materials apply the scene's fog. [RawShaderMaterial] uses its own shaders. Opaque
 * objects are drawn before transparent ones, which are blended back to front.
 */
@JsExport
class WebGLRenderer(context: GLContext) {
    private val gl = context

    /** Limits and optional features of this renderer's WebGL2 context. */
    val capabilities = WebGLCapabilities(gl)

    private val textures = WebGLTextures(gl, capabilities)
    private val attributes = WebGLAttributes(gl)
    private val geometries = WebGLGeometries()
    private val uniforms = WebGLUniforms(gl, textures)
    private val morphtargets = WebGLMorphtargets(gl, uniforms)
    private val lights = WebGLLights()
    private val programs = mutableMapOf<Pair<String, String>, WebGLProgram>()
    private val indexBuffer = gl.createBuffer()
    private val enabledAttributes = mutableSetOf<Int>()
    private val renderList = WebGLRenderList()

    private val clearColor = Color(0.0, 0.0, 0.0)
    private var clearAlpha = 1.0
    private val outputColor = Color()

    private var pixelRatio = 1.0
    /** Height in CSS pixels from [setSize], or null before the first call. */
    private var height: Int? = null
    private val viewProjection = Matrix4()
    private val projection = Matrix4()
    private val modelView = Matrix4()
    private val normalMatrix = Matrix3()
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
        scene.updateMatrixWorld()
        if (camera is ArrayCamera) {
            renderArray(scene, camera)
        } else {
            camera.updateWorldMatrix(updateParents = true, updateChildren = false)
            renderView(scene, camera, 0, 0, gl.drawingBufferWidth, gl.drawingBufferHeight)
        }
    }

    /** Renders all six views into [target] and restores the previous drawing framebuffer and viewport. */
    internal fun renderCube(scene: Scene, camera: CubeCamera, target: WebGLCubeRenderTarget) {
        require(target.gl === gl) { "The cube render target belongs to a different WebGL context." }
        val framebuffer = gl.getParameterHandle(GL.DRAW_FRAMEBUFFER_BINDING)
        val viewport = gl.getParameterInts(GL.VIEWPORT, IntArray(4))
        scene.updateMatrixWorld()
        camera.coordinateSystem = CoordinateSystem.WebGL
        try {
            camera.update { face, faceCamera ->
                target.bindFace(face)
                renderView(scene, faceCamera, 0, 0, target.size, target.size)
            }
        } finally {
            gl.bindFramebuffer(GL.DRAW_FRAMEBUFFER, framebuffer)
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
                gl.scissor(x, y, width, height)
                renderView(scene, subcamera, x, y, width, height)
            }
        } finally {
            gl.disable(GL.SCISSOR_TEST)
        }
    }

    /** Clears to the scene background, or to the clear color when the scene has none. */
    private fun clear(scene: Scene, colorOnly: Boolean = false, reversedDepth: Boolean = false) {
        val background = scene.background
        // Colors are stored in linear working space, while the drawing buffer holds sRGB values.
        if (background != null) {
            outputColor.copyLinearToSRGB(background)
            gl.clearColor(outputColor.r, outputColor.g, outputColor.b, 1.0)
        } else {
            outputColor.copyLinearToSRGB(clearColor)
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
        cameraPosition.setFromMatrixPosition(camera.matrixWorld)

        gl.viewport(x, y, width, height)
        clear(scene, reversedDepth = camera.reversedDepth)
        gl.depthFunc(if (camera.reversedDepth) GL.GEQUAL else GL.LEQUAL)

        renderList.build(scene, viewProjection)
        lights.setup(renderList.lights)
        lights.setupView(renderList.lights, camera.matrixWorldInverse)
        for (item in renderList.opaque) renderObject(item.`object`, item.geometry, item.material, scene, camera)
        for (item in renderList.transparent) renderObject(item.`object`, item.geometry, item.material, scene, camera)
        gl.disable(GL.BLEND)
        gl.depthMask(true)
    }

    /** Draws one object, as `renderBufferDirect` does in Three.js. */
    private fun renderObject(`object`: Object3D, geometry: BufferGeometry, material: Material, scene: Scene, camera: Camera) {
        val position = geometry.getAttribute("position")
        val wireframe = `object` is Mesh && material.wireframe
        val index = if (wireframe) geometries.getWireframeIndices(geometry.index, position?.count ?: 0) else geometry.index
        val rangeFactor = if (wireframe) 2 else 1
        // Clamp the draw range to the available indices or vertices, as Three.js does.
        val drawRange = geometry.drawRange
        var drawStart = drawRange.start.toLong() * rangeFactor
        var drawEnd = drawStart + drawRange.count.toLong() * rangeFactor
        if (index != null) {
            drawStart = maxOf(drawStart, 0)
            drawEnd = minOf(drawEnd, index.size.toLong())
        } else if (position != null) {
            drawStart = maxOf(drawStart, 0)
            drawEnd = minOf(drawEnd, position.count.toLong())
        }
        val drawCount = drawEnd - drawStart
        // A negative count draws nothing; an unbounded one cannot be drawn without attributes or indices.
        if (drawCount < 0 || drawCount >= Int.MAX_VALUE) return

        val morphTargetInfluences = when (`object`) {
            is Mesh -> `object`.morphTargetInfluences
            is Line -> `object`.morphTargetInfluences
            is Points -> `object`.morphTargetInfluences
            else -> null
        }
        val morphTargetsCount = if (morphTargetInfluences != null) geometry.morphAttributes["position"]?.size ?: 0 else 0
        val fog = scene.fog
        val program = getProgram(material, geometry, morphTargetsCount, fog)
        gl.useProgram(program.handle)
        setMaterialState(material, `object`.matrixWorld.determinantAffine() < 0.0)

        uniforms.reset()
        setBuiltInUniforms(program, `object`, camera)
        when (material) {
            is MeshBasicMaterial -> setBasicUniforms(program, geometry, material.color, material.opacity, material.map)
            is LineBasicMaterial -> setBasicUniforms(program, geometry, material.color, material.opacity, material.map)
            is PointsMaterial -> setPointsUniforms(program, material)
            is MeshPhongMaterial -> setPhongUniforms(program, geometry, material)
            is ShaderMaterial -> for (name in material.uniforms.names) {
                val info = program.uniforms[name] ?: continue
                uniforms.set(info, material.uniforms.get(name)!!.value)
            }
        }
        if (material !is ShaderMaterial) program.uniforms["alphaTest"]?.let { gl.uniform1f(it.location, material.alphaTest) }
        if (fog != null) {
            program.uniforms["fogColor"]?.let {
                // Fog is blended after the sRGB encoding, so its color is converted to sRGB here.
                outputColor.copyLinearToSRGB(fog.color)
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
        bindAttributes(program, geometry)

        val mode = when (`object`) {
            is LineSegments -> GL.LINES
            is LineLoop -> GL.LINE_LOOP
            is Line -> GL.LINE_STRIP
            is Points -> GL.POINTS
            else -> if (wireframe) GL.LINES else GL.TRIANGLES
        }
        if (wireframe) gl.lineWidth(pixelRatio)
        if (`object` is Line && material is LineBasicMaterial) gl.lineWidth(material.linewidth * pixelRatio)
        if (index == null) {
            gl.drawArrays(mode, drawStart.toInt(), drawCount.toInt())
        } else {
            gl.bindBuffer(GL.ELEMENT_ARRAY_BUFFER, indexBuffer)
            gl.bufferIntData(GL.ELEMENT_ARRAY_BUFFER, index, GL.STREAM_DRAW)
            gl.drawElements(mode, drawCount.toInt(), GL.UNSIGNED_INT, drawStart.toInt() * Int.SIZE_BYTES)
        }
    }

    /**
     * Returns the compiled program for [material], compiling it on first use. Built-in materials get
     * `#version 300 es` and the defines for their features, as Three.js `WebGLPrograms` does; raw shaders
     * get only the version line, when the material sets one. Programs are cached by their final source.
     */
    private fun getProgram(material: Material, geometry: BufferGeometry, morphTargetsCount: Int, fog: SceneFog?): WebGLProgram {
        val (vertexGlsl, fragmentGlsl) = when (material) {
            is MeshBasicMaterial -> builtInSource(
                ShaderLib.basicVertex, ShaderLib.basicFragment, material, material.map, geometry, morphTargetsCount,
                fog.takeIf { material.fog },
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
                buildList {
                    add("#define NUM_DIR_LIGHTS ${lights.directional.size}")
                    add("#define NUM_HEMI_LIGHTS ${lights.hemisphere.size}")
                    if (material.flatShading) add("#define FLAT_SHADED")
                    if (material.side == Side.Double) add("#define DOUBLE_SIDED")
                    if (material.side == Side.Back) add("#define FLIP_SIDED")
                },
            )
            is RawShaderMaterial -> {
                val versionString = material.glslVersion?.let { "#version ${it.version}\n" } ?: ""
                (versionString + material.vertexShader) to (versionString + material.fragmentShader)
            }
            is ShaderMaterial -> WebGLProgram.shaderMaterialSource(material)
            else -> throw IllegalArgumentException("WebGLRenderer cannot draw ${material::class.simpleName}.")
        }
        return programs.getOrPut(vertexGlsl to fragmentGlsl) { WebGLProgram(gl, vertexGlsl, fragmentGlsl) }
    }

    /** Prepends `#version 300 es` and the defines of the material's features, as Three.js `WebGLProgram` does. */
    private fun builtInSource(
        vertexShader: String, fragmentShader: String, material: Material, map: Texture?, geometry: BufferGeometry,
        morphTargetsCount: Int, fog: SceneFog?, extraDefines: List<String> = emptyList(),
    ): Pair<String, String> {
        val defines = buildList {
            addAll(extraDefines)
            if (material.transparent == false && material.blending == Blending.Normal) add("#define OPAQUE")
            if (fog != null) add("#define USE_FOG")
            if (fog is FogExp2) add("#define FOG_EXP2")
            if (material.alphaTest > 0.0) add("#define USE_ALPHATEST")
            if (material.premultipliedAlpha) add("#define PREMULTIPLIED_ALPHA")
            if (map != null) add("#define USE_MAP")
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
        if (material.side == Side.Double) {
            gl.disable(GL.CULL_FACE)
        } else {
            gl.enable(GL.CULL_FACE)
            gl.cullFace(if (material.side == Side.Front) GL.BACK else GL.FRONT)
        }
        gl.frontFace(if (mirrored) GL.CW else GL.CCW)

        val blending = if (material.blending == Blending.Normal && !material.transparent) Blending.None else material.blending
        setBlending(blending, material.premultipliedAlpha)

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
                Blending.None -> Unit
            }
        } else {
            when (blending) {
                Blending.Normal -> gl.blendFuncSeparate(GL.SRC_ALPHA, GL.ONE_MINUS_SRC_ALPHA, GL.ONE, GL.ONE_MINUS_SRC_ALPHA)
                Blending.Additive -> gl.blendFuncSeparate(GL.SRC_ALPHA, GL.ONE, GL.ONE, GL.ONE)
                Blending.Subtractive, Blending.Multiply -> throw IllegalArgumentException(
                    "$blending blending requires material.premultipliedAlpha = true.",
                )
                Blending.None -> Unit
            }
        }
    }

    /** Sets the camera and object matrices that the program declares. */
    private fun setBuiltInUniforms(program: WebGLProgram, `object`: Object3D, camera: Camera) {
        val programUniforms = program.uniforms
        modelView.multiplyMatrices(camera.matrixWorldInverse, `object`.matrixWorld)
        uniforms.setMatrix4(programUniforms["modelMatrix"], `object`.matrixWorld)
        uniforms.setMatrix4(programUniforms["modelViewMatrix"], modelView)
        uniforms.setMatrix4(programUniforms["projectionMatrix"], projection)
        uniforms.setMatrix4(programUniforms["viewMatrix"], camera.matrixWorldInverse)
        programUniforms["normalMatrix"]?.let { uniforms.setMatrix3(it, normalMatrix.getNormalMatrix(modelView)) }
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
        val ambient = lights.ambient
        programUniforms["ambientLightColor"]?.let { gl.uniform3f(it.location, ambient.r, ambient.g, ambient.b) }
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
     * Points each active attribute of [program] at the geometry attribute with the same name. Attributes
     * the geometry lacks are disabled and read as `(0, 0, 0, 1)`.
     */
    private fun bindAttributes(program: WebGLProgram, geometry: BufferGeometry) {
        val used = mutableSetOf<Int>()
        for ((name, location) in program.attributes) {
            val attribute = geometry.getAttribute(name) ?: continue
            require(attribute.itemSize in 1..4) { "Attribute '$name' must have one to four components." }
            val interleaved = attribute as? InterleavedBufferAttribute
            val storage = interleaved?.data?.storage ?: attribute
            val bytesPerElement = interleaved?.data?.bytesPerElement ?: 0
            attributes.bind(storage)
            gl.enableVertexAttribArray(location)
            val type = attributes.componentType(storage)
            val stride = (interleaved?.data?.stride ?: 0) * bytesPerElement
            val offset = (interleaved?.offset ?: 0) * bytesPerElement
            if (type != GL.FLOAT && attribute.gpuType == GpuType.Int) {
                gl.vertexAttribIPointer(location, attribute.itemSize, type, stride, offset)
            } else {
                gl.vertexAttribPointer(location, attribute.itemSize, type, attribute.normalized, stride, offset)
            }
            used.add(location)
        }
        for (location in enabledAttributes - used) gl.disableVertexAttribArray(location)
        enabledAttributes.clear()
        enabledAttributes.addAll(used)
    }

    /** Releases the shader programs, buffers, and textures owned by this renderer. */
    fun dispose() {
        for (location in enabledAttributes) gl.disableVertexAttribArray(location)
        enabledAttributes.clear()
        gl.useProgram(null)
        textures.dispose()
        attributes.dispose()
        gl.deleteBuffer(indexBuffer)
        morphtargets.dispose()
        for (program in programs.values) program.dispose()
        programs.clear()
    }
}
