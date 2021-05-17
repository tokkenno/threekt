package com.aitorgf.threekt.core

import com.aitorgf.threekt.core.buffers.Float32BufferAttribute
import com.aitorgf.threekt.math.*
import com.aitorgf.threekt.types.UUID
import com.aitorgf.threekt.events.EventEmitter
import com.aitorgf.threekt.events.Observable
import com.aitorgf.threekt.utils.Logger
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport
import kotlin.js.JsName

@ExperimentalJsExport
@JsExport
open class BufferGeometry : Entity, Disposable {
    private val log = Logger(BufferGeometry::class.qualifiedName ?: "BufferGeometry")

    override val id: UUID = UUID.randomUUID()
    override var name: String = ""

    protected val onDisposeEmitter = EventEmitter()

    override val onDispose: Observable
        get() = onDisposeEmitter

    private val attributes: MutableMap<String, BufferAttribute> = HashMap()
    private val morphAttributes: MutableMap<String, BufferAttribute> = HashMap()

    var boundingBox: MutableBox3? = null
        private set

    var boundingSphere: Sphere? = null
        private set

    private var drawRangeStart: Double = 0.0
    private var drawRangeCount: Double = Double.POSITIVE_INFINITY

    private val groups: MutableList<Group> = mutableListOf()

    fun setDrawRange(start: Double, count: Double) {
        this.drawRangeStart = start
        this.drawRangeCount = count
    }

    @JsName("applyMatrix4")
    fun apply(matrix: Matrix4): BufferGeometry {
        val position = this.attributes["position"]

        if (position != null) {
            position.apply(matrix)
            position.scheduleUpdate()
        }

        val normal = this.attributes["normal"]

        if (normal != null) {
            val normalMatrix = MutableMatrix3().getNormalMatrix(matrix)
            normal.applyNormalMatrix(normalMatrix)
            normal.scheduleUpdate()
        }

        val tangent = this.attributes["tangent"]

        if (tangent != null) {
            tangent.transformDirection(matrix)
            tangent.scheduleUpdate()
        }

        if (this.boundingBox != null) {
            this.computeBoundingBox()
        }

        if (this.boundingSphere != null) {
            this.computeBoundingSphere()
        }

        return this
    }

    fun rotateX(angle: Radian): BufferGeometry {
        // rotate geometry around world x-axis
        this.apply(MutableMatrix4().makeRotationX(angle))
        return this

    }

    fun rotateY(angle: Radian): BufferGeometry {
        // rotate geometry around world y-axis
        this.apply(MutableMatrix4().makeRotationY(angle))
        return this
    }

    fun rotateZ(angle: Radian): BufferGeometry {
        // rotate geometry around world z-axis
        this.apply(MutableMatrix4().makeRotationZ(angle))
        return this
    }

    fun translate(x: Double, y: Double, z: Double): BufferGeometry {
        this.apply(MutableMatrix4().makeTranslation(x, y, z))
        return this
    }

    fun scale(x: Double, y: Double, z: Double): BufferGeometry {
        this.apply(MutableMatrix4().makeScale(x, y, z))
        return this
    }

    fun lookAt(vector: Vector3): BufferGeometry {
        val obj = Object3()
        obj.lookAt(vector)
        obj.updateMatrix()
        this.apply(obj.matrix)
        return this
    }

    fun center(): BufferGeometry {
        this.computeBoundingBox()
        val boundingBox = this.boundingBox ?: throw EngineException("Can't calculate bounding box for geometry <$id>")
        val offset = boundingBox.center.toMutable().negate()
        this.translate(offset.x, offset.y, offset.z)
        return this
    }

    fun setFromPoints(points: Array<Vector3>): BufferGeometry {
        val position: MutableList<Double> = ArrayList()

        for (point in points) {
            position.add(point.x)
            position.add(point.y)
            position.add(point.z)
        }
        this.attributes["position"] = Float32BufferAttribute(position.toDoubleArray(), 3)
        return this
    }

