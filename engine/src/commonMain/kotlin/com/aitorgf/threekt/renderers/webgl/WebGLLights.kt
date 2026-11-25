package com.aitorgf.threekt.renderers.webgl

import com.aitorgf.threekt.lights.AmbientLight
import com.aitorgf.threekt.lights.DirectionalLight
import com.aitorgf.threekt.lights.Light
import com.aitorgf.threekt.math.Color
import com.aitorgf.threekt.math.Matrix4
import com.aitorgf.threekt.math.Vector3

/**
 * Light uniforms of a view, as Three.js `WebGLLights`. [setup] sums ambient lights and collects the
 * color of each directional light, multiplied by its intensity; [setupView] computes their directions
 * in view space. Other light types are not supported yet.
 */
internal class WebGLLights {
    /** Direction toward the light in view space, and its color times intensity. */
    class DirectionalUniforms(val direction: Vector3 = Vector3(), val color: Color = Color(0.0, 0.0, 0.0))

    /** Sum of the ambient lights. */
    val ambient = Color(0.0, 0.0, 0.0)

    /** One entry per directional light, in the order of the lights passed to [setup]. */
    val directional = mutableListOf<DirectionalUniforms>()

    private val vector = Vector3()

    fun setup(lights: List<Light>) {
        ambient.set(0.0, 0.0, 0.0)
        var directionalLength = 0
        for (light in lights) {
            val color = light.color
            val intensity = light.intensity
            when (light) {
                is AmbientLight -> {
                    ambient.r += color.r * intensity
                    ambient.g += color.g * intensity
                    ambient.b += color.b * intensity
                }
                is DirectionalLight -> {
                    if (directional.size == directionalLength) directional.add(DirectionalUniforms())
                    directional[directionalLength].color.copy(color).multiplyScalar(intensity)
                    directionalLength++
                }
            }
        }
        while (directional.size > directionalLength) directional.removeAt(directional.lastIndex)
    }

    fun setupView(lights: List<Light>, viewMatrix: Matrix4) {
        var directionalLength = 0
        for (light in lights) {
            if (light !is DirectionalLight) continue
            val uniforms = directional[directionalLength]
            uniforms.direction.setFromMatrixPosition(light.matrixWorld)
            vector.setFromMatrixPosition(light.target.matrixWorld)
            uniforms.direction.sub(vector)
            uniforms.direction.transformDirection(viewMatrix)
            directionalLength++
        }
    }
}
