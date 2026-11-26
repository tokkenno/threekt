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
import com.aitorgf.threekt.objects.InstancedMesh
import com.aitorgf.threekt.objects.Points
import com.aitorgf.threekt.objects.Sprite
import com.aitorgf.threekt.cameras.PerspectiveCamera
import com.aitorgf.threekt.math.Sphere
import kotlin.math.cos
import kotlin.math.sin
import com.aitorgf.threekt.renderers.CoordinateSystem
import kotlin.js.JsExport

/**
 * Finds intersections of a ray with mesh triangles, lines, points and sprites. Instanced meshes report
 * each hit instance through [Intersection.instanceId]. Three.js dispatches to each object's `raycast()`;
 * Threekt keeps the tests here.
 */
@JsExport
class Raycaster(var near: Double = 0.0, var far: Double = Double.POSITIVE_INFINITY) {
    /** World-space ray used for the tests, set by [setFromCamera] or directly. */
    val ray = Ray()
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
    private val instanceWorld = Matrix4()
    private val instanceLocal = Matrix4()
    private val sphere = Sphere()
    private val spriteScale = Vector3()
    private val spriteModelView = Matrix4()
    private val spritePosition = Vector3()

    /**
     * Camera of the last [setFromCamera] call. Sprites need it to know how they face the view; set it
     * directly when building the ray by hand.
     */
    var camera: Camera? = null