    fun computeBoundingBox() {
        var localBoundingBox = this.boundingBox ?: MutableBox3()

        val position = this.attributes["position"]
        val morphAttributesPosition = this.morphAttributes["position"]

        if (position != null && position.inGPU) {
            this.log.error(
                "computeBoundingBox(): GLBufferAttribute requires a manual bounding box. Alternatively set \"mesh.frustumCulled\" to \"false\".",
                this
            )
            localBoundingBox.set(
                Vector3(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY),
                Vector3(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY)
            )
            this.boundingBox = localBoundingBox
            return
        }

        if (position != null) {
            localBoundingBox.boxFromBufferAttribute(position)

            // process morph attributes if present

            if (morphAttributesPosition != null) {
                for (morphAttribute in morphAttributesPosition) {
                    val box = MutableBox3().boxFromBufferAttribute(morphAttribute)

                    if (this.morphTargetsRelative != null) {
                        var vector = MutableVector3().add(localBoundingBox.min, box.min)
                        localBoundingBox.expandByPoint(vector)

                        vector.add(localBoundingBox.max, box.max)
                        localBoundingBox.expandByPoint(vector)
                    } else {
                        localBoundingBox.expandByPoint(box.min)
                        localBoundingBox.expandByPoint(box.max)
                    }
                }
            }
        } else {
            localBoundingBox.makeEmpty()
        }

        this.boundingBox = localBoundingBox
    }

    fun computeBoundingSphere() {
        var localBoundingSphere = this.boundingSphere ?: Sphere()

        val position = this.attributes["position"]
        val morphAttributesPosition = this.morphAttributes["position"]

        if (position != null && position.inGPU) {
            this.log.error(
                "computeBoundingSphere(): GLBufferAttribute requires a manual bounding sphere. Alternatively set \"mesh.frustumCulled\" to \"false\".",
                this
            )
            localBoundingSphere.set(Vector3(), Double.POSITIVE_INFINITY)
            this.boundingSphere = localBoundingSphere
            return
        }

        if (position != null) {
            // first, find the center of the bounding sphere
            val center = localBoundingSphere.center

            val box = MutableBox3().boxFromBufferAttribute(position)

            // process morph attributes if present
            if (morphAttributesPosition != null) {
                for (morphAttribute in morphAttributesPosition) {
                    val boxMorphTargets = MutableBox3().boxFromBufferAttribute(morphAttribute)

                    if (this.morphTargetsRelative != null) {
                        val vector = MutableVector3().add(box.min, boxMorphTargets.min)
                        box.expandByPoint(vector)

                        vector.add(box.max, boxMorphTargets.max)
                        box.expandByPoint(vector)
                    } else {
                        box.expandByPoint(boxMorphTargets.min)
                        box.expandByPoint(boxMorphTargets.max)
                    }
                }
            }

            center.set(box.center)

            // second, try to find a boundingSphere with a radius smaller than the
            // boundingSphere of the boundingBox: sqrt(3) smaller in the best case
            var maxRadiusSq: Double = 0.0
            val vector = MutableVector3()

            for (i in 0..position.size-1) {
                vector.vectorFromBufferAttribute(position, i)
                maxRadiusSq = kotlin.math.max(maxRadiusSq, center.distanceToSquared(vector))
            }

            // process morph attributes if present
            if (morphAttributesPosition != null) {
                for (morphAttribute in morphAttributesPosition) {
                    val morphTargetsRelative = this.morphTargetsRelative

                    for (j in 0..morphAttribute.size-1) {
                        vector.vectorFromBufferAttribute(morphAttribute, j)
                        if (morphTargetsRelative) {
                            vector.add(MutableVector3().vectorFromBufferAttribute(position, j))
                        }
                        maxRadiusSq = kotlin.math.max(maxRadiusSq, center.distanceToSquared(vector))
                    }
                }
            }

            localBoundingSphere.radius = kotlin.math.sqrt(maxRadiusSq)
        }

        this.boundingSphere = localBoundingSphere
    }

