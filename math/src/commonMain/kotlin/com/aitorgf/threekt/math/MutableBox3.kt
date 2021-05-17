package com.aitorgf.threekt.math

import com.aitorgf.threekt.types.ComplexBuffer
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport
import kotlin.js.JsName

@ExperimentalJsExport
@JsExport
class MutableBox3(
    min: Vector3 = Vector3(
        Double.POSITIVE_INFINITY,
        Double.POSITIVE_INFINITY,
        Double.POSITIVE_INFINITY
    ),
    max: Vector3 = Vector3(
        Double.NEGATIVE_INFINITY,
        Double.NEGATIVE_INFINITY,
        Double.NEGATIVE_INFINITY
    )
) : Box3(min, max), MutableBox {
    override val min: MutableVector3 = min.toMutable()
    override val max: MutableVector3 = max.toMutable()

    fun set(min: Vector3, max: Vector3): MutableBox3 {
        this.min.set(min)
        this.max.set(max)
        return this
    }

    @JsName("setFromArray")
    fun set(array: DoubleArray): MutableBox3 {
        var minX = Double.POSITIVE_INFINITY
        var minY = Double.POSITIVE_INFINITY
        var minZ = Double.POSITIVE_INFINITY

        var maxX = Double.NEGATIVE_INFINITY
        var maxY = Double.NEGATIVE_INFINITY
        var maxZ = Double.NEGATIVE_INFINITY

        for (i in 0..array.size step 3) {
            val x = array[i]
            val y = array[i + 1]
            val z = array[i + 2]

            if (x < minX) minX = x
            if (y < minY) minY = y
            if (z < minZ) minZ = z

            if (x > maxX) maxX = x
            if (y > maxY) maxY = y
            if (z > maxZ) maxZ = z
        }

        this.min.set(minX, minY, minZ)
        this.max.set(maxX, maxY, maxZ)

        return this
    }

    @JsName("setBox3")
    fun set(box: MutableBox3): MutableBox3 {
        this.min.set(box.min)
        this.max.set(box.max)
        return this
    }

    override fun set(buffer: ComplexBuffer): MutableBox {
        var minX = Double.POSITIVE_INFINITY
        var minY = Double.POSITIVE_INFINITY
        var minZ = Double.POSITIVE_INFINITY

        var maxX = Double.NEGATIVE_INFINITY
        var maxY = Double.NEGATIVE_INFINITY
        var maxZ = Double.NEGATIVE_INFINITY

        for (i in 0..buffer.count - 1) {
            val x = buffer.getComplex(i, 0);
            val y = buffer.getComplex(i, 1);
            val z = buffer.getComplex(i, 2);

            if (x < minX) minX = x;
            if (y < minY) minY = y;
            if (z < minZ) minZ = z;

            if (x > maxX) maxX = x;
            if (y > maxY) maxY = y;
            if (z > maxZ) maxZ = z;
        }

        this.min.set(minX, minY, minZ);
        this.max.set(maxX, maxY, maxZ);

        return this;
    }

    fun setFromPoints(vararg points: Vector3): MutableBox3 {
        this.makeEmpty()
        points.forEach { this.expandByPoint(it) }
        return this
    }

    fun setFromCenterAndSize(center: Vector3, size: Vector3): MutableBox3 {
        val halfSize = MutableVector3().set(size).multiply(0.5)
        this.min.set(center).sub(halfSize)
        this.max.set(center).add(halfSize)
        return this
    }

    /** TODO: Move to external inline function
    fun setFromObject(obj: Object3): Box3 {
    this.makeEmpty()
    return this.expandByObject(obj)
    }
     */

    override fun clone(): MutableBox3 {
        return MutableBox3(this.min.clone(), this.max.clone())
    }

    fun makeEmpty(): MutableBox3 {
        this.min.x = Double.POSITIVE_INFINITY
        this.min.y = Double.POSITIVE_INFINITY
        this.min.z = Double.POSITIVE_INFINITY
        this.max.x = Double.NEGATIVE_INFINITY
        this.max.y = Double.NEGATIVE_INFINITY
        this.max.z = Double.NEGATIVE_INFINITY
        return this
    }

    fun expandByPoint(point: Vector3): MutableBox3 {
        this.min.min(point)
        this.max.max(point)
        return this
    }

    fun expandByVector(vector: Vector3): MutableBox3 {
        this.min.sub(vector)
        this.max.add(vector)
        return this
    }

    fun expandByScalar(scalar: Double): MutableBox3 {
        this.min.add(-scalar)
        this.max.add(scalar)
        return this
    }

    /** TODO: Move to external inline function
    fun expandByObject(obj: Object3): Box3 {
    // Computes the world-axis-aligned bounding box of an object (including its children),
    // accounting for both the object's, and children's, world transforms
    obj.updateWorldMatrix(false, false)

    if (obj is GeometricObject) {
    if (obj.geometry.boundingBox == null) {
    obj.geometry.computeBoundingBox()
    }

    val box = Box3().set(obj.geometry.boundingBox)
    box.applyMatrix4(obj.matrixWorld)

    this.union(box)
    }

    for (i in 0..obj.children.size) {
    this.expandByObject(obj.children[i])
    }

    return this
    }*/

    fun intersect(box: MutableBox3): MutableBox3 {
        this.min.max(box.min)
        this.max.min(box.max)
        // ensure that if there is no overlap, the result is fully empty, not slightly empty with non-inf/+inf values that will cause subsequence intersects to erroneously return valid values.
        if (this.empty) this.makeEmpty()
        return this
    }

    fun union(box: MutableBox3): MutableBox3 {
        this.min.min(box.min)
        this.max.max(box.max)
        return this
    }

    @JsName("applyMatrix4")
    fun apply(matrix: Matrix4): MutableBox3 {
        // transform of empty box is an empty box.
        if (this.empty) return this

        // NOTE: I am using a binary pattern to specify all 2^3 combinations below
        this.setFromPoints(
            MutableVector3(this.min.x, this.min.y, this.min.z).apply(matrix), // 000
            MutableVector3(this.min.x, this.min.y, this.max.z).apply(matrix), // 001
            MutableVector3(this.min.x, this.max.y, this.min.z).apply(matrix), // 010
            MutableVector3(this.min.x, this.max.y, this.max.z).apply(matrix), // 011
            MutableVector3(this.max.x, this.min.y, this.min.z).apply(matrix), // 100
            MutableVector3(this.max.x, this.min.y, this.max.z).apply(matrix), // 101
            MutableVector3(this.max.x, this.max.y, this.min.z).apply(matrix), // 110
            MutableVector3(this.max.x, this.max.y, this.max.z).apply(matrix) // 111
        )

        return this
    }

    fun translate(offset: Vector3): MutableBox3 {
        this.min.add(offset)
        this.max.add(offset)
        return this
    }
}
