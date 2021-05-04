package math

@ExperimentalJsExport
@JsExport
class Plane(
    val normal: MutableVector3 = MutableVector3(1.0, 0.0, 0.0),
    var constant: Double = 0.0
) {
    fun set(normal: Vector3, constant: Double): Plane {
        this.normal.set(normal)
        this.constant = constant

        return this
    }

    @JsName("setComponents")
    fun set(x: Double, y: Double, z: Double, w: Double): Plane {
        this.normal.set(x, y, z)
        this.constant = w

        return this
    }

    @JsName("setPlane")
    fun set(plane: Plane): Plane {
        this.normal.set(plane.normal)
        this.constant = plane.constant

        return this
    }

    fun setFromNormalAndCoplanarPoint(normal: Vector3, point: Vector3): Plane {
        this.normal.set(normal)
        this.constant = -point.dot(this.normal)

        return this
    }

    fun setFromCoplanarPoints(a: Vector3, b: Vector3, c: Vector3): Plane {
        val normal = MutableVector3().sub(c, b).cross(MutableVector3().sub(a, b)).normalize()

        // Q: should an error be thrown if normal is zero (e.g. degenerate plane)?
        this.setFromNormalAndCoplanarPoint(normal, a)

        return this
    }

    fun normalize(): Plane {
        // Note: will lead to a divide by zero if the plane is invalid.
        val inverseNormalLength = 1.0 / this.normal.length()
        this.normal.multiply(inverseNormalLength)
        this.constant *= inverseNormalLength

        return this
    }

    fun negate(): Plane {
        this.constant *= -1
        this.normal.negate()

        return this
    }

    fun distanceToPoint(point: Vector3): Double {
        return this.normal.dot(point) + this.constant
    }

    fun distanceToSphere(sphere: Sphere): Double {
        return this.distanceToPoint(sphere.center) - sphere.radius
    }

    fun projectPoint(point: Vector3): MutableVector3 {
        return MutableVector3().set(this.normal).multiply(-this.distanceToPoint(point)).add(point)
    }

    fun intersectLine( line: Line3): MutableVector3? {
        val direction = line.delta()
        val denominator = this.normal.dot(direction)

        if (denominator == 0.0) {
            // line is coplanar, return origin
            if (this.distanceToPoint(line.start) == 0.0) {
                return line.start.clone()
            }

            // Unsure if this is the correct method to handle this case.
            return null
        }

        val t = - (line.start.dot(this.normal) + this.constant) / denominator

        if (t < 0 || t > 1) {
            return null
        }

        return MutableVector3().set(direction).multiply(t).add(line.start)
    }

    fun intersectsLine( line: Line3 ): Boolean {
        // Note: this tests if a line intersects the plane, not whether it (or its end-points) are coplanar with it.
        val startSign = this.distanceToPoint(line.start)
        val endSign = this.distanceToPoint(line.end)

        return (startSign < 0 && endSign > 0) || (endSign < 0 && startSign > 0)
    }

    fun intersectsBox( box:  MutableBox3): Boolean {
        return box.intersects(this)
    }

    fun intersectsSphere( sphere: Sphere ): Boolean {
        return sphere.intersects(this)
    }

    fun coplanarPoint(): MutableVector3 {
        return MutableVector3().set(this.normal).multiply(-this.constant)

    }

    @JsName("applyMatrix4")
    fun apply( matrix: Matrix4): Plane {
        val normalMatrix = MutableMatrix3().getNormalMatrix(matrix)
        val referencePoint = this.coplanarPoint().apply(matrix)

        val normal = this.normal.apply(normalMatrix).normalize()

        this.constant = -referencePoint.dot(normal)

        return this

    }

    fun translate( offset: Vector3 ): Plane {
        this.constant -= offset.dot(this.normal)
        return this
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other !is Plane) return false
        return other.normal.equals(this.normal) && (other.constant == this.constant)
    }

    fun clone(): Plane {
        return Plane().set(this)
    }
}
