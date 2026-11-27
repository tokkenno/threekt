package com.aitorgf.threekt.math

import com.aitorgf.threekt.cameras.Camera
import kotlin.math.*
import kotlin.random.Random
import kotlin.js.JsExport
import kotlin.js.JsName

/** A mutable three-component vector for 3D points, displacements, directions, and other triples. */
@JsExport
class Vector3(
    var x: Double = 0.0,
    var y: Double = 0.0,
    var z: Double = 0.0,
) : MutableVector<Vector3>() {
    override val self: Vector3 get() = this
    override val dimension: Int get() = 3

    /**
     * Sets the supplied components and returns this vector. When [z] is omitted, its current value is
     * retained.
     */
    fun set(x: Double, y: Double, z: Double = this.z): Vector3 {
        this.x = x
        this.y = y
        this.z = z
        return this
    }

    /** Copies the supplied values into this instance and returns it. */
    @JsName("copyFrom")
    fun copy(source: Vector3): Vector3 = set(source.x, source.y, source.z)

    /** Adds the given vector to this instance. Returns this instance. */
    fun add(vector: Vector3): Vector3 = set(x + vector.x, y + vector.y, z + vector.z)

    /** Subtracts the given vector from this instance. Returns this instance. */
    fun sub(vector: Vector3): Vector3 = set(x - vector.x, y - vector.y, z - vector.z)

    /** Multiplies the given scalar value with all components of this instance. Returns this instance. */
    override fun multiplyScalar(scalar: Double): Vector3 = set(x * scalar, y * scalar, z * scalar)

    /**
     * Multiplies this vector (with an implicit 1 in the 4th dimension) by m, and divides by perspective.
     * Returns this instance.
     */
    fun applyMatrix4(matrix: Matrix4): Vector3 {
        val elements = matrix.elements
        val inverseW = 1.0 / (elements[3] * x + elements[7] * y + elements[11] * z + elements[15])
        return set(
            (elements[0] * x + elements[4] * y + elements[8] * z + elements[12]) * inverseW,
            (elements[1] * x + elements[5] * y + elements[9] * z + elements[13]) * inverseW,
            (elements[2] * x + elements[6] * y + elements[10] * z + elements[14]) * inverseW,
        )
    }

    /** Sets the vector components to the same value. Returns this instance. */
    override fun setScalar(scalar: Double): Vector3 = set(scalar, scalar, scalar)

    /** Returns the component at the zero-based [index], in X, Y, Z order. */
    override operator fun get(index: Int): Double = when (index) {
        0 -> x
        1 -> y
        2 -> z
        else -> throw IndexOutOfBoundsException("Component index: $index")
    }

    /** Sets the component at the zero-based [index]. */
    override operator fun set(index: Int, value: Double) {
        when (index) {
            0 -> x = value
            1 -> y = value
            2 -> z = value
            else -> throw IndexOutOfBoundsException("Component index: $index")
        }
    }

    /** Returns a new instance with the same values. */
    override fun copy(): Vector3 = Vector3(x, y, z)

    /** Adds the given scalar value to all components of this instance. Returns this instance. */
    fun addScalar(scalar: Double): Vector3 = set(x + scalar, y + scalar, z + scalar)

    /** Adds the given vectors and stores the result in this instance. Returns this instance. */
    fun addVectors(a: Vector3, b: Vector3): Vector3 = set(a.x + b.x, a.y + b.y, a.z + b.z)

    /** Subtracts the given scalar value from all components of this instance. Returns this instance. */
    fun subScalar(scalar: Double): Vector3 = set(x - scalar, y - scalar, z - scalar)

    /** Subtracts the given vectors and stores the result in this instance. Returns this instance. */
    fun subVectors(a: Vector3, b: Vector3): Vector3 = set(a.x - b.x, a.y - b.y, a.z - b.z)

    /** Multiplies the given vector with this instance. Returns this instance. */
    fun multiply(vector: Vector3): Vector3 = set(x * vector.x, y * vector.y, z * vector.z)

    /** Divides this instance by the given vector. Returns this instance. */
    fun divide(vector: Vector3): Vector3 = set(x / vector.x, y / vector.y, z / vector.z)

    /** Adds the given vector scaled by the given factor to this instance. Returns this instance. */
    fun addScaledVector(vector: Vector3, scalar: Double): Vector3 = set(x + vector.x * scalar, y + vector.y * scalar, z + vector.z * scalar)

    /**
     * If this vector's x, y or z value is greater than the given vector's x, y or z value, replace that
     * value with the corresponding min value. Returns this instance.
     */
    fun min(vector: Vector3): Vector3 = set(min(x, vector.x), min(y, vector.y), min(z, vector.z))

    /**
     * If this vector's x, y or z value is less than the given vector's x, y or z value, replace that value
     * with the corresponding max value. Returns this instance.
     */
    fun max(vector: Vector3): Vector3 = set(max(x, vector.x), max(y, vector.y), max(z, vector.z))

    /**
     * If this vector's x, y or z value is greater than the max vector's x, y or z value, it is replaced by
     * the corresponding value. If this vector's x, y or z value is less than the min vector's x, y or z
     * value, it is replaced by the corresponding value. Returns this instance.
     */
    fun clamp(min: Vector3, max: Vector3): Vector3 = set(MathUtils.clamp(x, min.x, max.x), MathUtils.clamp(y, min.y, max.y), MathUtils.clamp(z, min.z, max.z))

    /**
     * If this vector's x, y or z values are greater than the max value, they are replaced by the max
     * value. If this vector's x, y or z values are less than the min value, they are replaced by the min
     * value. Returns this instance.
     */
    fun clampScalar(min: Double, max: Double): Vector3 = set(MathUtils.clamp(x, min, max), MathUtils.clamp(y, min, max), MathUtils.clamp(z, min, max))

    /**
     * The components of this vector are rounded down to the nearest integer value. Returns this instance.
     */
    fun floor(): Vector3 = set(floor(x), floor(y), floor(z))

    /** The components of this vector are rounded up to the nearest integer value. Returns this instance. */
    fun ceil(): Vector3 = set(ceil(x), ceil(y), ceil(z))

    /** Rounds each component to the nearest integer, with ties rounded to even. Returns this vector. */
    fun round(): Vector3 = set(round(x), round(y), round(z))

    /**
     * The components of this vector are rounded towards zero (up if negative, down if positive) to an
     * integer value. Returns this instance.
     */
    fun roundToZero(): Vector3 = set(truncate(x), truncate(y), truncate(z))

    /** Inverts this vector - i.e. sets x = -x, y = -y and z = -z. Returns this instance. */
    fun negate(): Vector3 = set(-x, -y, -z)

    /** Calculates the dot product of the given vector with this instance. */
    override fun dot(vector: Vector3): Double = x * vector.x + y * vector.y + z * vector.z

    /** Computes the Manhattan length of this vector. */
    fun manhattanLength(): Double = abs(x) + abs(y) + abs(z)

    /**
     * Linearly interpolates between the given vector and this instance, where alpha is the percent
     * distance along the line - alpha = 0 will be this vector, and alpha = 1 will be the given one.
     * Returns this instance.
     */
    fun lerp(vector: Vector3, alpha: Double): Vector3 = set(x + (vector.x - x) * alpha, y + (vector.y - y) * alpha, z + (vector.z - z) * alpha)

    /**
     * Linearly interpolates between the given vectors, where alpha is the percent distance along the line
     * - alpha = 0 will be first vector, and alpha = 1 will be the second one. The result is stored in this
     * instance. Returns this instance.
     */
    fun lerpVectors(a: Vector3, b: Vector3, alpha: Double): Vector3 = set(a.x + (b.x - a.x) * alpha, a.y + (b.y - a.y) * alpha, a.z + (b.z - a.z) * alpha)

    /** Returns `true` if this vector is equal with the given one. */
    override fun equals(other: Any?): Boolean = other is Vector3 && sameComponents(other)

    /** Returns a hash based on the current component values. */
    override fun hashCode(): Int = componentHash()

    /**
     * Sets each component to an independent sample in the interval `[0, 1)` using [random]. Returns this
     * vector.
     */
    @JsExport.Ignore
    fun random(random: Random = Random.Default): Vector3 = set(random.nextDouble(), random.nextDouble(), random.nextDouble())

    /** Computes the distance from the given vector to this instance. */
    fun distanceTo(vector: Vector3): Double = sqrt(distanceToSquared(vector))

    /**
     * Computes the squared distance from the given vector to this instance. If you are just comparing the
     * distance with another distance, you should compare the distance squared instead as it is slightly
     * more efficient to calculate.
     */
    fun distanceToSquared(vector: Vector3): Double = (x - vector.x).let { it * it } + (y - vector.y).let { it * it } + (z - vector.z).let { it * it }

    /** Computes the Manhattan distance from the given vector to this instance. */
    fun manhattanDistanceTo(vector: Vector3): Double = abs(x - vector.x) + abs(y - vector.y) + abs(z - vector.z)

    /** Multiplies the given vectors and stores the result in this instance. Returns this instance. */
    fun multiplyVectors(a: Vector3, b: Vector3): Vector3 = set(a.x * b.x, a.y * b.y, a.z * b.z)

    /** Calculates the cross product of the given vector with this instance. */
    fun cross(vector: Vector3): Vector3 = crossVectors(this, vector)

    /**
     * Calculates the cross product of the given vectors and stores the result in this instance. Returns
     * this instance.
     */
    fun crossVectors(a: Vector3, b: Vector3): Vector3 =
        set(a.y * b.z - a.z * b.y, a.z * b.x - a.x * b.z, a.x * b.y - a.y * b.x)

    /** Returns the angle between the given vector and this instance in radians. */
    fun angleTo(vector: Vector3): Double {
        val denominator = sqrt(lengthSq() * vector.lengthSq())
        return if (denominator == 0.0) PI / 2.0 else acos(MathUtils.clamp(dot(vector) / denominator, -1.0, 1.0))
    }

    /** Projects this vector onto the given one. Returns this instance. */
    fun projectOnVector(vector: Vector3): Vector3 {
        val denominator = vector.lengthSq()
        if (denominator == 0.0) return setScalar(0.0)
        val factor = dot(vector) / denominator
        return copy(vector).multiplyScalar(factor)
    }

    /**
     * Projects this vector onto a plane by subtracting this vector projected onto the plane's normal from
     * this vector. Returns this instance.
     */
    fun projectOnPlane(normal: Vector3): Vector3 = sub(copy().projectOnVector(normal))

    /** Reflects this vector off a plane orthogonal to the given normal vector. Returns this instance. */
    fun reflect(normal: Vector3): Vector3 = addScaledVector(normal, -2.0 * dot(normal))

    /** Multiplies this vector with the given 3x3 matrix. Returns this instance. */
    fun applyMatrix3(matrix: Matrix3): Vector3 {
        val e = matrix.elements
        return set(
            e[0] * x + e[3] * y + e[6] * z,
            e[1] * x + e[4] * y + e[7] * z,
            e[2] * x + e[5] * y + e[8] * z,
        )
    }

    /**
     * Multiplies this vector by the given normal matrix and normalizes the result. Returns this instance.
     */
    fun applyNormalMatrix(matrix: Matrix3): Vector3 = applyMatrix3(matrix).normalize()

    /**
     * Transforms this vector by the upper left 3x3 sub-matrix of the given 4x4 matrix, and normalizes the
     * result. Returns this instance.
     */
    fun transformDirection(matrix: Matrix4): Vector3 {
        val e = matrix.elements
        return set(
            e[0] * x + e[4] * y + e[8] * z,
            e[1] * x + e[5] * y + e[9] * z,
            e[2] * x + e[6] * y + e[10] * z,
        ).normalize()
    }

    /** Applies the given Quaternion to this vector. Returns this instance. */
    fun applyQuaternion(q: Quaternion): Vector3 {
        val tx = 2.0 * (q.y * z - q.z * y)
        val ty = 2.0 * (q.z * x - q.x * z)
        val tz = 2.0 * (q.x * y - q.y * x)
        return set(
            x + q.w * tx + q.y * tz - q.z * ty,
            y + q.w * ty + q.z * tx - q.x * tz,
            z + q.w * tz + q.x * ty - q.y * tx,
        )
    }

    /** Applies a rotation specified by an axis and an angle to this vector. Returns this instance. */
    fun applyAxisAngle(axis: Vector3, angle: Double): Vector3 =
        applyQuaternion(Quaternion().setFromAxisAngle(axis, angle))

    /** Applies the given Euler rotation to this vector. Returns this instance. */
    fun applyEuler(euler: Euler): Vector3 = applyQuaternion(Quaternion().setFromEuler(euler))

    /** Sets the vector components from the given Euler angles. Returns this instance. */
    fun setFromEuler(euler: Euler): Vector3 = set(euler.x, euler.y, euler.z)

    /** Sets the vector components from the RGB components of the given color. Returns this instance. */
    @JsExport.Ignore
    fun setFromColor(color: Color): Vector3 = set(color.r, color.g, color.b)

    /** Sets the vector components from the given spherical coordinates. Returns this instance. */
    @JsExport.Ignore
    fun setFromSpherical(spherical: Spherical): Vector3 =
        setFromSphericalCoords(spherical.radius, spherical.phi, spherical.theta)

    /** Sets the vector components from the given spherical coordinates. Returns this instance. */
    fun setFromSphericalCoords(radius: Double, phi: Double, theta: Double): Vector3 {
        val horizontalRadius = sin(phi) * radius
        return set(horizontalRadius * sin(theta), cos(phi) * radius, horizontalRadius * cos(theta))
    }

    /** Sets the vector components from the given cylindrical coordinates. Returns this instance. */
    @JsExport.Ignore
    fun setFromCylindrical(cylindrical: Cylindrical): Vector3 =
        setFromCylindricalCoords(cylindrical.radius, cylindrical.theta, cylindrical.y)

    /** Sets the vector components from the given cylindrical coordinates. Returns this instance. */
    fun setFromCylindricalCoords(radius: Double, theta: Double, y: Double): Vector3 =
        set(radius * sin(theta), y, radius * cos(theta))

    /**
     * Sets the vector components to the position elements of the given transformation matrix. Returns this
     * instance.
     */
    fun setFromMatrixPosition(matrix: Matrix4): Vector3 = fromArray(matrix.elements, 12)

    /** Sets the vector components from the specified matrix column. Returns this instance. */
    fun setFromMatrixColumn(matrix: Matrix4, index: Int): Vector3 = fromArray(matrix.elements, index * 4)

    /** Sets the vector components from the specified matrix column. Returns this instance. */
    fun setFromMatrix3Column(matrix: Matrix3, index: Int): Vector3 = fromArray(matrix.elements, index * 3)

    /**
     * Sets the vector components to the scale elements of the given transformation matrix. Returns this
     * instance.
     */
    fun setFromMatrixScale(matrix: Matrix4): Vector3 {
        val sx = setFromMatrixColumn(matrix, 0).length()
        val sy = setFromMatrixColumn(matrix, 1).length()
        val sz = setFromMatrixColumn(matrix, 2).length()
        return set(sx, sy, sz)
    }

    /**
     * Projects this vector from world space into the camera's normalized device coordinate (NDC) space.
     * Returns this instance.
     */
    fun project(camera: Camera): Vector3 =
        applyMatrix4(camera.matrixWorldInverse).applyMatrix4(camera.projectionMatrix)

    /**
     * Unprojects this vector from the camera's normalized device coordinate (NDC) space into world space.
     * Returns this instance.
     */
    fun unproject(camera: Camera): Vector3 =
        applyMatrix4(camera.projectionMatrixInverse).applyMatrix4(camera.matrixWorld)

    /** Sets this vector to a uniformly distributed unit direction using [random] and returns it. */
    @JsExport.Ignore
    fun randomDirection(random: Random = Random.Default): Vector3 {
        val y = 2.0 * random.nextDouble() - 1.0
        val angle = 2.0 * PI * random.nextDouble()
        val radius = sqrt(1.0 - y * y)
        return set(radius * cos(angle), y, radius * sin(angle))
    }
}
