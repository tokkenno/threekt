package com.aitorgf.threekt.renderers.webgl

import com.aitorgf.threekt.core.BufferGeometry
import kotlin.math.ceil

/**
 * Uploads position morph targets to a `sampler2DArray` with one layer per target, as Three.js
 * `WebGLMorphtargets` does, and sets the morph uniforms of a draw. Shaders read texel
 * `gl_VertexID * MORPHTARGETS_TEXTURE_STRIDE` of each layer. The texture of a geometry is built once and
 * rebuilt only when its number of targets changes. Normal and color morph targets are not supported yet.
 */
internal class WebGLMorphtargets(private val gl: GLContext, private val uniforms: WebGLUniforms) {
    private class Entry(val count: Int, val texture: Any, val width: Int, val height: Int)

    private val entries = mutableMapOf<BufferGeometry, Entry>()
    private var influenceArray = FloatArray(0)

    /** Binds the morph texture of [geometry] and sets [influences] on [program]. */
    fun update(influences: DoubleArray, geometry: BufferGeometry, program: WebGLProgram) {
        val targets = geometry.morphAttributes["position"].orEmpty()
        val morphTargetsCount = targets.size
        var entry = entries[geometry]
        if (entry == null || entry.count != morphTargetsCount) {
            entry?.let { gl.deleteTexture(it.texture) }
            entry = createEntry(geometry, morphTargetsCount)
            entries[geometry] = entry
        }

        var morphInfluencesSum = 0.0
        for (index in 0 until morphTargetsCount) morphInfluencesSum += influences.getOrElse(index) { 0.0 }
        val morphBaseInfluence = if (geometry.morphTargetsRelative) 1.0 else 1.0 - morphInfluencesSum

        if (influenceArray.size != morphTargetsCount) influenceArray = FloatArray(morphTargetsCount)
        for (index in 0 until morphTargetsCount) influenceArray[index] = influences.getOrElse(index) { 0.0 }.toFloat()

        val programUniforms = program.uniforms
        programUniforms["morphTargetBaseInfluence"]?.let { gl.uniform1f(it.location, morphBaseInfluence) }
        programUniforms["morphTargetInfluences"]?.let { gl.uniform1fv(it.location, influenceArray) }
        programUniforms["morphTargetsTexture"]?.let {
            val unit = uniforms.allocateTextureUnit()
            gl.activeTexture(GL.TEXTURE0 + unit)
            gl.bindTexture(GL.TEXTURE_2D_ARRAY, entry.texture)
            gl.uniform1i(it.location, unit)
        }
        programUniforms["morphTargetsTextureSize"]?.let { gl.uniform2i(it.location, entry.width, entry.height) }
    }

    private fun createEntry(geometry: BufferGeometry, morphTargetsCount: Int): Entry {
        val vertexDataCount = 1
        val position = requireNotNull(geometry.getAttribute("position")) { "Morph targets require positions." }
        var width = position.count * vertexDataCount
        var height = 1
        val maxTextureSize = gl.getParameterInt(GL.MAX_TEXTURE_SIZE)
        if (width > maxTextureSize) {
            height = ceil(width.toDouble() / maxTextureSize).toInt()
            width = maxTextureSize
        }
        val buffer = FloatArray(width * height * 4 * morphTargetsCount)
        for ((layer, morphTarget) in geometry.morphAttributes["position"].orEmpty().withIndex()) {
            val offset = width * height * 4 * layer
            for (vertex in 0 until morphTarget.count) {
                val stride = vertex * vertexDataCount
                buffer[offset + stride * 4] = morphTarget.getX(vertex).toFloat()
                buffer[offset + stride * 4 + 1] = morphTarget.getY(vertex).toFloat()
                buffer[offset + stride * 4 + 2] = morphTarget.getZ(vertex).toFloat()
            }
        }
        val texture = gl.createTexture()
        gl.bindTexture(GL.TEXTURE_2D_ARRAY, texture)
        gl.texImage3D(GL.TEXTURE_2D_ARRAY, 0, GL.RGBA32F, width, height, morphTargetsCount, 0, GL.RGBA, GL.FLOAT, buffer)
        gl.texParameteri(GL.TEXTURE_2D_ARRAY, GL.TEXTURE_MIN_FILTER, GL.NEAREST)
        gl.texParameteri(GL.TEXTURE_2D_ARRAY, GL.TEXTURE_MAG_FILTER, GL.NEAREST)
        return Entry(morphTargetsCount, texture, width, height)
    }

    /** Deletes every morph texture. */
    fun dispose() {
        for (entry in entries.values) gl.deleteTexture(entry.texture)
        entries.clear()
    }
}
