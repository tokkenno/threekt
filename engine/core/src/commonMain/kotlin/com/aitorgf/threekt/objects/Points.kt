package com.aitorgf.threekt.objects

import com.aitorgf.threekt.core.BufferGeometry
import com.aitorgf.threekt.core.Object3D
import com.aitorgf.threekt.materials.Material
import com.aitorgf.threekt.materials.PointsMaterial
import com.aitorgf.threekt.math.Frustum
import com.aitorgf.threekt.core.Intersection
import com.aitorgf.threekt.core.Raycaster
import com.aitorgf.threekt.math.Matrix4
import com.aitorgf.threekt.math.Ray
import com.aitorgf.threekt.math.Sphere
import com.aitorgf.threekt.math.Vector3
import kotlin.math.sqrt
import kotlin.js.JsExport

/** Draws each vertex of a [BufferGeometry] as a point, typically with a [PointsMaterial]. */
@JsExport
class Points(
    var geometry: BufferGeometry = BufferGeometry(),
    var material: Material = PointsMaterial(),
) : Object3D() {
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
     * Shares the geometry and material of [source] and copies its morph target state when it is a
     * points. Returns this object.
     */
    override fun copy(source: Object3D, recursive: Boolean): Points {
        super.copy(source, recursive)
        if (source is Points) {
            morphTargets.copy(source.morphTargets)
            material = source.material
            geometry = source.geometry
        }
        return this
    }

    override fun clone(recursive: Boolean): Points = Points().copy(this, recursive)

    /** Tests the bounding sphere of this point cloud against [frustum], as the renderer does for culling. */
    @JsExport.Ignore
    override fun intersectsFrustum(frustum: Frustum): Boolean = frustum.intersectsObject(this)

    /**
     * Adds the points within [Raycaster.params] `Points.threshold` of the ray, after rejecting point clouds
     * whose enlarged bounding sphere the ray misses. Each hit reports the vertex index and its distance to
     * the ray.
     */
    override fun raycast(raycaster: Raycaster, intersects: MutableList<Intersection>): Boolean {
        val geometry = geometry
        val matrixWorld = matrixWorld
        val threshold = raycaster.params.points.threshold
        val drawRange = geometry.drawRange
        if (geometry.boundingSphere == null) geometry.computeBoundingSphere()
        pointsSphere.copy(geometry.boundingSphere!!).applyMatrix4(matrixWorld)
        pointsSphere.radius += threshold
        if (!raycaster.ray.intersectsSphere(pointsSphere)) return true
        pointsInverseMatrix.copy(matrixWorld).invert()
        pointsRay.copy(raycaster.ray).applyMatrix4(pointsInverseMatrix)
        val localThreshold = threshold / ((scale.x + scale.y + scale.z) / 3)
        val localThresholdSq = localThreshold * localThreshold
        val index = geometry.index
        val position = geometry.getAttribute("position") ?: return true
        val count = index?.count ?: position.count
        val start = maxOf(0, drawRange.start)
        val end = minOf(count.toLong(), drawRange.start.toLong() + drawRange.count).toInt()
        for (i in start until end) {
            val a = index?.getX(i)?.toInt() ?: i
            pointsPosition.fromBufferAttribute(position, a)
            testPoint(pointsPosition, a, localThresholdSq, matrixWorld, raycaster, intersects)
        }
        return true
    }

    private fun testPoint(
        point: Vector3, index: Int, localThresholdSq: Double, matrixWorld: Matrix4,
        raycaster: Raycaster, intersects: MutableList<Intersection>,
    ) {
        val rayPointDistanceSq = pointsRay.distanceSqToPoint(point)
        if (rayPointDistanceSq >= localThresholdSq) return
        val intersectPoint = Vector3()
        pointsRay.closestPointToPoint(point, intersectPoint)
        intersectPoint.applyMatrix4(matrixWorld)
        val distance = raycaster.ray.origin.distanceTo(intersectPoint)
        if (distance < raycaster.near || distance > raycaster.far) return
        intersects.add(Intersection(distance, intersectPoint, this).also {
            it.distanceToRay = sqrt(rayPointDistanceSq)
            it.index = index
        })
    }

    private companion object {
        val pointsInverseMatrix = Matrix4()
        val pointsRay = Ray()
        val pointsSphere = Sphere()
        val pointsPosition = Vector3()
    }
}
