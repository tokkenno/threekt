package com.aitorgf.threekt.core

import com.aitorgf.threekt.math.Box3
import com.aitorgf.threekt.math.MathUtils
import com.aitorgf.threekt.math.Matrix3
import com.aitorgf.threekt.math.Matrix4
import com.aitorgf.threekt.math.MutableVector
import com.aitorgf.threekt.math.Quaternion
import com.aitorgf.threekt.math.Sphere
import com.aitorgf.threekt.math.Vector2
import com.aitorgf.threekt.math.Vector3
import kotlin.js.JsExport
import com.aitorgf.threekt.utils.arrayNeedsUint32
import kotlin.js.JsName
import kotlin.jvm.JvmName
import kotlin.math.sqrt

/**
 * Stores named vertex attributes and optional indices. When [index] is null, vertices are consumed in
 * attribute order.
 *
 * Deviations from Three.js:
 * - The `attributes` object is replaced by [getAttribute], [setAttribute], [deleteAttribute],
 *   [hasAttribute] and [attributeNames], and `groups` is a read-only snapshot changed through [addGroup].
 * - A `drawRange.count` of [Int.MAX_VALUE] stands for `Infinity`.
 * - [clone] returns a plain [BufferGeometry]; Three.js creates an instance of the subclass, with its
 *   default parameters, and copies the data into it.
 * - `toJSON()` and the WebGPU-only `indirect` draws are not available yet.
 */
@JsExport
open class BufferGeometry : EventDispatcher() {
    /** Unique number of this geometry, assigned in creation order. */
    val id: Int = nextId++

    /** Random UUID of this geometry. */
    var uuid: String = MathUtils.generateUUID()

    /** Optional name used to identify the geometry. */
    var name = ""

    /** Custom data attached to the geometry. [copy] shares the map's values, as Three.js shares the object. */
    val userData: MutableMap<String, Any?> = linkedMapOf()

    private val attributes = linkedMapOf<String, BufferAttribute>()

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

    /**
     * Range of indices, or of vertices for non-indexed geometry, drawn with the material at
     * [materialIndex] when a mesh has several materials.
     */
    class Group(var start: Int, var count: Int, var materialIndex: Int = 0)

    private val drawGroups = mutableListOf<Group>()

    /**
     * Draw groups for multi-material meshes, a snapshot in insertion order. Meshes with a single material
     * ignore them.
     */
    val groups: Array<Group> get() = drawGroups.toTypedArray()

    /** Adds a draw group of [count] elements from [start] that uses material [materialIndex]. */
    fun addGroup(start: Int, count: Int, materialIndex: Int = 0) {
        drawGroups.add(Group(start, count, materialIndex))
    }

    /** Removes all draw groups. */
    fun clearGroups() {
        drawGroups.clear()
    }

    /**
     * Indices of the vertices to draw, with one component per item, or null for sequential vertices. On the
     * JVM its getter is named `index()` to avoid clashing with [getIndex].
     */
    @get:JvmName("index")
    var index: BufferAttribute? = null
        private set

    /** Returns [index]. */
    fun getIndex(): BufferAttribute? = index

    /**
     * Sets the vertex indices, an attribute with one component per item such as a [Uint16BufferAttribute],
     * or clears indexed drawing when [index] is null. Retains the attribute and returns this geometry.
     */
    fun setIndex(index: BufferAttribute?): BufferGeometry {
        this.index = index
        return this
    }

    /**
     * Sets the vertex indices from [indices], stored in a [Uint32BufferAttribute] when a value is 65535
     * or more and in a [Uint16BufferAttribute] otherwise, as Three.js does for plain arrays. The values are
     * copied. Three.js accepts the array in `setIndex()`; from JavaScript call `setIndexArray()`. Returns
     * this geometry.
     */
    @JsName("setIndexArray")
    fun setIndex(indices: IntArray): BufferGeometry = setIndex(
        if (arrayNeedsUint32(indices)) Uint32BufferAttribute(indices.copyOf(), 1)
        else Uint16BufferAttribute(ShortArray(indices.size) { indices[it].toShort() }, 1),
    )

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

    /**
     * Bounding box computed by [computeBoundingBox], or null before the first computation. Assign it
     * directly when the positions are not available on the CPU.
     */
    var boundingBox: Box3? = null

