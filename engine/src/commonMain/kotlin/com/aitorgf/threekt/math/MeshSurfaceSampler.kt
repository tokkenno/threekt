package com.aitorgf.threekt.math

import com.aitorgf.threekt.core.BufferAttribute
import com.aitorgf.threekt.objects.Mesh
import kotlin.js.JsExport
import kotlin.random.Random

/** Samples a mesh in local space with probability proportional to triangle area and optional vertex weights. */
@JsExport
class MeshSurfaceSampler(mesh: Mesh) {
    private val geometry = mesh.geometry
    private val index = geometry.getIndex()
    private val position = requireNotNull(geometry.getAttribute("position"))
    private val normal = geometry.getAttribute("normal")
    private val color = geometry.getAttribute("color")
    private val uv = geometry.getAttribute("uv")
    private val face = Triangle()
    private val sampleColor = Vector3()
    private var weight: BufferAttribute? = null
    private var random: () -> Double = { Random.nextDouble() }
    private var distribution = FloatArray(0)

    /** Uses the attribute's X component as a nonnegative weight. Call [build] after changing it. */
    fun setWeightAttribute(name: String?): MeshSurfaceSampler = apply {
        weight = name?.let(geometry::getAttribute)
    }

    /** Sets a random generator returning values in [0, 1). */
    fun setRandomGenerator(generator: () -> Double): MeshSurfaceSampler = apply { random = generator }

    /** Builds the cumulative distribution in linear time. */
    fun build(): MeshSurfaceSampler = apply {
        val weights = weight
        var total = 0.0
        distribution = FloatArray((index?.count ?: position.count) / 3) { faceIndex ->
            val a = vertexIndex(faceIndex * 3)
            val b = vertexIndex(faceIndex * 3 + 1)
            val c = vertexIndex(faceIndex * 3 + 2)
            readFace(position, a, b, c)
            val multiplier = if (weights == null) 1.0 else weights.getX(a) + weights.getX(b) + weights.getX(c)
            total += (face.getArea() * multiplier).toFloat()
            total.toFloat()
        }
    }

    /** Writes a random position and the optional normal, color and UV. Requires a nonempty, positive distribution. */
    fun sample(
        targetPosition: Vector3,
        targetNormal: Vector3? = null,
        targetColor: Color? = null,
        targetUV: Vector2? = null,
    ): MeshSurfaceSampler = apply {
        val total = distribution.lastOrNull() ?: 0f
        check(total > 0) { "Build a positive surface distribution before sampling." }
        val value = random() * total
        var start = 0
        var end = distribution.lastIndex
        while (start < end) {
            val mid = (start + end) / 2
            if (value < distribution[mid]) end = mid else start = mid + 1
        }

        var u = random()
        var v = random()
        if (u + v > 1) {
            u = 1 - u
            v = 1 - v
        }
        val w = 1 - u - v
        val a = vertexIndex(start * 3)
        val b = vertexIndex(start * 3 + 1)
        val c = vertexIndex(start * 3 + 2)
        fun interpolate(target: Vector3) {
            target.set(0.0, 0.0, 0.0).addScaledVector(face.a, u).addScaledVector(face.b, v).addScaledVector(face.c, w)
        }
        readFace(position, a, b, c)
        interpolate(targetPosition)
        if (targetNormal != null) {
            if (normal == null) face.getNormal(targetNormal)
            else {
                readFace(normal, a, b, c)
                interpolate(targetNormal)
                targetNormal.normalize()
            }
        }
        if (targetColor != null && color != null) {
            readFace(color, a, b, c)
            interpolate(sampleColor)
            targetColor.set(sampleColor.x, sampleColor.y, sampleColor.z)
        }
        if (targetUV != null && uv != null) {
            targetUV.set(
                uv.getX(a) * u + uv.getX(b) * v + uv.getX(c) * w,
                uv.getY(a) * u + uv.getY(b) * v + uv.getY(c) * w,
            )
        }
    }

    private fun vertexIndex(slot: Int): Int = index?.getX(slot)?.toInt() ?: slot

    private fun readFace(attribute: BufferAttribute, a: Int, b: Int, c: Int) {
        face.a.fromBufferAttribute(attribute, a)
        face.b.fromBufferAttribute(attribute, b)
        face.c.fromBufferAttribute(attribute, c)
    }
}
