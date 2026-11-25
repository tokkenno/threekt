package com.aitorgf.threekt.core

import com.aitorgf.threekt.math.Box3
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
