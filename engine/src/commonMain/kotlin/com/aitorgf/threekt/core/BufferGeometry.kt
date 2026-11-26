package com.aitorgf.threekt.core

import com.aitorgf.threekt.math.Box3
import com.aitorgf.threekt.math.Matrix3
import com.aitorgf.threekt.math.Matrix4
import com.aitorgf.threekt.math.MutableVector
import com.aitorgf.threekt.math.Sphere
import com.aitorgf.threekt.math.Vector3
import kotlin.js.JsExport
import kotlin.math.sqrt

/**
 * Stores named vertex attributes and optional indices. When [index] is null, vertices are consumed in
 * attribute order.
 */
@JsExport
open class BufferGeometry {
    private val attributes = mutableMapOf<String, BufferAttribute>()

    /**
     * Range of vertices, or of indices when [index] is set, to draw. Change it with [setDrawRange]. The
     * count defaults to [Int.MAX_VALUE], which stands for the Three.js `Infinity` and draws everything.
     */
    val drawRange = DrawRange()

    /** First element and number of elements to draw. */
    class DrawRange(var start: Int = 0, var count: Int = Int.MAX_VALUE)

    /**
     * Draws [count] vertices, or indices for indexed geometry, starting at [start]. Without attributes or
     * indices, the range alone sets how many vertices the shader generates. Returns nothing, as in
     * Three.js.
     */
    fun setDrawRange(start: Int, count: Int) {
        drawRange.start = start
        drawRange.count = count
    }

    /** Indices of the vertices to draw, or null for sequential vertices. */
    var index: IntArray? = null
        private set

    /**
     * Sets the vertex indices or clears indexed drawing when [indices] is null. Retains the supplied array
     * and returns this geometry.
     */
    fun setIndex(indices: IntArray?): BufferGeometry {
        index = indices
        return this
    }

    /**
     * Associates [attribute] with [name], replacing an existing attribute with that name. Returns this
     * geometry.
     */
    fun setAttribute(name: String, attribute: BufferAttribute): BufferGeometry {
        attributes[name] = attribute
        return this
    }

    /** Returns the attribute registered under [name], or null if no attribute is registered. */
    fun getAttribute(name: String): BufferAttribute? = attributes[name]

    /** Removes the attribute registered under [name], if any. Returns this geometry. */
    fun deleteAttribute(name: String): BufferGeometry {
        attributes.remove(name)
        return this
    }

    /**
     * Names of the registered attributes in insertion order, a snapshot. Three.js exposes the attributes
     * as an `attributes` object; use [getAttribute] to read each one.
     */
    val attributeNames: Array<String> get() = attributes.keys.toTypedArray()

    /** Returns whether an attribute is registered under [name]. */
    fun hasAttribute(name: String): Boolean = name in attributes

    /**
     * Morph targets by attribute name. Each list holds one attribute per target, with the same item count as
     * the base attribute. Three.js uses a plain object, as in `morphAttributes.position = [target]`; from
     * JavaScript, use [setMorphAttribute] and [getMorphAttribute].
     */
    @JsExport.Ignore
    val morphAttributes: MutableMap<String, List<BufferAttribute>> = linkedMapOf()

    /**
     * Whether morph targets hold offsets that are added to the base attribute (true) or absolute values that
     * the base attribute blends toward (false).
     */
    var morphTargetsRelative = false

    /** Sets the morph targets for the attribute [name], replacing any previous ones. Returns this geometry. */
    fun setMorphAttribute(name: String, targets: Array<BufferAttribute>): BufferGeometry {
        morphAttributes[name] = targets.toList()
        return this
    }

    /** Returns the morph targets for the attribute [name], or null when it has none. */
    fun getMorphAttribute(name: String): Array<BufferAttribute>? = morphAttributes[name]?.toTypedArray()

    /** Bounding box computed by [computeBoundingBox], or null before the first computation. */
    var boundingBox: Box3? = null
        private set

    /** Bounding sphere computed by [computeBoundingSphere], or null before the first computation. */
    var boundingSphere: Sphere? = null
        private set

