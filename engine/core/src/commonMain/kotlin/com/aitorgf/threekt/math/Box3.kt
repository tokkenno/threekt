package com.aitorgf.threekt.math

import com.aitorgf.threekt.core.BufferAttribute
import com.aitorgf.threekt.core.Object3D
import com.aitorgf.threekt.objects.Line
import com.aitorgf.threekt.objects.Mesh
import com.aitorgf.threekt.objects.Points
import kotlin.js.JsExport
import kotlin.js.JsName
import kotlin.math.abs

/** An axis-aligned bounding box in three dimensions. The default bounds represent an empty box. */
@JsExport
class Box3(
    val min: Vector3 = Vector3().setScalar(Double.POSITIVE_INFINITY),
    val max: Vector3 = Vector3().setScalar(Double.NEGATIVE_INFINITY),
) {

    /**
     * Sets the lower and upper boundaries of this box. Copies component values into the existing bounds.
     * Returns this instance.
     */
    fun set(min: Vector3, max: Vector3): Box3 = apply {
        this.min.copy(min)
        this.max.copy(max)
    }

    /** Copies the supplied values into this instance and returns it. */
    @JsName("copyFrom")
    fun copy(box: Box3): Box3 = set(box.min, box.max)

    /** Returns a new instance with the same values. */
    fun copy(): Box3 = Box3().copy(this)

    /** Makes this box empty which means in encloses a zero space in 3D. Returns this instance. */
    fun makeEmpty(): Box3 = apply {
        min.setScalar(Double.POSITIVE_INFINITY)
        max.setScalar(Double.NEGATIVE_INFINITY)
    }

    /**
     * Returns whether any upper bound is below its corresponding lower bound. Coincident bounds form a
     * valid point-sized box.
     */
    fun isEmpty(): Boolean = max.x < min.x || max.y < min.y || max.z < min.z

    /**
     * Writes the midpoint of the bounds to [target] and returns it. An empty box yields the zero vector.
     */
    fun getCenter(target: Vector3): Vector3 =
        if (isEmpty()) target.setScalar(0.0) else target.addVectors(min, max).multiplyScalar(0.5)

    /**
     * Writes the extent along each axis to [target] and returns it. An empty box yields the zero vector.
     */
    fun getSize(target: Vector3): Vector3 =
        if (isEmpty()) target.setScalar(0.0) else target.subVectors(max, min)

    /** Replaces the bounds with a box enclosing all [points]. An empty input empties the box. */
    fun setFromPoints(points: Array<out Vector3>): Box3 = setFromPoints(points.asIterable())

    /** Replaces the bounds with a box enclosing all [points]. An empty input empties the box. */
    @JsExport.Ignore
    fun setFromPoints(points: Iterable<Vector3>): Box3 {
        makeEmpty()
        for (point in points) expandByPoint(point)
        return this
    }

    /**
     * Centers this box on the given center vector and sets this box's width, height and depth to the given
     * size values. Returns this instance.
     */
    fun setFromCenterAndSize(center: Vector3, size: Vector3): Box3 {
        val half = size.copy().multiplyScalar(0.5)
        return set(center.copy().sub(half), center.copy().add(half))
    }

    /** Expands the boundaries of this box to include the given point. Returns this instance. */
    fun expandByPoint(point: Vector3): Box3 = apply {
        min.min(point)
        max.max(point)
    }

    /**
     * Expands this box equilaterally by the given vector. The width of this box will be expanded by the x
     * component of the vector in both directions. The height of this box will be expanded by the y
     * component of the vector in both directions. The depth of this box will be expanded by the z
     * component of the vector in both directions. Returns this instance.
     */
    fun expandByVector(vector: Vector3): Box3 = apply {
        min.sub(vector)
        max.add(vector)
    }

    /**
     * Expands each dimension of the box by the given scalar. If negative, the dimensions of the box will
     * be contracted. Returns this instance.
     */
    fun expandByScalar(scalar: Double): Box3 = apply {
        min.addScalar(-scalar)
        max.addScalar(scalar)
    }

    /** Returns `true` if the given point lies within or on the boundaries of this box. */
    fun containsPoint(point: Vector3): Boolean = point.x >= min.x && point.x <= max.x && point.y >= min.y && point.y <= max.y && point.z >= min.z && point.z <= max.z

    /**
     * Returns `true` if this bounding box includes the entirety of the given bounding box. If this box and
     * the given one are identical, this function also returns `true`.
     */
    fun containsBox(box: Box3): Boolean = containsPoint(box.min) && containsPoint(box.max)

    /** Returns a point as a proportion of this box's width, height and depth. */
    fun getParameter(point: Vector3, target: Vector3): Vector3 =
        target.set((point.x - min.x) / (max.x - min.x), (point.y - min.y) / (max.y - min.y), (point.z - min.z) / (max.z - min.z))

    /** Returns `true` if the given bounding box intersects with this bounding box. */
    fun intersectsBox(box: Box3): Boolean =
        box.max.x >= min.x && box.min.x <= max.x && box.max.y >= min.y && box.min.y <= max.y && box.max.z >= min.z && box.min.z <= max.z

    /** Clamps the given point within the bounds of this box. */
    fun clampPoint(point: Vector3, target: Vector3): Vector3 = target.copy(point).clamp(min, max)

    /**
     * Returns the Euclidean distance from [point] to the nearest point in this box. The distance is zero
     * for points inside the box.
     */
    fun distanceToPoint(point: Vector3): Double = clampPoint(point, Vector3()).distanceTo(point)

    /**
     * Computes the intersection of this bounding box and the given one, setting the upper bound of this
     * box to the lesser of the two boxes' upper bounds and the lower bound of this box to the greater of
     * the two boxes' lower bounds. If there's no overlap, makes this box empty. Returns this instance.
     */
    fun intersect(box: Box3): Box3 {
        min.max(box.min)
        max.min(box.max)
        if (isEmpty()) makeEmpty()
        return this
    }

    /** Expands this box to enclose its current bounds and [box]. Returns this box. */
    fun union(box: Box3): Box3 = apply {
        min.min(box.min)
        max.max(box.max)
    }

    /**
     * Adds the given offset to both the upper and lower bounds of this bounding box, effectively moving it
     * in 3D space. Returns this instance.
     */
    fun translate(offset: Vector3): Box3 = apply {
        min.add(offset)
        max.add(offset)
    }

    /** Returns `true` if this bounding box is equal with the given one. */
    override fun equals(other: Any?): Boolean = other is Box3 && min == other.min && max == other.max

    /** Returns a hash based on the current component values. */
    override fun hashCode(): Int = 31 * min.hashCode() + max.hashCode()

    /**
     * Sets the upper and lower bounds of this box so it encloses the position data in the given array.
     * Returns this instance.
     */
    fun setFromArray(array: DoubleArray): Box3 {
        makeEmpty()
        val point = Vector3()
        for (offset in array.indices step 3) expandByPoint(point.fromArray(array, offset))
        return this
    }

    /**
     * Sets the upper and lower bounds of this box so it encloses the position data in the given buffer
     * attribute. Returns this instance.
     */
    fun setFromBufferAttribute(attribute: BufferAttribute): Box3 {
        makeEmpty()
        val point = Vector3()
        repeat(attribute.count) { expandByPoint(point.fromBufferAttribute(attribute, it)) }
        return this
    }

    /**
     * Replaces this box with the world-space bounds of mesh vertices under [object3D]. With [precise],
     * transforms each vertex before measuring the bounds. Returns this box.
     */
    fun setFromObject(object3D: Object3D, precise: Boolean = false): Box3 =
        makeEmpty().expandByObject(object3D, precise)

    /**
     * Expands this box to include the world-space bounds of the meshes, lines, and points under
     * [object3D]. With [precise], transforms each vertex before measuring the bounds. Returns this box.
     */
    fun expandByObject(object3D: Object3D, precise: Boolean = false): Box3 {
        var root = object3D
        while (root.parent != null) root = checkNotNull(root.parent)
        root.updateMatrixWorld()
        object3D.traverse { expandByNode(it, precise) }
        return this
    }

    private fun expandByNode(node: Object3D, precise: Boolean) {
        val geometry = when (node) {
            is Mesh -> node.geometry
            is Line -> node.geometry
            is Points -> node.geometry
            else -> null
        }
        val positions = geometry?.getAttribute("position")
        if (positions != null) {
            if (precise) {
                val point = Vector3()
                repeat(positions.count) { expandByPoint(point.fromBufferAttribute(positions, it).applyMatrix4(node.matrixWorld)) }
            } else {
                union(Box3().setFromBufferAttribute(positions).applyMatrix4(node.matrixWorld))
            }
        }
    }

    /** Returns `true` if the given bounding sphere intersects with this bounding box. */
    fun intersectsSphere(sphere: Sphere): Boolean =
        clampPoint(sphere.center, Vector3()).distanceToSquared(sphere.center) <= sphere.radius * sphere.radius

    /** Returns `true` if the given plane intersects with this bounding box. */
    fun intersectsPlane(plane: Plane): Boolean {
        val center = getCenter(Vector3())
        val extents = getSize(Vector3()).multiplyScalar(0.5)
        val n = plane.normal
        val radius = extents.x * abs(n.x) + extents.y * abs(n.y) + extents.z * abs(n.z)
        return !isEmpty() && abs(plane.distanceToPoint(center)) <= radius
    }

    /** Returns `true` if the given triangle intersects with this bounding box. */
    fun intersectsTriangle(triangle: Triangle): Boolean {
        if (isEmpty()) return false
        val center = getCenter(Vector3())
        val extents = getSize(Vector3()).multiplyScalar(0.5)
        val a = triangle.a.copy().sub(center)
        val b = triangle.b.copy().sub(center)
        val c = triangle.c.copy().sub(center)
        val edges = listOf(b.copy().sub(a), c.copy().sub(b), a.copy().sub(c))
        val axes = listOf(Vector3(1.0, 0.0, 0.0), Vector3(0.0, 1.0, 0.0), Vector3(0.0, 0.0, 1.0))
        fun overlaps(axis: Vector3): Boolean {
            val radius = extents.x * abs(axis.x) + extents.y * abs(axis.y) + extents.z * abs(axis.z)
            val da = a.dot(axis)
            val db = b.dot(axis)
            val dc = c.dot(axis)
            return minOf(da, db, dc) <= radius && maxOf(da, db, dc) >= -radius
        }
        return axes.all(::overlaps) &&
            edges.all { edge -> axes.all { axis -> overlaps(Vector3().crossVectors(edge, axis)) } } &&
            overlaps(Vector3().crossVectors(edges[0], edges[1]))
    }

    /** Returns a bounding sphere that encloses this bounding box. */
    fun getBoundingSphere(target: Sphere): Sphere {
        if (isEmpty()) return target.makeEmpty()
        getCenter(target.center)
        target.radius = getSize(Vector3()).length() * 0.5
        return target
    }

    /** Transforms this bounding box by the given 4x4 transformation matrix. Returns this instance. */
    fun applyMatrix4(matrix: Matrix4): Box3 {
        if (isEmpty()) return this
        val lower = min.copy()
        val upper = max.copy()
        makeEmpty()
        val corner = Vector3()
        for (index in 0..7) {
            corner.set(
                if (index and 1 == 0) lower.x else upper.x,
                if (index and 2 == 0) lower.y else upper.y,
                if (index and 4 == 0) lower.z else upper.z,
            )
            expandByPoint(corner.applyMatrix4(matrix))
        }
        return this
    }

    /** Captures the two bounds as immutable coordinate values. */
    fun snapshot(): Snapshot = Snapshot(Point(min.x, min.y, min.z), Point(max.x, max.y, max.z))

    /** Copies the bounds in [snapshot] into this box and returns it. */
    fun restore(snapshot: Snapshot): Box3 = apply {
        min.set(snapshot.min.x, snapshot.min.y, snapshot.min.z)
        max.set(snapshot.max.x, snapshot.max.y, snapshot.max.z)
    }

    /** Immutable coordinates of one bounding-box corner. */
    data class Point(val x: Double, val y: Double, val z: Double)

    /** Immutable lower and upper bounds of a box. */
    data class Snapshot(val min: Point, val max: Point)
}
