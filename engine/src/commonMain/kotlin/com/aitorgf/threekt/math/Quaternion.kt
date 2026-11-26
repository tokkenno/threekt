package com.aitorgf.threekt.math

import com.aitorgf.threekt.core.BufferAttribute
import kotlin.math.*
import kotlin.random.Random
import kotlin.js.JsExport
import kotlin.js.JsName

/**
 * A mutable quaternion with components [x], [y], [z], and [w]. Rotation operations expect unit
 * quaternions. The default value represents the identity rotation.
 *
 * Every change runs the callback registered with [onChange], which [com.aitorgf.threekt.core.Object3D]
 * uses to keep its `rotation` in sync. [equals] compares the components, as the Three.js `equals()`.
 */
@JsExport
class Quaternion(x: Double = 0.0, y: Double = 0.0, z: Double = 0.0, w: Double = 1.0) {
    private var xValue = x
    private var yValue = y
    private var zValue = z
    private var wValue = w
    private var onChangeCallback: () -> Unit = {}

    /** The x component. */
    var x: Double
        get() = xValue
        set(value) {
            xValue = value
            onChangeCallback()
        }

    /** The y component. */
    var y: Double
        get() = yValue
        set(value) {
            yValue = value
            onChangeCallback()
        }

    /** The z component. */
    var z: Double
        get() = zValue
        set(value) {
            zValue = value
            onChangeCallback()
        }

    /** The w component. */
    var w: Double
        get() = wValue
        set(value) {
            wValue = value
            onChangeCallback()
        }

    /** Sets the quaternion components. Returns this instance. */
    fun set(x: Double, y: Double, z: Double, w: Double): Quaternion {
        setSilently(x, y, z, w)
        onChangeCallback()
        return this
    }

    private fun setSilently(x: Double, y: Double, z: Double, w: Double) {
        xValue = x
        yValue = y
        zValue = z
        wValue = w
    }

    /** Returns a new quaternion with the same components, as the Three.js `clone()`. */
    fun copy(): Quaternion = Quaternion(xValue, yValue, zValue, wValue)

    /** Sets the rotation about a unit [axis] by [angle] radians. */
    fun setFromAxisAngle(axis: Vector3, angle: Double): Quaternion {
        val halfAngle = angle / 2.0
        val sine = sin(halfAngle)
        return set(axis.x * sine, axis.y * sine, axis.z * sine, cos(halfAngle))
    }

    /** Copies the supplied values into this instance and returns it. */
    @JsName("copyFrom")
    fun copy(other: Quaternion): Quaternion = set(other.x, other.y, other.z, other.w)

    /** Sets this quaternion to the identity rotation and returns it. */
    fun identity(): Quaternion = set(0.0, 0.0, 0.0, 1.0)

    /**
     * Inverts this quaternion via [Quaternion.conjugate]. The quaternion is assumed to have unit length.
     * Returns this instance.
     */
    fun invert(): Quaternion = conjugate()

    /**
     * Returns the rotational conjugate of this quaternion. The conjugate of a quaternion represents the
     * same rotation in the opposite direction about the rotational axis. Returns this instance.
     */
    fun conjugate(): Quaternion = set(-x, -y, -z, w)

    /** Calculates the dot product of this quaternion and the given one. */
    fun dot(other: Quaternion): Double = x * other.x + y * other.y + z * other.z + w * other.w

    /**
     * Computes the squared Euclidean length (straight-line length) of this quaternion, considered as a 4
     * dimensional vector. This can be useful if you are comparing the lengths of two quaternions, as this
     * is a slightly more efficient calculation than [Quaternion.length].
     */
    fun lengthSq(): Double = dot(this)

    /**
     * Computes the Euclidean length (straight-line length) of this quaternion, considered as a 4
     * dimensional vector.
     */
    fun length(): Double = sqrt(lengthSq())

    /**
     * Scales this quaternion to unit length and returns it. A zero quaternion is replaced with the
     * identity.
     */
    fun normalize(): Quaternion {
        val length = length()
        if (length == 0.0) return identity()
        val scale = 1.0 / length
        return set(x * scale, y * scale, z * scale, w * scale)
    }

    /** Multiplies this quaternion by the given one. Returns this instance. */
    fun multiply(other: Quaternion): Quaternion = multiplyQuaternions(this, other)

    /** Pre-multiplies this quaternion by the given one. Returns this instance. */
    fun premultiply(other: Quaternion): Quaternion = multiplyQuaternions(other, this)

