package math

@ExperimentalJsExport
@JsExport
class Frustum(
    p0: Plane = Plane(),
    p1: Plane = Plane(),
    p2: Plane = Plane(),
    p3: Plane = Plane(),
    p4: Plane = Plane(),
    p5: Plane = Plane()
) {
    val planes: Array<Plane> = arrayOf(p0, p1, p2, p3, p4, p5)


    fun set(p0: Plane, p1: Plane, p2: Plane, p3: Plane, p4: Plane, p5: Plane): Frustum {
        this.planes[0].set(p0)
        this.planes[1].set(p1)
        this.planes[2].set(p2)
        this.planes[3].set(p3)
        this.planes[4].set(p4)
        this.planes[5].set(p5)

        return this
    }

    @JsName("setFrustrum")
    fun set(frustum: Frustum): Frustum {
        for (i in 0..6) {
            this.planes[i] = frustum.planes[i]
        }

        return this
    }

    fun copy(frustum: Frustum): Frustum {
        frustum.planes.forEachIndexed { index, plane ->
            this.planes[index] = plane
        }

        return this
    }

    fun setFromProjectionMatrix(m: Matrix4): Frustum {
        val me0 = m.elements[0]
        val me1 = m.elements[1]
        val me2 = m.elements[2]
        val me3 = m.elements[3]
        val me4 = m.elements[4]
        val me5 = m.elements[5]
        val me6 = m.elements[6]
        val me7 = m.elements[7]
        val me8 = m.elements[8]
        val me9 = m.elements[9]
        val me10 = m.elements[10]
        val me11 = m.elements[11]
        val me12 = m.elements[12]
        val me13 = m.elements[13]
        val me14 = m.elements[14]
        val me15 = m.elements[15]

        this.planes[0].set(me3 - me0, me7 - me4, me11 - me8, me15 - me12).normalize()
        this.planes[1].set(me3 + me0, me7 + me4, me11 + me8, me15 + me12).normalize()
        this.planes[2].set(me3 + me1, me7 + me5, me11 + me9, me15 + me13).normalize()
        this.planes[3].set(me3 - me1, me7 - me5, me11 - me9, me15 - me13).normalize()
        this.planes[4].set(me3 - me2, me7 - me6, me11 - me10, me15 - me14).normalize()
        this.planes[5].set(me3 + me2, me7 + me6, me11 + me10, me15 + me14).normalize()

        return this
    }

    /* TODO
    fun intersectsObject( obj: Object3 ): Boolean {
        val geometry = obj.geometry

        if ( geometry.boundingSphere == null ) {
            geometry.computeBoundingSphere()
        }

        Sphere().set( geometry.boundingSphere ).applyMatrix4( obj.matrixWorld )

        return this.intersectsSphere( _sphere )
    }

    intersectsSprite( sprite ) {

        _sphere.center.set( 0, 0, 0 )
        _sphere.radius = 0.7071067811865476
        _sphere.applyMatrix4( sprite.matrixWorld )

        return this.intersectsSphere( _sphere )

    }*/

    fun intersectsSphere(sphere: Sphere): Boolean {
        val planes = this.planes
        val center = sphere.center
        val negRadius = -sphere.radius

        for (i in 0..6) {
            val distance = planes[i].distanceToPoint(center)

            if (distance < negRadius) {
                return false
            }
        }

        return true
    }

    fun intersectsBox(box: MutableBox3): Boolean {
        val planes = this.planes

        for (i in 0..6) {

            val plane = planes[i]

            // corner at max distance

            val vector = MutableVector3()
            vector.x = if (plane.normal.x > 0) {
                box.max.x
            } else box.min.x
            vector.y = if (plane.normal.y > 0) {
                box.max.y
            } else box.min.y
            vector.z = if (plane.normal.z > 0) {
                box.max.z
            } else box.min.z

            if (plane.distanceToPoint(vector) < 0) {
                return false
            }
        }

        return true
    }

    fun containsPoint(point: Vector3): Boolean {
        for (i in 0..6) {
            if (this.planes[i].distanceToPoint(point) < 0) {
                return false
            }
        }

        return true
    }

    fun clone(): Frustum {
        return Frustum().set(this)
    }
}
