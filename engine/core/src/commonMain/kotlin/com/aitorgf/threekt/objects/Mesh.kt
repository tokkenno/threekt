package com.aitorgf.threekt.objects

import com.aitorgf.threekt.core.BufferAttribute
import com.aitorgf.threekt.core.BufferGeometry
import com.aitorgf.threekt.core.Intersection
import com.aitorgf.threekt.core.Raycaster
import com.aitorgf.threekt.core.Object3D
import com.aitorgf.threekt.materials.Material
import com.aitorgf.threekt.materials.MeshBasicMaterial
import com.aitorgf.threekt.materials.Side
import com.aitorgf.threekt.math.Frustum
import com.aitorgf.threekt.math.Matrix4
import com.aitorgf.threekt.math.Ray
import com.aitorgf.threekt.math.Sphere
import com.aitorgf.threekt.math.Triangle
import com.aitorgf.threekt.math.Vector2
import com.aitorgf.threekt.math.Vector3
import kotlin.js.JsExport
import kotlin.js.JsName

/**
 * A scene node that draws the triangles of a [BufferGeometry] with a [Material]. With [materials], each
 * draw group of the geometry uses the material at its index instead, as when Three.js receives an array
 * of materials.
 */
@JsExport
open class Mesh(
    var geometry: BufferGeometry = BufferGeometry(),
    var material: Material = MeshBasicMaterial(),
) : Object3D() {
    /**
     * Creates a multi-material mesh. Three.js accepts the array in the `material` argument; from JavaScript
     * call `Mesh.withMaterials(geometry, materials)`.
     */
    @JsName("withMaterials")
    constructor(geometry: BufferGeometry, materials: Array<Material>) : this(geometry, materials[0]) {
        this.materials = materials
    }

    /**
     * Materials selected by the [BufferGeometry.groups] of the geometry, or null to draw the whole
     * geometry with [material]. Groups whose index has no material are skipped.
     */
    var materials: Array<Material>? = null

    private val morphTargets = MorphTargets()

    init {
        updateMorphTargets()
    }

    /**
     * Weight of each morph target of the geometry, or null when the geometry had no morph targets at the
     * last [updateMorphTargets]. Influences are applied by the renderer on every draw.
     */
    val morphTargetInfluences: DoubleArray? get() = morphTargets.influences

    /** Index of each morph target by name, or by position when the target has no name. */
    @JsExport.Ignore
    val morphTargetDictionary: Map<String, Int>? get() = morphTargets.dictionary

    /** Resets [morphTargetInfluences] to zero for the geometry's current morph targets. */
    fun updateMorphTargets() = morphTargets.update(geometry)

    /**
     * Shares the geometry and materials of [source] and copies its morph target state when it is a
     * mesh. Returns this object.
     */
    override fun copy(source: Object3D, recursive: Boolean): Mesh {
        super.copy(source, recursive)
        if (source is Mesh) {
            morphTargets.copy(source.morphTargets)
            material = source.material
            materials = source.materials?.copyOf()
            geometry = source.geometry
        }
        return this
    }

    override fun clone(recursive: Boolean): Mesh = Mesh().copy(this, recursive)

    /** Tests the bounding sphere of this mesh against [frustum], as the renderer does for culling. */
    @JsExport.Ignore
    override fun intersectsFrustum(frustum: Frustum): Boolean = frustum.intersectsObject(this)

    /**
     * Writes the position of vertex [index] to [target], with the current morph target influences applied,
     * and returns it.
     */
    fun getVertexPosition(index: Int, target: Vector3): Vector3 {
        val position = geometry.getAttribute("position")!!
        target.fromBufferAttribute(position, index)
        val morphPosition = geometry.morphAttributes["position"]
        val influences = morphTargetInfluences
        if (morphPosition != null && influences != null) {
            morphA.set(0.0, 0.0, 0.0)
            for ((i, morphAttribute) in morphPosition.withIndex()) {
                val influence = influences[i]
                if (influence == 0.0) continue
                tempA.fromBufferAttribute(morphAttribute, index)
                if (geometry.morphTargetsRelative) {
                    morphA.addScaledVector(tempA, influence)
                } else {
                    morphA.addScaledVector(tempA.sub(target), influence)
                }
            }
            target.add(morphA)
        }
        return target
    }

    /**
     * Adds the triangles hit by the ray of [raycaster], after rejecting meshes whose bounding sphere and
     * box the ray misses. With several materials, each draw group is tested with its material's side.
     */
    override fun raycast(raycaster: Raycaster, intersects: MutableList<Intersection>): Boolean {
        val geometry = geometry
        val matrixWorld = matrixWorld
        if (geometry.boundingSphere == null) geometry.computeBoundingSphere()
        sphere.copy(geometry.boundingSphere!!)
        sphere.applyMatrix4(matrixWorld)
        localRay.copy(raycaster.ray).recast(raycaster.near)
        if (!sphere.containsPoint(localRay.origin)) {
            if (localRay.intersectSphere(sphere, sphereHitAt) == null) return true
            val range = raycaster.far - raycaster.near
            if (localRay.origin.distanceToSquared(sphereHitAt) > range * range) return true
        }
        inverseMatrix.copy(matrixWorld).invert()
        localRay.copy(raycaster.ray).applyMatrix4(inverseMatrix)
        val boundingBox = geometry.boundingBox
        if (boundingBox != null && !localRay.intersectsBox(boundingBox)) return true
        computeIntersections(raycaster, intersects, localRay)
        return true
    }

    /** Tests every triangle in the draw range, and in each group with several materials, against [rayLocalSpace]. */
    internal fun computeIntersections(raycaster: Raycaster, intersects: MutableList<Intersection>, rayLocalSpace: Ray) {
        val geometry = geometry
        val index = geometry.index
        val position = geometry.getAttribute("position") ?: return
        val uv = geometry.getAttribute("uv")
        val uv1 = geometry.getAttribute("uv1")
        val normal = geometry.getAttribute("normal")
        val drawRange = geometry.drawRange
        val count = index?.count ?: position.count
        fun vertex(i: Int) = index?.getX(i)?.toInt() ?: i
        val materials = materials
        if (materials != null) {
            for (group in geometry.groups) {
                val groupMaterial = materials.getOrNull(group.materialIndex) ?: continue
                val start = maxOf(group.start, drawRange.start)
                val end = minOf(count.toLong(), minOf(group.start.toLong() + group.count, drawRange.start.toLong() + drawRange.count)).toInt()
                for (j in start until end step 3) {
                    val intersection = checkGeometryIntersection(
                        this, groupMaterial, raycaster, rayLocalSpace, uv, uv1, normal, vertex(j), vertex(j + 1), vertex(j + 2),
                    ) ?: continue
                    intersection.faceIndex = j / 3
                    intersection.face!!.materialIndex = group.materialIndex
                    intersects.add(intersection)
                }
            }
        } else {
            val start = maxOf(0, drawRange.start)
            val end = minOf(count.toLong(), drawRange.start.toLong() + drawRange.count).toInt()
            for (i in start until end step 3) {
                val intersection = checkGeometryIntersection(
                    this, material, raycaster, rayLocalSpace, uv, uv1, normal, vertex(i), vertex(i + 1), vertex(i + 2),
                ) ?: continue
                intersection.faceIndex = i / 3
                intersects.add(intersection)
            }
        }
    }

    private val morphA = Vector3()
    private val tempA = Vector3()

    private companion object {
        val inverseMatrix = Matrix4()
        val localRay = Ray()
        val sphere = Sphere()
        val sphereHitAt = Vector3()
        val vA = Vector3()
        val vB = Vector3()
        val vC = Vector3()
        val intersectionPoint = Vector3()
        val intersectionPointWorld = Vector3()

        fun checkIntersection(
            obj: Object3D, material: Material, raycaster: Raycaster, ray: Ray,
            pA: Vector3, pB: Vector3, pC: Vector3, point: Vector3,
        ): Intersection? {
            val intersect = if (material.side == Side.Back) {
                ray.intersectTriangle(pC, pB, pA, true, point)
            } else {
                ray.intersectTriangle(pA, pB, pC, material.side == Side.Front, point)
            }
            if (intersect == null) return null
            intersectionPointWorld.copy(point).applyMatrix4(obj.matrixWorld)
            val distance = raycaster.ray.origin.distanceTo(intersectionPointWorld)
            if (distance < raycaster.near || distance > raycaster.far) return null
            return Intersection(distance, intersectionPointWorld.copy(), obj)
        }

        fun checkGeometryIntersection(
            mesh: Mesh, material: Material, raycaster: Raycaster, ray: Ray,
            uv: BufferAttribute?, uv1: BufferAttribute?, normal: BufferAttribute?, a: Int, b: Int, c: Int,
        ): Intersection? {
            mesh.getVertexPosition(a, vA)
            mesh.getVertexPosition(b, vB)
            mesh.getVertexPosition(c, vC)
            val intersection = checkIntersection(mesh, material, raycaster, ray, vA, vB, vC, intersectionPoint) ?: return null
            val barycoord = Vector3()
            Triangle.getBarycoord(intersectionPoint, vA, vB, vC, barycoord)
            if (uv != null) intersection.uv = Triangle.getInterpolatedAttribute(uv, a, b, c, barycoord, Vector2())
            if (uv1 != null) intersection.uv1 = Triangle.getInterpolatedAttribute(uv1, a, b, c, barycoord, Vector2())
            if (normal != null) {
                val interpolated = Triangle.getInterpolatedAttribute(normal, a, b, c, barycoord, Vector3())
                if (interpolated.dot(ray.direction) > 0) interpolated.multiplyScalar(-1.0)
                intersection.normal = interpolated
            }
            intersection.face = Intersection.Face(a, b, c, Triangle.getNormal(vA, vB, vC, Vector3()))
            intersection.barycoord = barycoord
            return intersection
        }
    }
}
