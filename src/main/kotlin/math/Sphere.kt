package math

@ExperimentalJsExport
@JsExport
class Sphere(
    val center: MutableVector3 = MutableVector3(),
    var radius: Double = -1.0
) {
    val empty: Boolean
        get() = this.radius < 0

    fun set( center: Vector3, radius: Double ): Sphere {
		this.center.set( center )
		this.radius = radius
		return this
	}

    fun setFromPoints( points: Array<Vector3>, optionalCenter: Vector3? = null ): Sphere {
		if ( optionalCenter != null ) {
            this.center.set( optionalCenter )
		} else {
            this.center.set(MutableBox3().setFromPoints(*points).center)
		}

		var maxRadiusSq = 0.0

        points.forEach {
            maxRadiusSq = kotlin.math.max( maxRadiusSq, center.distanceToSquared( it ) )
        }

		this.radius = kotlin.math.sqrt( maxRadiusSq )

		return this
	}

    @JsName("setFromSphere")
	fun set(sphere: Sphere): Sphere {
		this.center.set( sphere.center )
		this.radius = sphere.radius
		return this
	}

	fun makeEmpty() : Sphere {
		this.center.set( 0.0, 0.0, 0.0 )
		this.radius = - 1.0
		return this
	}

	fun contains( point : Vector3):Boolean {
		return ( point.distanceToSquared( this.center ) <= ( this.radius * this.radius ) )
	}

	fun distanceTo( point: Vector3 ): Double {
		return ( point.distanceTo( this.center ) - this.radius )
	}

    @JsName("intersectsSphere")
	fun intersects( sphere: Sphere ): Boolean {
		val radiusSum = this.radius + sphere.radius
		return sphere.center.distanceToSquared( this.center ) <= ( radiusSum * radiusSum )
	}

    @JsName("intersectsBox")
	fun intersects( box: MutableBox3 ): Boolean {
		return box.intersects( this )
	}

    @JsName("intersectsPlane")
    fun intersects(plane: Plane): Boolean {
        return kotlin.math.abs(plane.distanceToPoint(this.center)) <= this.radius
    }

    fun clampPoint( point: Vector3): Vector3 {
        val deltaLengthSq = this.center.distanceToSquared( point )
        val target = MutableVector3().set( point )

        if ( deltaLengthSq > ( this.radius * this.radius ) ) {
            target.sub( this.center ).normalize()
            target.multiply( this.radius ).add( this.center )
        }

        return target
    }

    fun getBoundingBox(): MutableBox3 {
        val target = MutableBox3()

        if ( this.empty ) {
            // Empty sphere produces empty bounding box
            target.makeEmpty()
        } else {
            target.set( this.center, this.center )
            target.expandByScalar( this.radius )
        }

        return target
    }

    @JsName("applyMatrix4")
    fun apply( matrix: Matrix4 ): Sphere {
        this.center.apply( matrix )
        this.radius = this.radius * matrix.getMaxScaleOnAxis()
        return this
    }

    fun translate( offset: Vector3 ): Sphere {
        this.center.add( offset )
        return this
    }

    fun expandByPoint( point: Vector3 ): Sphere {
        // from https://github.com/juj/MathGeoLib/blob/2940b99b99cfe575dd45103ef20f4019dee15b54/src/Geometry/Sphere.cpp#L649-L671
        val toPoint = MutableVector3().sub( point, this.center )
        val lengthSq = toPoint.lengthSq()

        if ( lengthSq > ( this.radius * this.radius ) ) {
            val length = kotlin.math.sqrt( lengthSq )
            val missingRadiusHalf = ( length - this.radius ) * 0.5

            // Nudge this sphere towards the target point. Add half the missing distance to radius,
            // and the other half to position. This gives a tighter enclosure, instead of if
            // the whole missing distance were just added to radius.

            this.center.add( toPoint.multiply( missingRadiusHalf / length ) )
            this.radius += missingRadiusHalf

        }

        return this
    }

    fun union( sphere: Sphere ): Sphere {
        // from https://github.com/juj/MathGeoLib/blob/2940b99b99cfe575dd45103ef20f4019dee15b54/src/Geometry/Sphere.cpp#L759-L769

        // To enclose another sphere into this sphere, we only need to enclose two points:
        // 1) Enclose the farthest point on the other sphere into this sphere.
        // 2) Enclose the opposite point of the farthest point into this sphere.

        val toFarthestPoint = MutableVector3()
            .sub( sphere.center, this.center )
            .normalize()
            .multiply( sphere.radius )

        this.expandByPoint( sphere.center.clone().add( toFarthestPoint ) )
        this.expandByPoint( sphere.center.clone().sub( toFarthestPoint ) )

        return this
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other === this) return true
        if (other !is Sphere) return false
        return this.center.equals(other.center) && this.radius == other.radius
    }

    fun clone(): Sphere {
        return Sphere().set( this )
    }
}