    /**
     * Bounding sphere computed by [computeBoundingSphere], or null before the first computation. Frustum
     * culling and raycasting use it. Assign it directly when the positions are not available on the CPU.
     */
    var boundingSphere: Sphere? = null

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
        val position = attributes["position"]
        if (position == null) return
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
     * Sets the `position` attribute from [points], using zero for the Z of 2D points. Without a position
     * attribute, a new one is created; otherwise the existing one is updated in place, ignoring points that
     * do not fit, and marked for upload. Returns this geometry.
     */
    fun setFromPoints(points: List<MutableVector<*>>): BufferGeometry {
        val position = attributes["position"]
        if (position == null) {
            val positions = FloatArray(points.size * 3)
            for ((i, point) in points.withIndex()) {
                positions[i * 3] = point[0].toFloat()
                positions[i * 3 + 1] = point[1].toFloat()
                positions[i * 3 + 2] = if (point.dimension > 2) point[2].toFloat() else 0f
            }
            setAttribute("position", Float32BufferAttribute(positions, 3))
        } else {
            for (i in 0 until minOf(points.size, position.count)) {
                val point = points[i]
                position.setXYZ(i, point[0], point[1], if (point.dimension > 2) point[2] else 0.0)
            }
            position.markUpdated()
        }
        return this
    }

    /**
     * Computes a `tangent` attribute with four components, whose W is the handedness of the tangent frame,
     * from the indices, positions, normals and UVs, as Three.js does for normal mapping. Requires all four;
     * throws [IllegalStateException] otherwise, where Three.js logs an error. Replaces the attribute when
     * its size does not match.
     */
    fun computeTangents() {
        val index = index
        val positionAttribute = attributes["position"]
        val normalAttribute = attributes["normal"]
        val uvAttribute = attributes["uv"]
        check(index != null && positionAttribute != null && normalAttribute != null && uvAttribute != null) {
            "BufferGeometry.computeTangents() failed. Missing required attributes (index, position, normal or uv)."
        }
        var tangentAttribute = attributes["tangent"]
        if (tangentAttribute == null || tangentAttribute.count != positionAttribute.count) {
            tangentAttribute = Float32BufferAttribute(FloatArray(4 * positionAttribute.count), 4)
            setAttribute("tangent", tangentAttribute)
        }
        val tan1 = Array(positionAttribute.count) { Vector3() }
        val tan2 = Array(positionAttribute.count) { Vector3() }
        val used = BooleanArray(positionAttribute.count)
        val vA = Vector3()
        val vB = Vector3()
        val vC = Vector3()
        val uvA = Vector2()
        val uvB = Vector2()
        val uvC = Vector2()
        val sdir = Vector3()
        val tdir = Vector3()

        fun handleTriangle(a: Int, b: Int, c: Int) {
            used[a] = true
            used[b] = true
            used[c] = true
            vA.fromBufferAttribute(positionAttribute, a)
            vB.fromBufferAttribute(positionAttribute, b)
            vC.fromBufferAttribute(positionAttribute, c)
            uvA.fromBufferAttribute(uvAttribute, a)
            uvB.fromBufferAttribute(uvAttribute, b)
            uvC.fromBufferAttribute(uvAttribute, c)
            vB.sub(vA)
            vC.sub(vA)
            uvB.sub(uvA)
            uvC.sub(uvA)
            val r = 1.0 / (uvB.x * uvC.y - uvC.x * uvB.y)
            if (!r.isFinite()) return
            sdir.copy(vB).multiplyScalar(uvC.y).addScaledVector(vC, -uvB.y).multiplyScalar(r)
            tdir.copy(vC).multiplyScalar(uvB.x).addScaledVector(vB, -uvC.x).multiplyScalar(r)
            tan1[a].add(sdir)
            tan1[b].add(sdir)
            tan1[c].add(sdir)
            tan2[a].add(tdir)
            tan2[b].add(tdir)
            tan2[c].add(tdir)
        }

        val ranges = if (drawGroups.isEmpty()) listOf(Group(0, index.count)) else drawGroups
        for (group in ranges) {
            for (j in group.start until group.start + group.count step 3) {
                handleTriangle(index.getX(j).toInt(), index.getX(j + 1).toInt(), index.getX(j + 2).toInt())
            }
        }

        val tmp = Vector3()
        val tmp2 = Vector3()
        val n = Vector3()
        val n2 = Vector3()
        for (v in 0 until positionAttribute.count) {
            if (!used[v]) continue
            n.fromBufferAttribute(normalAttribute, v)
            n2.copy(n)
            val t = tan1[v]
            tmp.copy(t)
            tmp.sub(n.multiplyScalar(n.dot(t))).normalize()
            tmp2.crossVectors(n2, t)
            val w = if (tmp2.dot(tan2[v]) < 0.0) -1.0 else 1.0
            tangentAttribute.setXYZW(v, tmp.x, tmp.y, tmp.z, w)
        }
    }