    /** Multiplies the given quaternions and stores the result in this instance. Returns this instance. */
    fun multiplyQuaternions(a: Quaternion, b: Quaternion): Quaternion = set(
        a.x * b.w + a.w * b.x + a.y * b.z - a.z * b.y,
        a.y * b.w + a.w * b.y + a.z * b.x - a.x * b.z,
        a.z * b.w + a.w * b.z + a.x * b.y - a.y * b.x,
        a.w * b.w - a.x * b.x - a.y * b.y - a.z * b.z,
    )

    /**
     * Sets this quaternion from the rotation specified by the given Euler angles. The [onChange] callback
     * runs only when [update] is true. Returns this instance.
     */
    fun setFromEuler(euler: Euler, update: Boolean = true): Quaternion {
        val cx = cos(euler.x / 2)
        val cy = cos(euler.y / 2)
        val cz = cos(euler.z / 2)
        val sx = sin(euler.x / 2)
        val sy = sin(euler.y / 2)
        val sz = sin(euler.z / 2)
        val x = sx * cy * cz
        val y = cx * sy * cz
        val z = cx * cy * sz
        val w = cx * cy * cz
        val crossX = cx * sy * sz
        val crossY = sx * cy * sz
        val crossZ = sx * sy * cz
        val crossW = sx * sy * sz
        when (euler.order) {
            Euler.Order.XYZ -> setSilently(x + crossX, y - crossY, z + crossZ, w - crossW)
            Euler.Order.YXZ -> setSilently(x + crossX, y - crossY, z - crossZ, w + crossW)
            Euler.Order.ZXY -> setSilently(x - crossX, y + crossY, z + crossZ, w - crossW)
            Euler.Order.ZYX -> setSilently(x - crossX, y + crossY, z - crossZ, w + crossW)
            Euler.Order.YZX -> setSilently(x + crossX, y + crossY, z - crossZ, w - crossW)
            Euler.Order.XZY -> setSilently(x - crossX, y - crossY, z + crossZ, w + crossW)
        }
        if (update) onChangeCallback()
        return this
    }

    /** Sets this quaternion from the unscaled upper-left 3×3 rotation in [matrix] and returns it. */
    fun setFromRotationMatrix(matrix: Matrix4): Quaternion {
        val m = matrix.elements
        val trace = m[0] + m[5] + m[10]
        return when {
            trace > 0.0 -> {
                val s = 0.5 / sqrt(trace + 1.0)
                set((m[6] - m[9]) * s, (m[8] - m[2]) * s, (m[1] - m[4]) * s, 0.25 / s)
            }
            m[0] > m[5] && m[0] > m[10] -> {
                val s = 2.0 * sqrt(1.0 + m[0] - m[5] - m[10])
                set(0.25 * s, (m[4] + m[1]) / s, (m[8] + m[2]) / s, (m[6] - m[9]) / s)
            }
            m[5] > m[10] -> {
                val s = 2.0 * sqrt(1.0 + m[5] - m[0] - m[10])
                set((m[4] + m[1]) / s, 0.25 * s, (m[9] + m[6]) / s, (m[8] - m[2]) / s)
            }
            else -> {
                val s = 2.0 * sqrt(1.0 + m[10] - m[0] - m[5])
                set((m[8] + m[2]) / s, (m[9] + m[6]) / s, 0.25 * s, (m[1] - m[4]) / s)
            }
        }
    }

    /**
     * Sets this quaternion to the rotation required to rotate the direction vector `vFrom` to the
     * direction vector `vTo`. Returns this instance.
     */
    fun setFromUnitVectors(from: Vector3, to: Vector3): Quaternion {
        val real = from.dot(to) + 1.0
        val axis = if (real < 1e-8) {
            if (abs(from.x) > abs(from.z)) Vector3(-from.y, from.x, 0.0) else Vector3(0.0, -from.z, from.y)
        } else Vector3().crossVectors(from, to)
        return set(axis.x, axis.y, axis.z, if (real < 1e-8) 0.0 else real).normalize()
    }

    /** Returns the angle between this quaternion and the given one in radians. */
    fun angleTo(other: Quaternion): Double = 2.0 * acos(abs(MathUtils.clamp(dot(other), -1.0, 1.0)))

    /**
     * Rotates this quaternion by a given angular step to the given quaternion. The method ensures that the
     * final quaternion will not overshoot `q`. Returns this instance.
     */
    fun rotateTowards(other: Quaternion, step: Double): Quaternion {
        val angle = angleTo(other)
        return if (angle == 0.0) this else slerp(other, min(1.0, step / angle))
    }