    fun computeTangents() {
        val position = attributes["position"]
        val normal = attributes["normal"]
        val uv = attributes["uv"]

        // based on http://www.terathon.com/code/tangent.html
        // (per vertex tangents)
        if (index == null ||
            position == null ||
            normal == null ||
            uv == null
        ) {
            throw EngineException("BufferGeometry: .computeTangents() failed. Missing required attributes (index, position, normal or uv)")
        }

        val indices = index.array
        val positions = position.array
        val normals = normal.array
        val uvs = uv.array

        val nVertices = positions.size / 3

        var tangent = attributes["tangent"]
        if (tangent == null) {
            tangent = BufferAttribute(Array<Double>(4 * nVertices) { 0.0 }, 4)
            attributes["tangent"] = tangent
        }

        val tangents = tangent.array

        val tan1: Array<MutableVector3> = Array(nVertices) { MutableVector3() }
        val tan2: Array<MutableVector3> = Array(nVertices) { MutableVector3() }

        val vA = MutableVector3 ()
        val vB = MutableVector3 ()
        val vC = MutableVector3 ()

        val uvA = MutableVector2()
        val uvB = MutableVector2()
        val uvC = MutableVector2()

        val sdir = MutableVector3 ()
        val tdir = MutableVector3 ()

        if (this.groups.size == 0) {
            this.groups.set(Group(0, indices.length))
        }

        this.groups.forEach {
            for (j in it.start..(it.start + it.count -1) step 3) {
                handvarriangle(
                    indices[j + 0],
                    indices[j + 1],
                    indices[j + 2]
                )
            }
        }

        val tmp = MutableVector3 ()
        val tmp2 = MutableVector3()
        val n = MutableVector3 ()
        val n2 = MutableVector3()

        function handleVertex (v) {
            n.fromArray(normals, v * 3)
            n2.copy(n)

            val t = tan1[v]

            // Gram-Schmidt orthogonalize

            tmp.copy(t)
            tmp.sub(n.multiplyScalar(n.dot(t))).normalize()

            // Calculate handedness

            tmp2.crossVectors(n2, t)
            val test = tmp2.dot(tan2[v])
            val w = (test < 0.0) ?-1.0 : 1.0

            tangents[v * 4] = tmp.x
            tangents[v * 4 + 1] = tmp.y
            tangents[v * 4 + 2] = tmp.z
            tangents[v * 4 + 3] = w

        }

        this.groups.forEach {
            for (j in it.start..(it.start + it.count -1) step 3) {
                handleVertex(indices[j + 0])
                handleVertex(indices[j + 1])
                handleVertex(indices[j + 2])
            }
        }
    }

    fun computeVertexNormals() {
        val index = this.index
        val positionAttribute = this.getAttribute('position')

        if (positionAttribute != undefined) {

            var normalAttribute = this.getAttribute('normal')

            if (normalAttribute === undefined) {
                normalAttribute = new BufferAttribute (new Float32Array (positionAttribute.count * 3), 3)
                this.setAttribute('normal', normalAttribute)
            } else {
                // reset existing normals to zero
                for (var i = 0, il = normalAttribute.count i < il i++) {
                    normalAttribute.setXYZ(i, 0, 0, 0)
                }
            }

            val pA = Vector3 (), pB = new Vector3(), pC = new Vector3()
            val nA = Vector3 (), nB = new Vector3(), nC = new Vector3()
            val cb = Vector3 (), ab = new Vector3()

            // indexed elements

            if (index) {

                for (var i = 0, il = index.count i < il i += 3) {

                    val vA = index.getX(i + 0)
                    val vB = index.getX(i + 1)
                    val vC = index.getX(i + 2)

                    pA.fromBufferAttribute(positionAttribute, vA)
                    pB.fromBufferAttribute(positionAttribute, vB)
                    pC.fromBufferAttribute(positionAttribute, vC)

                    cb.subVectors(pC, pB)
                    ab.subVectors(pA, pB)
                    cb.cross(ab)

                    nA.fromBufferAttribute(normalAttribute, vA)
                    nB.fromBufferAttribute(normalAttribute, vB)
                    nC.fromBufferAttribute(normalAttribute, vC)

                    nA.add(cb)
                    nB.add(cb)
                    nC.add(cb)

                    normalAttribute.setXYZ(vA, nA.x, nA.y, nA.z)
                    normalAttribute.setXYZ(vB, nB.x, nB.y, nB.z)
                    normalAttribute.setXYZ(vC, nC.x, nC.y, nC.z)

                }

            } else {

                // non-indexed elements (unconnected triangle soup)

                for (var i = 0, il = positionAttribute.count i < il i += 3) {

                    pA.fromBufferAttribute(positionAttribute, i + 0)
                    pB.fromBufferAttribute(positionAttribute, i + 1)
                    pC.fromBufferAttribute(positionAttribute, i + 2)

                    cb.subVectors(pC, pB)
                    ab.subVectors(pA, pB)
                    cb.cross(ab)

                    normalAttribute.setXYZ(i + 0, cb.x, cb.y, cb.z)
                    normalAttribute.setXYZ(i + 1, cb.x, cb.y, cb.z)
                    normalAttribute.setXYZ(i + 2, cb.x, cb.y, cb.z)

                }

            }

            this.normalizeNormals()

            normalAttribute.needsUpdate = true

        }

    }