    /**
     * Computes [boundingBox] from the position attribute and its morph targets. The box is empty when there
     * are no positions. Call it again after changing positions.
     */
    fun computeBoundingBox() {
        val box = boundingBox ?: Box3().also { boundingBox = it }
        val position = attributes["position"]
        if (position == null) {
            box.makeEmpty()
            return
        }
        box.setFromBufferAttribute(position)
        expandByMorphTargets(box)
    }

    /**
     * Computes [boundingSphere] from the position attribute and its morph targets. The sphere is centered on
     * their bounding box. Has no effect when there are no positions. Call it again after changing positions.
     */
    fun computeBoundingSphere() {
        val sphere = boundingSphere ?: Sphere().also { boundingSphere = it }
        val position = attributes["position"] ?: return
        val box = Box3().setFromBufferAttribute(position)
        expandByMorphTargets(box)
        val center = box.getCenter(sphere.center)
        val vector = Vector3()
        val offset = Vector3()
        var maxRadiusSq = 0.0
        for (index in 0 until position.count) {
            maxRadiusSq = maxOf(maxRadiusSq, center.distanceToSquared(vector.fromBufferAttribute(position, index)))
        }
        for (morphAttribute in morphAttributes["position"].orEmpty()) {
            for (index in 0 until morphAttribute.count) {
                vector.fromBufferAttribute(morphAttribute, index)
                if (morphTargetsRelative) vector.add(offset.fromBufferAttribute(position, index))
                maxRadiusSq = maxOf(maxRadiusSq, center.distanceToSquared(vector))
            }
        }
        sphere.radius = sqrt(maxRadiusSq)
    }

    /**
     * Sets the `position` attribute from [points], using zero for the Z of 2D points, as Three.js
     * `setFromPoints`. Replaces an existing position attribute. Returns this geometry.
     */
    fun setFromPoints(points: List<MutableVector<*>>): BufferGeometry {
        val positions = FloatArray(points.size * 3)
        for ((i, point) in points.withIndex()) {
            positions[i * 3] = point[0].toFloat()
            positions[i * 3 + 1] = point[1].toFloat()
            positions[i * 3 + 2] = if (point.dimension > 2) point[2].toFloat() else 0f
        }
        setAttribute("position", Float32BufferAttribute(positions, 3))
        return this
    }

    /**
     * Computes smooth vertex normals by summing the face normals of the triangles that share each vertex,
     * weighted by their area, as Three.js does. Non-indexed geometry gets flat normals. Replaces the
     * `normal` attribute when its size does not match the positions, and marks it for upload.
     */
    fun computeVertexNormals() {
        val position = attributes["position"] ?: return
        var normal = attributes["normal"]
        if (normal == null || normal.count != position.count) {
            normal = Float32BufferAttribute(FloatArray(position.count * 3), 3)
            setAttribute("normal", normal)
        } else {
            for (i in 0 until normal.count) normal.setXYZ(i, 0.0, 0.0, 0.0)
        }
        val pA = Vector3()
        val pB = Vector3()
        val pC = Vector3()
        val nA = Vector3()
        val nB = Vector3()
        val nC = Vector3()
        val cb = Vector3()
        val ab = Vector3()
        val indices = index
        if (indices != null) {
            for (i in 0 until indices.size - 2 step 3) {
                val vA = indices[i]
                val vB = indices[i + 1]
                val vC = indices[i + 2]
                pA.fromBufferAttribute(position, vA)
                pB.fromBufferAttribute(position, vB)
                pC.fromBufferAttribute(position, vC)
                cb.subVectors(pC, pB).cross(ab.subVectors(pA, pB))
                nA.fromBufferAttribute(normal, vA).add(cb)
                nB.fromBufferAttribute(normal, vB).add(cb)
                nC.fromBufferAttribute(normal, vC).add(cb)
                normal.setXYZ(vA, nA.x, nA.y, nA.z)
                normal.setXYZ(vB, nB.x, nB.y, nB.z)
                normal.setXYZ(vC, nC.x, nC.y, nC.z)
            }
        } else {
            for (i in 0 until position.count - 2 step 3) {
                pA.fromBufferAttribute(position, i)
                pB.fromBufferAttribute(position, i + 1)
                pC.fromBufferAttribute(position, i + 2)
                cb.subVectors(pC, pB).cross(ab.subVectors(pA, pB))
                for (vertex in i..i + 2) normal.setXYZ(vertex, cb.x, cb.y, cb.z)
            }
        }
        normalizeNormals()
        normal.markUpdated()
    }