    /**
     * Computes smooth vertex normals by summing the face normals of the triangles that share each vertex,
     * weighted by their area. Non-indexed geometry gets flat normals. Replaces the `normal` attribute when
     * its size does not match the positions, and marks it for upload.
     */
    fun computeVertexNormals() {
        val positionAttribute = attributes["position"] ?: return
        var normalAttribute = attributes["normal"]
        if (normalAttribute == null || normalAttribute.count != positionAttribute.count) {
            normalAttribute = Float32BufferAttribute(FloatArray(positionAttribute.count * 3), 3)
            setAttribute("normal", normalAttribute)
        } else {
            for (i in 0 until normalAttribute.count) normalAttribute.setXYZ(i, 0.0, 0.0, 0.0)
        }
        val pA = Vector3()
        val pB = Vector3()
        val pC = Vector3()
        val nA = Vector3()
        val nB = Vector3()
        val nC = Vector3()
        val cb = Vector3()
        val ab = Vector3()
        val index = index
        if (index != null) {
            for (i in 0 until index.count - 2 step 3) {
                val vA = index.getX(i).toInt()
                val vB = index.getX(i + 1).toInt()
                val vC = index.getX(i + 2).toInt()
                pA.fromBufferAttribute(positionAttribute, vA)
                pB.fromBufferAttribute(positionAttribute, vB)
                pC.fromBufferAttribute(positionAttribute, vC)
                cb.subVectors(pC, pB)
                ab.subVectors(pA, pB)
                cb.cross(ab)
                nA.fromBufferAttribute(normalAttribute, vA).add(cb)
                nB.fromBufferAttribute(normalAttribute, vB).add(cb)
                nC.fromBufferAttribute(normalAttribute, vC).add(cb)
                normalAttribute.setXYZ(vA, nA.x, nA.y, nA.z)
                normalAttribute.setXYZ(vB, nB.x, nB.y, nB.z)
                normalAttribute.setXYZ(vC, nC.x, nC.y, nC.z)
            }
        } else {
            for (i in 0 until positionAttribute.count - 2 step 3) {
                pA.fromBufferAttribute(positionAttribute, i)
                pB.fromBufferAttribute(positionAttribute, i + 1)
                pC.fromBufferAttribute(positionAttribute, i + 2)
                cb.subVectors(pC, pB)
                ab.subVectors(pA, pB)
                cb.cross(ab)
                normalAttribute.setXYZ(i, cb.x, cb.y, cb.z)
                normalAttribute.setXYZ(i + 1, cb.x, cb.y, cb.z)
                normalAttribute.setXYZ(i + 2, cb.x, cb.y, cb.z)
            }
        }
        normalizeNormals()
        normalAttribute.markUpdated()
    }

    /** Scales every normal to unit length. Has no effect without a `normal` attribute. */
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

    /** Rotates the geometry by [q]. Returns this geometry. */
    fun applyQuaternion(q: Quaternion): BufferGeometry = applyMatrix4(m1.makeRotationFromQuaternion(q))

    /** Rotates the geometry about the X axis by [angle] radians. Returns this geometry. */
    fun rotateX(angle: Double): BufferGeometry = applyMatrix4(m1.makeRotationX(angle))

    /** Rotates the geometry about the Y axis by [angle] radians. Returns this geometry. */
    fun rotateY(angle: Double): BufferGeometry = applyMatrix4(m1.makeRotationY(angle))

    /** Rotates the geometry about the Z axis by [angle] radians. Returns this geometry. */
    fun rotateZ(angle: Double): BufferGeometry = applyMatrix4(m1.makeRotationZ(angle))