    fun merge(geometry: BufferGeometry, offset: Double = 0.0 ): BufferGeometry {
        val attributes = this.attributes

        for (attribute1 in attributes) {
            val attributeArray1 = attribute1.array

            val attribute2 = geometry.attributes[key]
            val attributeArray2 = attribute2.array

            val attributeOffset = attribute2.itemSize * offset
            val length = kotlin.math.min(attributeArray2.length, attributeArray1.length - attributeOffset)

            for (i in 0..length-1) {
                val j = i + attributeOffset
                attributeArray1[j] = attributeArray2[i]
            }
        }

        return this
    }

    fun normalizeNormals() {
        val normals = this.attributes.normal

        val vector = MutableVector3()
        for (i in 0..normals.size - 1) {
           vector.vectorFromBufferAttribute(normals, i)
            vector.normalize()
            normals.setXYZ(i, vector.x, vector.y, vector.z)
        }
    }

    fun toNonIndexed()
    {

        function convertBufferAttribute (attribute, indices) {

        val array = attribute.array
        val itemSize = attribute.itemSize
        val normalized = attribute.normalized

        val array2 = new array . valructor (indices.length * itemSize)

        var index = 0, index2 = 0

        for (var i = 0, l = indices.length i < l i++) {

        index = indices[i] * itemSize

        for (var j = 0 j < itemSize j++ ) {

        array2[index2++] = array[index++]

    }

    }

        return new BufferAttribute (array2, itemSize, normalized)

    }

        //

        if (this.index === null) {

            console.warn('THREE.BufferGeometry.toNonIndexed(): BufferGeometry is already non-indexed.')
            return this

        }

        val geometry2 = new BufferGeometry ()

        val indices = this.index.array
        val attributes = this.attributes

        // attributes

        for (val name in attributes) {

            val attribute = attributes[name]

            val newAttribute = convertBufferAttribute(attribute, indices)

            geometry2.setAttribute(name, newAttribute)

        }

        // morph attributes

        val morphAttributes = this.morphAttributes

        for (val name in morphAttributes) {

            val morphArray = []
            val morphAttribute = morphAttributes[name] // morphAttribute: array of Float32BufferAttributes

            for (var i = 0, il = morphAttribute.length i < il i++) {

                val attribute = morphAttribute[i]

                val newAttribute = convertBufferAttribute(attribute, indices)

                morphArray.push(newAttribute)

            }

            geometry2.morphAttributes[name] = morphArray

        }

        geometry2.morphTargetsRelative = this.morphTargetsRelative

        // groups

        val groups = this.groups

        for (var i = 0, l = groups.length i < l i++) {

            val group = groups[i]
            geometry2.addGroup(group.start, group.count, group.materialIndex)

        }

        return geometry2

    }

    fun clone(): BufferGeometry {
        return BufferGeometry().copy(this)
    }