    /**
     * Performs a spherical linear interpolation between this quaternion and the target quaternion. Returns
     * this instance.
     */
    fun slerp(other: Quaternion, t: Double): Quaternion = slerpQuaternions(this, other, t)

    /**
     * Performs a spherical linear interpolation between the given quaternions and stores the result in
     * this quaternion. Returns this instance.
     */
    fun slerpQuaternions(a: Quaternion, b: Quaternion, t: Double): Quaternion {
        val dot = a.dot(b)
        val direction = if (dot < 0.0) -1.0 else 1.0
        val cosine = abs(dot)
        if (cosine < 0.9995) {
            val angle = acos(cosine)
            val sine = sin(angle)
            val wa = sin((1.0 - t) * angle) / sine
            val wb = direction * sin(t * angle) / sine
            return set(a.x * wa + b.x * wb, a.y * wa + b.y * wb, a.z * wa + b.z * wb, a.w * wa + b.w * wb)
        }
        return set(
            MathUtils.lerp(a.x, direction * b.x, t), MathUtils.lerp(a.y, direction * b.y, t),
            MathUtils.lerp(a.z, direction * b.z, t), MathUtils.lerp(a.w, direction * b.w, t),
        ).normalize()
    }

    /** Sets this quaternion to a uniformly sampled unit rotation using [random] and returns it. */
    @JsExport.Ignore
    fun random(random: Random = Random.Default): Quaternion {
        val ratio = random.nextDouble()
        val a = random.nextDouble() * 2.0 * PI
        val b = random.nextDouble() * 2.0 * PI
        return set(sqrt(1.0 - ratio) * sin(a), sqrt(1.0 - ratio) * cos(a), sqrt(ratio) * sin(b), sqrt(ratio) * cos(b))
    }

    /** Sets this quaternion's components from the given array. Returns this instance. */
    fun fromArray(array: DoubleArray, offset: Int = 0): Quaternion =
        set(array[offset], array[offset + 1], array[offset + 2], array[offset + 3])

    /**
     * Writes the components of this quaternion to the given array. If no array is provided, the method
     * returns a new instance.
     */
    fun toArray(array: DoubleArray = DoubleArray(4), offset: Int = 0): DoubleArray {
        require(offset >= 0 && offset <= array.size - 4)
        array[offset] = x
        array[offset + 1] = y
        array[offset + 2] = z
        array[offset + 3] = w
        return array
    }

    /**
     * Registers [callback] to run after every change of the components, replacing the previous callback.
     * Three.js calls this internal method `_onChange`. Returns this instance.
     */
    internal fun onChange(callback: () -> Unit): Quaternion {
        onChangeCallback = callback
        return this
    }

    override fun equals(other: Any?): Boolean =
        other is Quaternion && other.xValue == xValue && other.yValue == yValue && other.zValue == zValue && other.wValue == wValue

    override fun hashCode(): Int = listOf(xValue, yValue, zValue, wValue).hashCode()

    override fun toString(): String = "Quaternion(x=$xValue, y=$yValue, z=$zValue, w=$wValue)"

    /** Sets the components of this quaternion from the given buffer attribute. Returns this instance. */
    fun fromBufferAttribute(attribute: BufferAttribute, index: Int): Quaternion {
        return set(attribute.getX(index), attribute.getY(index), attribute.getZ(index), attribute.getW(index))
    }


    companion object {

        /**
         * Interpolates between two quaternions via SLERP. This implementation assumes the quaternion data
         * are managed in flat arrays.
         */
        fun slerpFlat(dst: DoubleArray, dstOffset: Int, src0: DoubleArray, srcOffset0: Int, src1: DoubleArray, srcOffset1: Int, t: Double) {
            val start = Quaternion().fromArray(src0, srcOffset0)
            val end = Quaternion().fromArray(src1, srcOffset1)
            if (start != end) start.slerp(end, t)
            start.toArray(dst, dstOffset)
        }

        /**
         * Multiplies two quaternions. This implementation assumes the quaternion data are managed in flat
         * arrays.
         */
        fun multiplyQuaternionsFlat(dst: DoubleArray, dstOffset: Int, src0: DoubleArray, srcOffset0: Int, src1: DoubleArray, srcOffset1: Int): DoubleArray =
            Quaternion().fromArray(src0, srcOffset0).multiply(Quaternion().fromArray(src1, srcOffset1)).toArray(dst, dstOffset)
    }
}