    /** Moves the geometry by ([x], [y], [z]). Returns this geometry. */
    fun translate(x: Double, y: Double, z: Double): BufferGeometry = applyMatrix4(m1.makeTranslation(x, y, z))

    /** Scales the geometry by ([x], [y], [z]). Returns this geometry. */
    fun scale(x: Double, y: Double, z: Double): BufferGeometry = applyMatrix4(m1.makeScale(x, y, z))

    /** Rotates the geometry so that its positive Z axis points toward [vector]. Returns this geometry. */
    fun lookAt(vector: Vector3): BufferGeometry {
        val obj = Object3D()
        obj.lookAt(vector)
        obj.updateMatrix()
        return applyMatrix4(obj.matrix)
    }

    /** Moves the geometry so that the center of its bounding box is at the origin. Returns this geometry. */
    fun center(): BufferGeometry {
        computeBoundingBox()
        val offset = boundingBox!!.getCenter(Vector3()).negate()
        return translate(offset.x, offset.y, offset.z)
    }

    /**
     * Returns a non-indexed copy in which every index is replaced by a copy of the vertex it refers to,
     * keeping the morph attributes and groups. Attributes keep their array type, and interleaved attributes
     * become separate ones. Returns this geometry unchanged when it is not indexed; Three.js also logs a
     * warning.
     */
    fun toNonIndexed(): BufferGeometry {
        val index = index ?: return this
        val indices = IntArray(index.count) { index.getX(it).toInt() }
        val geometry = BufferGeometry()
        for ((name, attribute) in attributes) geometry.setAttribute(name, convertBufferAttribute(attribute, indices))
        for ((name, targets) in morphAttributes) {
            geometry.morphAttributes[name] = targets.map { convertBufferAttribute(it, indices) }
        }
        geometry.morphTargetsRelative = morphTargetsRelative
        for (group in drawGroups) geometry.addGroup(group.start, group.count, group.materialIndex)
        return geometry
    }

    private fun convertBufferAttribute(attribute: BufferAttribute, indices: IntArray): BufferAttribute {
        val itemSize = attribute.itemSize
        val storage = (attribute as? InterleavedBufferAttribute)?.data?.storage ?: attribute
        val result = storage.createEmpty(indices.size * itemSize, itemSize, attribute.normalized)
        var index2 = 0
        for (i in indices) {
            var index = if (attribute is InterleavedBufferAttribute) i * attribute.data.stride + attribute.offset else i * itemSize
            for (j in 0 until itemSize) result.writeRaw(index2++, storage.readRaw(index++))
        }
        return result
    }

    /**
     * Returns an independent copy with cloned index, attributes and morph attributes, and copies of the
     * groups, bounds and draw range. The copy is a plain [BufferGeometry], whereas Three.js preserves the
     * subclass.
     */
    open fun clone(): BufferGeometry = BufferGeometry().copy(this)

    /**
     * Replaces the contents of this geometry with clones of the index, attributes and morph attributes of
     * [source], and copies of its name, groups, bounds, draw range and user data. Interleaved attributes
     * become separate float attributes. Returns this geometry.
     */
    open fun copy(source: BufferGeometry): BufferGeometry {
        index = null
        attributes.clear()
        morphAttributes.clear()
        drawGroups.clear()
        boundingBox = null
        boundingSphere = null
        name = source.name
        source.index?.let { setIndex(it.clone()) }
        for ((name, attribute) in source.attributes) setAttribute(name, attribute.clone())
        for ((name, targets) in source.morphAttributes) morphAttributes[name] = targets.map { it.clone() }
        morphTargetsRelative = source.morphTargetsRelative
        for (group in source.drawGroups) addGroup(group.start, group.count, group.materialIndex)
        source.boundingBox?.let { boundingBox = it.copy() }
        source.boundingSphere?.let { boundingSphere = it.copy() }
        setDrawRange(source.drawRange.start, source.drawRange.count)
        userData.clear()
        userData.putAll(source.userData)
        return this
    }

    /** Dispatches a `dispose` event so that renderers release the GPU resources of this geometry. */
    fun dispose() {
        dispatchEvent(DispatchedEvent(DISPOSE))
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

    private val m1 = Matrix4()

    companion object {
        private var nextId = 0

        /** Type of the event dispatched by [dispose]. */
        const val DISPOSE = "dispose"
    }
}