    /** Sets the ray from normalized device coordinates in [-1, 1]. */
    fun setFromCamera(coords: Vector2, camera: Camera) {
        camera.updateWorldMatrix(true, false)
        this.camera = camera
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
        if (node is Sprite) return intersectSprite(node, results)
        if (node is InstancedMesh) {
            if (node.matrixWorld.determinantAffine() == 0.0) return
            if (node.boundingSphere == null) node.computeBoundingSphere()
            sphere.copy(node.boundingSphere!!).applyMatrix4(node.matrixWorld)
            if (!ray.intersectsSphere(sphere)) return
            for (instanceId in 0 until node.count) {
                instanceWorld.multiplyMatrices(node.matrixWorld, node.getMatrixAt(instanceId, instanceLocal))
                intersectMesh(node, instanceWorld, instanceId, results)
            }
            return
        }
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
            is Mesh -> intersectMesh(node, node.matrixWorld, null, results)
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

    /** Tests the triangles of [node] placed with [matrixWorld], tagging hits with [instanceId]. */
    private fun intersectMesh(node: Mesh, matrixWorld: Matrix4, instanceId: Int?, results: MutableList<Intersection>) {
        val geometry = node.geometry
        val position = geometry.getAttribute("position") ?: return
        val indices = geometry.index
        val range = geometry.drawRange
        val start = maxOf(0, range.start)
        val end = minOf((indices?.size ?: position.count).toLong(), range.start.toLong() + range.count).toInt()
        if (matrixWorld.determinantAffine() == 0.0) return
        localRay.copy(ray).applyMatrix4(inverse.copy(matrixWorld).invertAffine())
        val bounds = geometry.boundingBox ?: run {
            geometry.computeBoundingBox()
            geometry.boundingBox
        }
        if (bounds != null && !localRay.intersectsBox(bounds)) return
        val uv = geometry.getAttribute("uv")
        // With several materials, each draw group is tested with its own material's side, as in Three.js.
        val materials = node.materials
        val ranges = if (materials == null) listOf(Triple(start, end, node.material)) else geometry.groups.mapNotNull { group ->
            val material = materials.getOrNull(group.materialIndex) ?: return@mapNotNull null
            Triple(maxOf(start, group.start), minOf(end.toLong(), group.start.toLong() + group.count).toInt(), material)
        }
        for ((rangeStart, rangeEnd, material) in ranges) for (i in rangeStart until rangeEnd - 2 step 3) {
            val ia = indices?.get(i) ?: i
            val ib = indices?.get(i + 1) ?: (i + 1)
            val ic = indices?.get(i + 2) ?: (i + 2)
            a.fromBufferAttribute(position, ia)
            b.fromBufferAttribute(position, ib)
            c.fromBufferAttribute(position, ic)
            val side = material.side
            val intersection = if (side == Side.Back) localRay.intersectTriangle(c, b, a, true, hit)
            else localRay.intersectTriangle(a, b, c, side == Side.Front, hit)
            if (intersection == null) continue
            world.copy(hit).applyMatrix4(matrixWorld)
            val distance = world.distanceTo(ray.origin)
            if (distance !in near..far) continue
            val normal = b.copy().sub(a).cross(c.copy().sub(a)).normalize()
            val w = if (uv != null) Triangle.getBarycoord(hit, a, b, c, weights) else null
            val coordinates = if (uv != null && w != null) Vector2(
                uv.getX(ia) * w.x + uv.getX(ib) * w.y + uv.getX(ic) * w.z,
                uv.getY(ia) * w.x + uv.getY(ib) * w.y + uv.getY(ic) * w.z,
            ) else null
            results.add(Intersection(distance, world.copy(), node, i / 3, ia, ib, ic, normal, coordinates, instanceId))
        }
    }

    /**
     * Tests the two camera-facing triangles of [sprite], as Three.js `Sprite.raycast`. Requires [camera].
     * Reports the UV at the hit point.
     */
    private fun intersectSprite(sprite: Sprite, results: MutableList<Intersection>) {
        val camera = requireNotNull(camera) { "Raycaster.camera must be set to intersect sprites." }
        spriteScale.setFromMatrixScale(sprite.matrixWorld)
        spriteModelView.multiplyMatrices(camera.matrixWorldInverse, sprite.matrixWorld)
        spritePosition.setFromMatrixPosition(spriteModelView)
        if (camera is PerspectiveCamera && !sprite.material.sizeAttenuation) spriteScale.multiplyScalar(-spritePosition.z)
        val rotation = sprite.material.rotation
        val sin = sin(rotation)
        val cos = cos(rotation)
        fun transformVertex(target: Vector3, x: Double, y: Double): Vector3 {
            val alignedX = (x - sprite.center.x + 0.5) * spriteScale.x
            val alignedY = (y - sprite.center.y + 0.5) * spriteScale.y
            return target.set(
                spritePosition.x + cos * alignedX - sin * alignedY,
                spritePosition.y + sin * alignedX + cos * alignedY,
                spritePosition.z,
            ).applyMatrix4(camera.matrixWorld)
        }
        // First triangle: (-0.5, -0.5), (0.5, -0.5), (0.5, 0.5) with UVs (0, 0), (1, 0), (1, 1).
        transformVertex(a, -0.5, -0.5)
        transformVertex(b, 0.5, -0.5)
        transformVertex(c, 0.5, 0.5)
        var uvB = 1.0 to 0.0
        var uvC = 1.0 to 1.0
        if (ray.intersectTriangle(a, b, c, false, hit) == null) {
            // Second triangle: (-0.5, -0.5), (0.5, 0.5), (-0.5, 0.5) with UVs (0, 0), (1, 1), (0, 1).
            b.copy(c)
            transformVertex(c, -0.5, 0.5)
            uvB = 1.0 to 1.0
            uvC = 0.0 to 1.0
            if (ray.intersectTriangle(a, b, c, false, hit) == null) return
        }
        val distance = ray.origin.distanceTo(hit)
        if (distance < near || distance > far) return
        val w = Triangle.getBarycoord(hit, a, b, c, weights)
        val uv = if (w != null) {
            Vector2(uvB.first * w.y + uvC.first * w.z, uvB.second * w.y + uvC.second * w.z)
        } else null
        results.add(Intersection(distance, hit.copy(), sprite, -1, uv = uv))
    }
}
