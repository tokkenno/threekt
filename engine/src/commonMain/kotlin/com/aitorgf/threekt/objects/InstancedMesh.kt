package com.aitorgf.threekt.objects

import com.aitorgf.threekt.core.BufferGeometry
import com.aitorgf.threekt.core.InstancedBufferAttribute
import com.aitorgf.threekt.materials.Material
import com.aitorgf.threekt.math.Box3
import com.aitorgf.threekt.math.Color
import com.aitorgf.threekt.math.Matrix4
import com.aitorgf.threekt.math.Sphere
import kotlin.js.JsExport

/**
 * A [Mesh] drawn [count] times in one call, each instance with its own transform and optional color, as
 * Three.js `InstancedMesh`. Instance transforms are applied before the object's own world transform.
 * After changing matrices or colors through the setters, call [InstancedBufferAttribute.markUpdated] on
 * [instanceMatrix] or [instanceColor] so the renderer uploads them. Instanced morph targets are not
 * supported yet.
 */
@JsExport
class InstancedMesh(
    geometry: BufferGeometry,
    material: Material,
    /** Maximum number of instances, which sizes [instanceMatrix]. */
    maxCount: Int,
) : Mesh(geometry, material) {
    /** Column-major transform of each instance, 16 floats per instance. Initialized to identity. */
    val instanceMatrix = InstancedBufferAttribute(FloatArray(maxCount * 16), 16)

    /**
     * Color of each instance, created by the first [setColorAt] with white for the other instances, or
     * null when no color has been set. The colors multiply the material color.
     */
    var instanceColor: InstancedBufferAttribute? = null
        private set

    /** Number of instances drawn. Can be lowered below the initial count without reallocating. */
    var count = maxCount

    /** Box around all instances in local space, computed by [computeBoundingBox], or null before. */
    var boundingBox: Box3? = null
        private set

    /** Sphere around all instances in local space, computed by [computeBoundingSphere], or null before. */
    var boundingSphere: Sphere? = null
        private set

    private val instanceLocalMatrix = Matrix4()

    init {
        for (i in 0 until maxCount) setMatrixAt(i, instanceLocalMatrix)
    }

    /** Computes [boundingBox] from the geometry's box transformed by every instance matrix. */
    fun computeBoundingBox() {
        val box = boundingBox ?: Box3().also { boundingBox = it }
        if (geometry.boundingBox == null) geometry.computeBoundingBox()
        val geometryBox = geometry.boundingBox!!
        val instanceBox = Box3()
        box.makeEmpty()
        for (i in 0 until count) {
            getMatrixAt(i, instanceLocalMatrix)
            instanceBox.copy(geometryBox).applyMatrix4(instanceLocalMatrix)
            box.union(instanceBox)
        }
    }

    /** Computes [boundingSphere] from the geometry's sphere transformed by every instance matrix. */
    fun computeBoundingSphere() {
        val sphere = boundingSphere ?: Sphere().also { boundingSphere = it }
        if (geometry.boundingSphere == null) geometry.computeBoundingSphere()
        val geometrySphere = geometry.boundingSphere!!
        val instanceSphere = Sphere()
        sphere.makeEmpty()
        for (i in 0 until count) {
            getMatrixAt(i, instanceLocalMatrix)
            instanceSphere.copy(geometrySphere).applyMatrix4(instanceLocalMatrix)
            sphere.union(instanceSphere)
        }
    }

    /** Copies the color of instance [index] into [color] and returns it. Requires [instanceColor]. */
    fun getColorAt(index: Int, color: Color): Color {
        val colors = requireNotNull(instanceColor) { "No instance colors have been set." }.array
        return color.set(colors[index * 3].toDouble(), colors[index * 3 + 1].toDouble(), colors[index * 3 + 2].toDouble())
    }

    /** Copies the transform of instance [index] into [matrix] and returns it. */
    fun getMatrixAt(index: Int, matrix: Matrix4): Matrix4 {
        val elements = matrix.elements
        val array = instanceMatrix.array
        for (i in 0 until 16) elements[i] = array[index * 16 + i].toDouble()
        return matrix
    }

    /** Sets the color of instance [index], creating [instanceColor] on first use. */
    fun setColorAt(index: Int, color: Color) {
        val colors = instanceColor ?: InstancedBufferAttribute(FloatArray(instanceMatrix.count * 3) { 1f }, 3)
            .also { instanceColor = it }
        colors.array[index * 3] = color.r.toFloat()
        colors.array[index * 3 + 1] = color.g.toFloat()
        colors.array[index * 3 + 2] = color.b.toFloat()
    }

    /** Sets the transform of instance [index]. */
    fun setMatrixAt(index: Int, matrix: Matrix4) {
        val elements = matrix.elements
        val array = instanceMatrix.array
        for (i in 0 until 16) array[index * 16 + i] = elements[i].toFloat()
    }
}
