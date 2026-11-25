package com.aitorgf.threekt.renderers.webgl

import com.aitorgf.threekt.math.Color
import com.aitorgf.threekt.math.Matrix3
import com.aitorgf.threekt.math.Matrix4
import com.aitorgf.threekt.math.Quaternion
import com.aitorgf.threekt.math.Vector2
import com.aitorgf.threekt.math.Vector3
import com.aitorgf.threekt.math.Vector4
import com.aitorgf.threekt.textures.Texture

/**
 * Uploads Kotlin values to program uniforms according to their GLSL type. Samplers take consecutive
 * texture units starting after [reset].
 */
internal class WebGLUniforms(private val gl: GLContext, private val textures: WebGLTextures) {
    private val matrix3 = FloatArray(9)
    private val matrix4 = FloatArray(16)
    private var nextTextureUnit = 0

    /** Makes the next sampler use texture unit `0`. Call it before setting the uniforms of a draw. */
    fun reset() {
        nextTextureUnit = 0
    }

    /** Reserves the next texture unit for a sampler that the renderer binds itself. */
    fun allocateTextureUnit(): Int = nextTextureUnit++

    /** Sets [uniform] from [value]. Throws [IllegalArgumentException] if the value does not fit its type. */
    fun set(uniform: WebGLProgram.UniformInfo, value: Any) {
        val location = uniform.location
        when (uniform.type) {
            GL.FLOAT -> gl.uniform1f(location, number(uniform, value))
            GL.INT, GL.BOOL -> gl.uniform1i(location, integer(uniform, value))
            GL.UNSIGNED_INT -> gl.uniform1ui(location, integer(uniform, value))
            GL.FLOAT_VEC2 -> {
                val vector = expect<Vector2>(uniform, value)
                gl.uniform2f(location, vector.x, vector.y)
            }
            GL.FLOAT_VEC3 -> when (value) {
                is Vector3 -> gl.uniform3f(location, value.x, value.y, value.z)
                is Color -> gl.uniform3f(location, value.r, value.g, value.b)
                else -> unsupported(uniform, value)
            }
            GL.FLOAT_VEC4 -> when (value) {
                is Vector4 -> gl.uniform4f(location, value.x, value.y, value.z, value.w)
                is Quaternion -> gl.uniform4f(location, value.x, value.y, value.z, value.w)
                else -> unsupported(uniform, value)
            }
            GL.FLOAT_MAT3 -> gl.uniformMatrix3fv(location, false, floats(expect<Matrix3>(uniform, value).elements, matrix3))
            GL.FLOAT_MAT4 -> gl.uniformMatrix4fv(location, false, floats(expect<Matrix4>(uniform, value).elements, matrix4))
            GL.SAMPLER_2D -> setSampler2D(uniform, value)
            else -> throw IllegalArgumentException("Uniform '${uniform.name}' has an unsupported GLSL type.")
        }
    }

    private fun setSampler2D(uniform: WebGLProgram.UniformInfo, value: Any) {
        val arraySize = uniform.arraySize
        if (arraySize == null) {
            val unit = allocateTextureUnit()
            textures.bind(expect<Texture>(uniform, value), unit)
            gl.uniform1i(uniform.location, unit)
            return
        }

        val images = expect<Array<*>>(uniform, value)
        require(images.size >= arraySize) { "Uniform '${uniform.name}' needs at least $arraySize textures." }
        val units = IntArray(arraySize)
        for (index in units.indices) {
            val image = images[index] as? Texture
                ?: throw IllegalArgumentException("Uniform '${uniform.name}[$index]' needs a texture.")
            val unit = allocateTextureUnit()
            units[index] = unit
            textures.bind(image, unit)
        }
        gl.uniform1iv(uniform.location, units)
    }

    /** Sets a `mat4` uniform from [matrix] when the program declares it. */
    fun setMatrix4(uniform: WebGLProgram.UniformInfo?, matrix: Matrix4) {
        if (uniform != null) gl.uniformMatrix4fv(uniform.location, false, floats(matrix.elements, matrix4))
    }

    /** Sets a `mat3` uniform from [matrix] when the program declares it. */
    fun setMatrix3(uniform: WebGLProgram.UniformInfo?, matrix: Matrix3) {
        if (uniform != null) gl.uniformMatrix3fv(uniform.location, false, floats(matrix.elements, matrix3))
    }

    private fun floats(elements: DoubleArray, target: FloatArray): FloatArray {
        for (index in target.indices) target[index] = elements[index].toFloat()
        return target
    }

    private fun number(uniform: WebGLProgram.UniformInfo, value: Any): Double =
        (value as? Number)?.toDouble() ?: unsupported(uniform, value)

    private fun integer(uniform: WebGLProgram.UniformInfo, value: Any): Int = when (value) {
        is Boolean -> if (value) 1 else 0
        is Number -> value.toInt()
        else -> unsupported(uniform, value)
    }

    private inline fun <reified T> expect(uniform: WebGLProgram.UniformInfo, value: Any): T =
        value as? T ?: unsupported(uniform, value)

    private fun unsupported(uniform: WebGLProgram.UniformInfo, value: Any): Nothing =
        throw IllegalArgumentException("Uniform '${uniform.name}' cannot be set from ${value::class.simpleName}.")
}
