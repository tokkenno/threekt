package math

class Box2(
    var min: Vector2 = Vector2(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY),
    var max: Vector2 = Vector2(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY)
) : Box {
    val center: Vector2
        get() {
            return if (this.isEmpty()) Vector2() else this.min.clone().add(this.max).multiply(0.5)
        }

    val size: Vector2
        get() {
            return if (this.isEmpty()) Vector2() else this.max.clone().sub(this.min)
        }

    fun set(min: Vector2, max: Vector2): Box2 {
        this.min.set(min)
        this.max.set(max)
        return this
    }

    fun set(other: Box2): Box2 {
        this.min.set(other.min)
        this.max.set(other.max)
        return this
    }

    fun clone(): Box2 {
        return Box2().set(this)
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other === this) return true
        if (other !is Box2) return false
        return this.min == other.min && this.max == other.max
    }

    fun setFromCenterAndSize(center: Vector2, size: Vector2): Box2 {
        val halfSize = Vector2().set(size).multiply(0.5)
        this.min.set(center).sub(halfSize)
        this.max.set(center).add(halfSize)
        return this
    }

    override fun isEmpty(): Boolean {
        return (this.max.x < this.min.x) || (this.max.y < this.min.y)
    }

    fun expandByPoint(point: Vector2): Box2 {
        this.min.min(point)
        this.max.max(point)
        return this
    }

    fun expandByVector(vector: Vector2): Box2 {
        this.min.sub(vector)
        this.max.add(vector)
        return this
    }

    fun expandByScalar(scalar: Double): Box2 {
        this.min.add(-scalar)
        this.max.add(scalar)
        return this
    }

    fun containsPoint(point: Vector2): Boolean {
        return !(point.x < this.min.x || point.x > this.max.x ||
                point.y < this.min.y || point.y > this.max.y)
    }

    fun containsBox(box: Box2): Boolean {
        return this.min.x <= box.min.x && box.max.x <= this.max.x &&
                this.min.y <= box.min.y && box.max.y <= this.max.y
    }

    fun intersectsBox(box: Box2): Boolean {
        // using 4 splitting planes to rule out intersections
        return !(box.max.x < this.min.x || box.min.x > this.max.x ||
                box.max.y < this.min.y || box.min.y > this.max.y)
    }

    fun clampPoint(point: Vector2): Vector2 {
        return Vector2().set(point).clamp(this.min, this.max)
    }

    fun distanceToPoint(point: Vector2): Double {
        val clampedPoint = Vector2().set(point).clamp(this.min, this.max);
        return clampedPoint.sub(point).length()
    }

    fun intersect(box: Box2): Box2 {
        this.min.max(box.min)
        this.max.min(box.max)
        return this
    }

    fun union(box: Box2): Box2 {
        this.min.min(box.min)
        this.max.max(box.max)
        return this
    }

    fun translate(offset: Vector2): Box2 {
        this.min.add(offset);
        this.max.add(offset);
        return this
    }
}