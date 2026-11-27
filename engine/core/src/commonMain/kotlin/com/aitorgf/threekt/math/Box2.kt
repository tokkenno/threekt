package com.aitorgf.threekt.math

/** An axis-aligned bounding rectangle. The default bounds represent an empty box. */
class Box2(
    val min: Vector2 = Vector2().setScalar(Double.POSITIVE_INFINITY),
    val max: Vector2 = Vector2().setScalar(Double.NEGATIVE_INFINITY),
) {

    /**
     * Sets the lower and upper boundaries of this box. Copies component values into the existing bounds.
     * Returns this instance.
     */
    fun set(min: Vector2, max: Vector2): Box2 = apply {
        this.min.copy(min)
        this.max.copy(max)
    }

    /** Copies the supplied values into this instance and returns it. */
    fun copy(box: Box2): Box2 = set(box.min, box.max)

    /** Returns a new instance with the same values. */
    fun copy(): Box2 = Box2().copy(this)

    /** Makes this box empty which means in encloses a zero space in 2D. Returns this instance. */
    fun makeEmpty(): Box2 = apply {
        min.setScalar(Double.POSITIVE_INFINITY)
        max.setScalar(Double.NEGATIVE_INFINITY)
    }

    /**
     * Returns whether any upper bound is below its corresponding lower bound. Coincident bounds form a
     * valid point-sized box.
     */
    fun isEmpty(): Boolean = max.x < min.x || max.y < min.y

    /**
     * Writes the midpoint of the bounds to [target] and returns it. An empty box yields the zero vector.
     */
    fun getCenter(target: Vector2): Vector2 =
        if (isEmpty()) target.setScalar(0.0) else target.addVectors(min, max).multiplyScalar(0.5)

    /** Writes the width and height to [target] and returns it. An empty box yields the zero vector. */
    fun getSize(target: Vector2): Vector2 =
        if (isEmpty()) target.setScalar(0.0) else target.subVectors(max, min)

    /** Replaces the bounds with a box enclosing all [points]. An empty input empties the box. */
    fun setFromPoints(points: Array<out Vector2>): Box2 = setFromPoints(points.asIterable())

    /** Replaces the bounds with a box enclosing all [points]. An empty input empties the box. */
    fun setFromPoints(points: Iterable<Vector2>): Box2 {
        makeEmpty()
        for (point in points) expandByPoint(point)
        return this
    }

    /**
     * Centers this box on the given center vector and sets this box's width, height and depth to the given
     * size values. Returns this instance.
     */
    fun setFromCenterAndSize(center: Vector2, size: Vector2): Box2 {
        val half = size.copy().multiplyScalar(0.5)
        return set(center.copy().sub(half), center.copy().add(half))
    }

    /** Expands the boundaries of this box to include the given point. Returns this instance. */
    fun expandByPoint(point: Vector2): Box2 = apply {
        min.min(point)
        max.max(point)
    }

    /**
     * Expands this box equilaterally by the given vector. The width of this box will be expanded by the x
     * component of the vector in both directions. The height of this box will be expanded by the y
     * component of the vector in both directions. Returns this instance.
     */
    fun expandByVector(vector: Vector2): Box2 = apply {
        min.sub(vector)
        max.add(vector)
    }

    /**
     * Expands each dimension of the box by the given scalar. If negative, the dimensions of the box will
     * be contracted. Returns this instance.
     */
    fun expandByScalar(scalar: Double): Box2 = apply {
        min.addScalar(-scalar)
        max.addScalar(scalar)
    }

    /** Returns `true` if the given point lies within or on the boundaries of this box. */
    fun containsPoint(point: Vector2): Boolean = point.x >= min.x && point.x <= max.x && point.y >= min.y && point.y <= max.y

    /**
     * Returns `true` if this bounding box includes the entirety of the given bounding box. If this box and
     * the given one are identical, this function also returns `true`.
     */
    fun containsBox(box: Box2): Boolean = containsPoint(box.min) && containsPoint(box.max)

    /** Returns a point as a proportion of this box's width and height. */
    fun getParameter(point: Vector2, target: Vector2): Vector2 =
        target.set((point.x - min.x) / (max.x - min.x), (point.y - min.y) / (max.y - min.y))

    /** Returns `true` if the given bounding box intersects with this bounding box. */
    fun intersectsBox(box: Box2): Boolean =
        box.max.x >= min.x && box.min.x <= max.x && box.max.y >= min.y && box.min.y <= max.y

    /** Clamps the given point within the bounds of this box. */
    fun clampPoint(point: Vector2, target: Vector2): Vector2 = target.copy(point).clamp(min, max)

    /**
     * Returns the Euclidean distance from [point] to the nearest point in this box. The distance is zero
     * for points inside the box.
     */
    fun distanceToPoint(point: Vector2): Double = clampPoint(point, Vector2()).distanceTo(point)

    /**
     * Computes the intersection of this bounding box and the given one, setting the upper bound of this
     * box to the lesser of the two boxes' upper bounds and the lower bound of this box to the greater of
     * the two boxes' lower bounds. If there's no overlap, makes this box empty. Returns this instance.
     */
    fun intersect(box: Box2): Box2 {
        min.max(box.min)
        max.min(box.max)
        if (isEmpty()) makeEmpty()
        return this
    }

    /** Expands this box to enclose its current bounds and [box]. Returns this box. */
    fun union(box: Box2): Box2 = apply {
        min.min(box.min)
        max.max(box.max)
    }

    /**
     * Adds the given offset to both the upper and lower bounds of this bounding box, effectively moving it
     * in 2D space. Returns this instance.
     */
    fun translate(offset: Vector2): Box2 = apply {
        min.add(offset)
        max.add(offset)
    }

    /** Returns `true` if this bounding box is equal with the given one. */
    override fun equals(other: Any?): Boolean = other is Box2 && min == other.min && max == other.max

    /** Returns a hash based on the current component values. */
    override fun hashCode(): Int = 31 * min.hashCode() + max.hashCode()
}