    /** Scales every normal to unit length. */
    fun normalizeNormals() {
        val normals = attributes["normal"] ?: return
        val vector = Vector3()
        for (i in 0 until normals.count) {
            vector.fromBufferAttribute(normals, i).normalize()
            normals.setXYZ(i, vector.x, vector.y, vector.z)
        }
    }

    /**
     * Transforms the positions by [matrix], the normals by its normal matrix and the tangents as
     * directions, marks them for upload, and recomputes existing bounding volumes. Returns this geometry.
     */
    fun applyMatrix4(matrix: Matrix4): BufferGeometry {
        attributes["position"]?.let {
            it.applyMatrix4(matrix)
            it.markUpdated()
        }
        attributes["normal"]?.let {
            it.applyNormalMatrix(Matrix3().getNormalMatrix(matrix))
            it.markUpdated()
        }
        attributes["tangent"]?.let {
            it.transformDirection(matrix)
            it.markUpdated()
        }
        if (boundingBox != null) computeBoundingBox()
        if (boundingSphere != null) computeBoundingSphere()
        return this
    }

    /** Rotates the geometry about the X axis by [angle] radians. Returns this geometry. */
    fun rotateX(angle: Double): BufferGeometry = applyMatrix4(Matrix4().makeRotationX(angle))

    /** Rotates the geometry about the Y axis by [angle] radians. Returns this geometry. */
    fun rotateY(angle: Double): BufferGeometry = applyMatrix4(Matrix4().makeRotationY(angle))

    /** Rotates the geometry about the Z axis by [angle] radians. Returns this geometry. */
    fun rotateZ(angle: Double): BufferGeometry = applyMatrix4(Matrix4().makeRotationZ(angle))

    /** Moves the geometry by ([x], [y], [z]). Returns this geometry. */
    fun translate(x: Double, y: Double, z: Double): BufferGeometry = applyMatrix4(Matrix4().makeTranslation(x, y, z))

    /** Scales the geometry by ([x], [y], [z]). Returns this geometry. */
    fun scale(x: Double, y: Double, z: Double): BufferGeometry = applyMatrix4(Matrix4().makeScale(x, y, z))

    /** Moves the geometry so that the center of its bounding box is at the origin. Returns this geometry. */
    fun center(): BufferGeometry {
        computeBoundingBox()
        val offset = boundingBox!!.getCenter(Vector3()).negate()
        return translate(offset.x, offset.y, offset.z)
    }

    /**
     * Returns an independent copy with cloned attributes, morph attributes, indices and draw range.
     * Interleaved attributes become separate float attributes. Subclass parameters are not kept: the copy
     * is a plain [BufferGeometry], whereas Three.js preserves the subclass.
     */
    open fun clone(): BufferGeometry {
        val copy = BufferGeometry()
        index?.let { copy.setIndex(it.copyOf()) }
        for ((name, attribute) in attributes) copy.setAttribute(name, attribute.clone())
        for ((name, targets) in morphAttributes) copy.morphAttributes[name] = targets.map { it.clone() }
        copy.morphTargetsRelative = morphTargetsRelative
        copy.setDrawRange(drawRange.start, drawRange.count)
        boundingBox?.let { copy.boundingBox = it.copy() }
        boundingSphere?.let { copy.boundingSphere = it.copy() }
        return copy
    }

    private fun expandByMorphTargets(box: Box3) {
        val targetBox = Box3()
        val vector = Vector3()
        for (morphAttribute in morphAttributes["position"].orEmpty()) {
            targetBox.setFromBufferAttribute(morphAttribute)
            if (morphTargetsRelative) {
                box.expandByPoint(vector.addVectors(box.min, targetBox.min))
                box.expandByPoint(vector.addVectors(box.max, targetBox.max))
            } else {
                box.expandByPoint(targetBox.min)
                box.expandByPoint(targetBox.max)
            }
        }
    }
}
