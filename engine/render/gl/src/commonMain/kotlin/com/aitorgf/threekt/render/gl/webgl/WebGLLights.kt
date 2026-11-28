package com.aitorgf.threekt.render.gl.webgl

import com.aitorgf.threekt.lights.AmbientLight
import com.aitorgf.threekt.lights.DirectionalLight
import com.aitorgf.threekt.lights.HemisphereLight
import com.aitorgf.threekt.lights.Light
import com.aitorgf.threekt.lights.PointLight
import com.aitorgf.threekt.math.Color
import com.aitorgf.threekt.math.Matrix4
import com.aitorgf.threekt.math.Vector3

/**
 * Light uniforms of a view, as Three.js `WebGLLights`. [setup] sums ambient lights and collects the
 * color of each directional light, multiplied by its intensity; [setupView] computes their directions
 * in view space. Point lights store their view-space position, range and decay. Hemisphere lights
 * provide separate sky and ground colors for indirect lighting.
 */
internal class WebGLLights {
    /** Direction toward the light in view space, and its color times intensity. */
    class DirectionalUniforms(val direction: Vector3 = Vector3(), val color: Color = Color(0.0, 0.0, 0.0))

    class HemisphereUniforms(
        val direction: Vector3 = Vector3(),
        val skyColor: Color = Color(),
        val groundColor: Color = Color(),
    )

    val hemisphere = mutableListOf<HemisphereUniforms>()

    /** Position in view space, color times intensity, cutoff distance and decay exponent. */
    class PointUniforms(
        val position: Vector3 = Vector3(),
        val color: Color = Color(0.0, 0.0, 0.0),
        var distance: Double = 0.0,
        var decay: Double = 0.0,
    )

    /** One entry per point light, in the order of the lights passed to [setup]. */
    val point = mutableListOf<PointUniforms>()

    /** Sum of the ambient lights. */
    val ambient = Color(0.0, 0.0, 0.0)

    /** One entry per directional light, in the order of the lights passed to [setup]. */
    val directional = mutableListOf<DirectionalUniforms>()

    private val vector = Vector3()

    fun setup(lights: List<Light>) {
        ambient.set(0.0, 0.0, 0.0)
        var directionalLength = 0
        var hemisphereLength = 0
        var pointLength = 0
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
                is PointLight -> {
                    if (point.size == pointLength) point.add(PointUniforms())
                    val uniforms = point[pointLength++]
                    uniforms.color.copy(color).multiplyScalar(intensity)
                    uniforms.distance = light.distance
                    uniforms.decay = light.decay
                }
                is HemisphereLight -> {
                    if (hemisphere.size == hemisphereLength) hemisphere.add(HemisphereUniforms())
                    val uniforms = hemisphere[hemisphereLength++]
                    uniforms.skyColor.copy(color).multiplyScalar(intensity)
                    uniforms.groundColor.copy(light.groundColor).multiplyScalar(intensity)
                }
            }
        }
        while (directional.size > directionalLength) directional.removeAt(directional.lastIndex)
        while (hemisphere.size > hemisphereLength) hemisphere.removeAt(hemisphere.lastIndex)
        while (point.size > pointLength) point.removeAt(point.lastIndex)
    }

    fun setupView(lights: List<Light>, viewMatrix: Matrix4) {
        var directionalLength = 0
        var hemisphereLength = 0
        var pointLength = 0
        for (light in lights) {
            when (light) {
                is PointLight -> point[pointLength++].position.setFromMatrixPosition(light.matrixWorld).applyMatrix4(viewMatrix)
                is DirectionalLight -> {
                    val uniforms = directional[directionalLength++]
                    uniforms.direction.setFromMatrixPosition(light.matrixWorld)
                    vector.setFromMatrixPosition(light.target.matrixWorld)
                    uniforms.direction.sub(vector).transformDirection(viewMatrix)
                }
                is HemisphereLight -> hemisphere[hemisphereLength++].direction
                    .setFromMatrixPosition(light.matrixWorld).transformDirection(viewMatrix)
            }
        }
    }
}
