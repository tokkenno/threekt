package com.aitorgf.threekt.objects

import com.aitorgf.threekt.core.BufferGeometry
import com.aitorgf.threekt.core.Float32BufferAttribute
import com.aitorgf.threekt.core.Object3D
import com.aitorgf.threekt.materials.LineBasicMaterial
import com.aitorgf.threekt.materials.Material
import com.aitorgf.threekt.math.Vector3
import com.aitorgf.threekt.math.Frustum
import com.aitorgf.threekt.core.Intersection
import com.aitorgf.threekt.core.Raycaster
import com.aitorgf.threekt.math.Matrix4
import com.aitorgf.threekt.math.Ray
import com.aitorgf.threekt.math.Sphere
import kotlin.js.JsExport

/** A continuous line through the vertices of a [BufferGeometry], drawn with a [Material]. */
@JsExport
open class Line(
    var geometry: BufferGeometry = BufferGeometry(),
    var material: Material = LineBasicMaterial(),
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
     * Stores the distance along the line at each vertex in a `lineDistance` attribute, for dashed
     * materials. Only non-indexed geometry is supported; indexed geometry is left unchanged. Returns this
     * line.
     */
    open fun computeLineDistances(): Line {
        val position = geometry.getAttribute("position")
        if (geometry.index != null || position == null) return this
        val start = Vector3()
        val end = Vector3()
        val distances = FloatArray(position.count)
        var distance = 0.0
        for (index in 1 until position.count) {
            distance += start.fromBufferAttribute(position, index - 1).distanceTo(end.fromBufferAttribute(position, index))
            distances[index] = distance.toFloat()
        }
        geometry.setAttribute("lineDistance", Float32BufferAttribute(distances, 1))
        return this
    }

    /**
     * Shares the geometry and material of [source] and copies its morph target state when it is a
     * line. Returns this object.
     */
    override fun copy(source: Object3D, recursive: Boolean): Line {
        super.copy(source, recursive)
        if (source is Line) {
            morphTargets.copy(source.morphTargets)
            material = source.material
            geometry = source.geometry
        }
        return this
    }

    override fun clone(recursive: Boolean): Line = Line().copy(this, recursive)

    /** Tests the bounding sphere of this line against [frustum], as the renderer does for culling. */
    @JsExport.Ignore
    override fun intersectsFrustum(frustum: Frustum): Boolean = frustum.intersectsObject(this)

    /**
     * Adds the segments that pass within [Raycaster.params] `Line.threshold` of the ray, after rejecting
     * lines whose enlarged bounding sphere the ray misses. Each hit reports the index of the segment's first
     * vertex.
     */
    override fun raycast(raycaster: Raycaster, intersects: MutableList<Intersection>): Boolean {
        val geometry = geometry
        val matrixWorld = matrixWorld
        val threshold = raycaster.params.line.threshold
        val drawRange = geometry.drawRange
        if (geometry.boundingSphere == null) geometry.computeBoundingSphere()
        lineSphere.copy(geometry.boundingSphere!!).applyMatrix4(matrixWorld)
        lineSphere.radius += threshold
        if (!raycaster.ray.intersectsSphere(lineSphere)) return true
        lineInverseMatrix.copy(matrixWorld).invert()
        lineRay.copy(raycaster.ray).applyMatrix4(lineInverseMatrix)
        val localThreshold = threshold / ((scale.x + scale.y + scale.z) / 3)
        val localThresholdSq = localThreshold * localThreshold
        val step = if (this is LineSegments) 2 else 1
        val index = geometry.index
        val position = geometry.getAttribute("position") ?: return true
        val count = index?.count ?: position.count
        fun vertex(i: Int) = index?.getX(i)?.toInt() ?: i
        val start = maxOf(0, drawRange.start)
        val end = minOf(count.toLong(), drawRange.start.toLong() + drawRange.count).toInt()
        for (i in start until end - 1 step step) {
            checkLineIntersection(this, raycaster, lineRay, localThresholdSq, vertex(i), vertex(i + 1), i)?.let { intersects.add(it) }
        }
        if (this is LineLoop) {
            checkLineIntersection(this, raycaster, lineRay, localThresholdSq, vertex(end - 1), vertex(start), end - 1)
                ?.let { intersects.add(it) }
        }
        return true
    }

    private companion object {
        val lineStart = Vector3()
        val lineEnd = Vector3()
        val lineInverseMatrix = Matrix4()
        val lineRay = Ray()
        val lineSphere = Sphere()
        val intersectPointOnRay = Vector3()
        val intersectPointOnSegment = Vector3()

        fun checkLineIntersection(
            line: Line, raycaster: Raycaster, ray: Ray, thresholdSq: Double, a: Int, b: Int, i: Int,
        ): Intersection? {
            val position = line.geometry.getAttribute("position")!!
            lineStart.fromBufferAttribute(position, a)
            lineEnd.fromBufferAttribute(position, b)
            val distSq = ray.distanceSqToSegment(lineStart, lineEnd, intersectPointOnRay, intersectPointOnSegment)
            if (distSq > thresholdSq) return null
            intersectPointOnRay.applyMatrix4(line.matrixWorld)
            val distance = raycaster.ray.origin.distanceTo(intersectPointOnRay)
            if (distance < raycaster.near || distance > raycaster.far) return null
            return Intersection(distance, intersectPointOnSegment.copy().applyMatrix4(line.matrixWorld), line).also { it.index = i }
        }
    }
}
