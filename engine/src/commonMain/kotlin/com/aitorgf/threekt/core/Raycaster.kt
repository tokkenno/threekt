package com.aitorgf.threekt.core

import com.aitorgf.threekt.cameras.Camera
import com.aitorgf.threekt.cameras.OrthographicCamera
import com.aitorgf.threekt.materials.Side
import com.aitorgf.threekt.math.Matrix4
import com.aitorgf.threekt.math.Ray
import com.aitorgf.threekt.math.Triangle
import com.aitorgf.threekt.math.Vector2
import com.aitorgf.threekt.math.Vector3
import com.aitorgf.threekt.objects.Line
import com.aitorgf.threekt.objects.LineLoop
import com.aitorgf.threekt.objects.LineSegments
import com.aitorgf.threekt.objects.Mesh
import com.aitorgf.threekt.objects.Points
import com.aitorgf.threekt.renderers.CoordinateSystem
import kotlin.js.JsExport

/** Finds intersections of a ray with mesh triangles, lines and points. */
@JsExport
class Raycaster(var near: Double = 0.0, var far: Double = Double.POSITIVE_INFINITY) {
    private val ray = Ray()
    var lineThreshold = 1.0
    var pointsThreshold = 1.0

    private val inverse = Matrix4()
    private val localRay = Ray()
    private val a = Vector3()
    private val b = Vector3()
    private val c = Vector3()
    private val hit = Vector3()
    private val rayPoint = Vector3()
    private val world = Vector3()
    private val weights = Vector3()

    /** Sets the ray from normalized device coordinates in [-1, 1]. */
    fun setFromCamera(coords: Vector2, camera: Camera) {
        camera.updateWorldMatrix(true, false)
        if (camera is OrthographicCamera) {
            val low = if (camera.coordinateSystem == CoordinateSystem.WebGPU || camera.reversedDepth) 0.0 else -1.0
            val high = 1.0
            val front = if (camera.reversedDepth) high else low
            val back = if (camera.reversedDepth) low else high
            val z = front + (back - front) * -camera.near / (camera.far - camera.near)
            ray.origin.set(coords.x, coords.y, z).applyMatrix4(camera.projectionMatrixInverse).applyMatrix4(camera.matrixWorld)
            ray.direction.set(0.0, 0.0, -1.0).transformDirection(camera.matrixWorld)
        } else {
            ray.origin.setFromMatrixPosition(camera.matrixWorld)
            ray.direction.set(coords.x, coords.y, 0.5).applyMatrix4(camera.projectionMatrixInverse)
                .applyMatrix4(camera.matrixWorld).sub(ray.origin).normalize()
        }
    }

    /** Returns independent hits sorted by distance. Visibility does not affect ray tests. */
    fun intersectObject(object3d: Object3D, recursive: Boolean = true): Array<Intersection> =
        intersectObjects(arrayOf(object3d), recursive)

    /** Tests the objects and, optionally, their descendants. The caller keeps ownership of the input array. */
    fun intersectObjects(objects: Array<Object3D>, recursive: Boolean = true): Array<Intersection> {
        val results = mutableListOf<Intersection>()
        for (object3d in objects) {
            object3d.updateWorldMatrix(true, recursive)
            if (recursive) object3d.traverse { collect(it, results) } else collect(object3d, results)
        }
        return results.sortedBy { it.distance }.toTypedArray()
    }

    private fun collect(node: Object3D, results: MutableList<Intersection>) {
        val geometry = when (node) {
            is Mesh -> node.geometry
            is Line -> node.geometry
            is Points -> node.geometry
            else -> return
        }
        val position = geometry.getAttribute("position") ?: return
        val indices = geometry.index
        val range = geometry.drawRange
        val start = maxOf(0, range.start)
        val end = minOf((indices?.size ?: position.count).toLong(), range.start.toLong() + range.count).toInt()
        fun index(i: Int) = indices?.get(i) ?: i
        when (node) {
            is Mesh -> {
                if (node.matrixWorld.determinantAffine() == 0.0) return
                localRay.copy(ray).applyMatrix4(inverse.copy(node.matrixWorld).invertAffine())
                val bounds = geometry.boundingBox ?: run {
                    geometry.computeBoundingBox()
                    geometry.boundingBox
                }
                if (bounds != null && !localRay.intersectsBox(bounds)) return
                val uv = geometry.getAttribute("uv")
                for (i in start until end - 2 step 3) {
                    val ia = index(i)
                    val ib = index(i + 1)
                    val ic = index(i + 2)
                    a.fromBufferAttribute(position, ia)
                    b.fromBufferAttribute(position, ib)
                    c.fromBufferAttribute(position, ic)
                    val side = node.material.side
                    val intersection = if (side == Side.Back) localRay.intersectTriangle(c, b, a, true, hit)
                    else localRay.intersectTriangle(a, b, c, side == Side.Front, hit)
                    if (intersection == null) continue
                    world.copy(hit).applyMatrix4(node.matrixWorld)
                    val distance = world.distanceTo(ray.origin)
                    if (distance !in near..far) continue
                    val normal = b.copy().sub(a).cross(c.copy().sub(a)).normalize()
                    val w = if (uv != null) Triangle.getBarycoord(hit, a, b, c, weights) else null
                    val coordinates = if (uv != null && w != null) Vector2(
                        uv.getX(ia) * w.x + uv.getX(ib) * w.y + uv.getX(ic) * w.z,
                        uv.getY(ia) * w.x + uv.getY(ib) * w.y + uv.getY(ic) * w.z,
                    ) else null
                    results.add(Intersection(distance, world.copy(), node, i / 3, ia, ib, ic, normal, coordinates))
                }
            }
            is Points -> {
                val limit = pointsThreshold * pointsThreshold
                for (i in start until end) {
                    val vertex = index(i)
                    a.fromBufferAttribute(position, vertex).applyMatrix4(node.matrixWorld)
                    ray.closestPointToPoint(a, hit)
                    if (a.distanceToSquared(hit) >= limit) continue
                    val distance = hit.distanceTo(ray.origin)
                    if (distance in near..far) results.add(Intersection(distance, hit.copy(), node, vertex))
                }
            }
            is Line -> {
                val step = if (node is LineSegments) 2 else 1
                val limit = lineThreshold * lineThreshold
                val last = if (node is LineLoop && end - start > 1) end else end - 1
                for (i in start until last step step) {
                    a.fromBufferAttribute(position, index(i)).applyMatrix4(node.matrixWorld)
                    b.fromBufferAttribute(position, index(if (i + 1 == end) start else i + 1)).applyMatrix4(node.matrixWorld)
                    if (ray.distanceSqToSegment(a, b, rayPoint, hit) > limit) continue
                    val distance = rayPoint.distanceTo(ray.origin)
                    if (distance in near..far) results.add(Intersection(distance, hit.copy(), node, i))
                }
            }
        }
    }
}