    fun copy(source: BufferGeometry): BufferGeometry {
        // reset
        this.index = null
        this.attributes.clear()
        this.morphAttributes.clear()
        this.groups.clear()
        this.boundingBox = null
        this.boundingSphere = null

        // used for storing cloned, shared data
        val data = {}

        // name
        this.name = source.name

        // index
        val index = source.index
        if (index != null) {
            this.setIndex(index.clone(data))
        }

        // attributes
        val attributes = source.attributes

        for (name in attributes) {
            val attribute = attributes[name]
            this.setAttribute(name, attribute.clone(data))
        }

        // morph attributes
        for (morphAttribute in source.morphAttributes) {
            val array = [] // morphAttribute: array of Float32BufferAttributes

            for (var i = 0, l = morphAttribute.length i < l i++) {
                array.push(morphAttribute[i].clone(data))
            }

            this.morphAttributes[name] = array
        }

        this.morphTargetsRelative = source.morphTargetsRelative

        // groups
        for (group in source.groups) {
            this.groups.add(Group(group.start, group.count, group.materialIndex))
        }

        // bounding box
        val boundingBox = source.boundingBox
        if (boundingBox != null) {
            this.boundingBox = boundingBox.clone()
        }

        // bounding sphere
        val boundingSphere = source.boundingSphere
        if (boundingSphere != null) {
            this.boundingSphere = boundingSphere.clone()
        }

        // draw range
        this.drawRange.start = source.drawRange.start
        this.drawRange.count = source.drawRange.count

        // user data
        this.userData = source.userData

        return this
    }

    fun dispose() {
        this.onDisposeEmitter.emit()
    }

    data class Group(
        var start: Int,
        var count: Int,
        var materialIndex: Int = 0
    )

    class ComputeTangentsHelper {
        val vA = MutableVector3()
        val vB = MutableVector3()
        val vC = MutableVector3()

        val uvA = MutableVector2()
        val uvB = MutableVector2()
        val uvC = MutableVector2()

        val sdir = MutableVector3()
        val tdir = MutableVector3()

        fun handleTriangle(a: Double, b: Double, c: Double) {
            vA.fromArray( positions, a * 3 );
            vB.fromArray( positions, b * 3 );
            vC.fromArray( positions, c * 3 );

            uvA.fromArray( uvs, a * 2 );
            uvB.fromArray( uvs, b * 2 );
            uvC.fromArray( uvs, c * 2 );

            vB.sub( vA );
            vC.sub( vA );

            uvB.sub( uvA );
            uvC.sub( uvA );

            val r = 1.0 / ( uvB.x * uvC.y - uvC.x * uvB.y );

            // silently ignore degenerate uv triangles having coincident or colinear vertices

            if ( ! isFinite( r ) ) return;

            sdir.set( vB ).multiply( uvC.y ).addScaledVector( vC, - uvB.y ).multiply( r );
            tdir.set( vC ).multiply( uvB.x ).addScaledVector( vB, - uvC.x ).multiply( r );

            tan1[ a ].add( sdir );
            tan1[ b ].add( sdir );
            tan1[ c ].add( sdir );

            tan2[ a ].add( tdir );
            tan2[ b ].add( tdir );
            tan2[ c ].add( tdir );
        }

        fun handvarriangle(a: Double, b: Double, c: Double) {
            vA.fromArray(positions, a * 3)
            vB.fromArray(positions, b * 3)
            vC.fromArray(positions, c * 3)

            uvA.fromArray(uvs, a * 2)
            uvB.fromArray(uvs, b * 2)
            uvC.fromArray(uvs, c * 2)

            vB.sub(vA)
            vC.sub(vA)

            uvB.sub(uvA)
            uvC.sub(uvA)

            val r = 1.0 / (uvB.x * uvC.y - uvC.x * uvB.y)

            // silently ignore degenerate uv triangles having coincident or colinear vertices

            if (!isFinite(r)) return

            sdir.set(vB).multiply(uvC.y).addScaledVector(vC, -uvB.y).multiply(r)
            tdir.set(vC).multiply(uvB.x).addScaledVector(vB, -uvC.x).multiply(r)

            tan1[a].add(sdir)
            tan1[b].add(sdir)
            tan1[c].add(sdir)

            tan2[a].add(tdir)
            tan2[b].add(tdir)
            tan2[c].add(tdir)
        }
    }
}
