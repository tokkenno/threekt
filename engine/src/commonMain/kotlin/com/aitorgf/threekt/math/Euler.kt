package com.aitorgf.threekt.math

import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.js.JsExport
import kotlin.js.JsName

/**
 * A mutable rotation represented by three axis angles in radians applied in [order].
 *
 * Every change runs the callback registered with [onChange], which [com.aitorgf.threekt.core.Object3D]
 * uses to keep its `quaternion` in sync. [equals] compares the angles and the order, as the Three.js
 * `equals()`.
 */
@JsExport
class Euler(x: Double = 0.0, y: Double = 0.0, z: Double = 0.0, order: Order = DEFAULT_ORDER) {
    private var xValue = x
    private var yValue = y
    private var zValue = z
    private var orderValue = order
    private var onChangeCallback: () -> Unit = {}

    /** The angle around the X axis, in radians. */
    var x: Double
        get() = xValue
        set(value) {
            xValue = value
            onChangeCallback()
        }

    /** The angle around the Y axis, in radians. */
    var y: Double
        get() = yValue
        set(value) {
            yValue = value
            onChangeCallback()
        }

    /** The angle around the Z axis, in radians. */
    var z: Double
        get() = zValue
        set(value) {
            zValue = value
            onChangeCallback()
        }

    /** The order in which the rotations are applied. */
    var order: Order
        get() = orderValue
        set(value) {
            orderValue = value
            onChangeCallback()
        }

    /** Sets the Euler components. Returns this instance. */
    fun set(x: Double, y: Double, z: Double, order: Order = this.order): Euler {
        xValue = x
        yValue = y
        zValue = z
        orderValue = order
        onChangeCallback()
        return this
    }

    /** Returns a new Euler with the same angles and order, as the Three.js `clone()`. */
    fun copy(): Euler = Euler(xValue, yValue, zValue, orderValue)

    /** Copies the supplied values into this instance and returns it. */
    @JsName("copyFrom")
    fun copy(other: Euler): Euler = set(other.x, other.y, other.z, other.order)

    /**
     * Sets the angles of this Euler instance from a pure rotation matrix. The [onChange] callback runs only
     * when [update] is true. Returns this instance.
     */
    fun setFromRotationMatrix(matrix: Matrix4, order: Order = this.order, update: Boolean = true): Euler {
        val e = matrix.elements
        val m11 = e[0]
        val m12 = e[4]
        val m13 = e[8]
        val m21 = e[1]
        val m22 = e[5]
        val m23 = e[9]
        val m31 = e[2]
        val m32 = e[6]
        val m33 = e[10]
        val limit = 0.9999999
        fun angle(component: Double) = asin(MathUtils.clamp(component, -1.0, 1.0))

        val (x, y, z) = when (order) {
            Order.XYZ -> if (abs(m13) < limit) Triple(atan2(-m23, m33), angle(m13), atan2(-m12, m11))
                else Triple(atan2(m32, m22), angle(m13), 0.0)
            Order.YXZ -> if (abs(m23) < limit) Triple(angle(-m23), atan2(m13, m33), atan2(m21, m22))
                else Triple(angle(-m23), atan2(-m31, m11), 0.0)
            Order.ZXY -> if (abs(m32) < limit) Triple(angle(m32), atan2(-m31, m33), atan2(-m12, m22))
                else Triple(angle(m32), 0.0, atan2(m21, m11))
            Order.ZYX -> if (abs(m31) < limit) Triple(atan2(m32, m33), angle(-m31), atan2(m21, m11))
                else Triple(0.0, angle(-m31), atan2(-m12, m22))
            Order.YZX -> if (abs(m21) < limit) Triple(atan2(-m23, m22), atan2(-m31, m11), angle(m21))
                else Triple(0.0, atan2(m13, m33), angle(m21))
            Order.XZY -> if (abs(m12) < limit) Triple(atan2(m32, m22), atan2(m13, m11), angle(-m12))
                else Triple(atan2(-m23, m33), 0.0, angle(-m12))
        }
        xValue = x
        yValue = y
        zValue = z
        orderValue = order
        if (update) onChangeCallback()
        return this
    }

    /**
     * Sets the angles of this Euler instance from a normalized quaternion. The [onChange] callback runs only
     * when [update] is true. Returns this instance.
     */
    fun setFromQuaternion(quaternion: Quaternion, order: Order = this.order, update: Boolean = true): Euler =
        setFromRotationMatrix(Matrix4().makeRotationFromQuaternion(quaternion), order, update)

    /** Sets the angles of this Euler instance from the given vector. Returns this instance. */
    fun setFromVector3(vector: Vector3, order: Order = this.order): Euler = set(vector.x, vector.y, vector.z, order)

    /**
     * Resets the euler angle with a new order by creating a quaternion from this euler angle and then
     * setting this euler angle with the quaternion and the new order. Warning: This discards revolution
     * information. Returns this instance.
     */
    fun reorder(order: Order): Euler = setFromQuaternion(Quaternion().setFromEuler(this), order)

    /**
     * Reads three angles in radians from [angles] starting at [offset] and sets [order]. Returns this
     * rotation.
     */
    fun fromArray(angles: DoubleArray, order: Order = this.order, offset: Int = 0): Euler =
        set(angles[offset], angles[offset + 1], angles[offset + 2], order)

    /** Writes the three angles in radians to [array] starting at [offset] and returns that array. */
    fun toArray(array: DoubleArray = DoubleArray(3), offset: Int = 0): DoubleArray {
        array[offset] = x
        array[offset + 1] = y
        array[offset + 2] = z
        return array
    }

    /**
     * Registers [callback] to run after every change of the angles or the order, replacing the previous
     * callback. Three.js calls this internal method `_onChange`. Returns this instance.
     */
    internal fun onChange(callback: () -> Unit): Euler {
        onChangeCallback = callback
        return this
    }

    override fun equals(other: Any?): Boolean =
        other is Euler && other.xValue == xValue && other.yValue == yValue && other.zValue == zValue &&
            other.orderValue == orderValue

    override fun hashCode(): Int = listOf(xValue, yValue, zValue, orderValue).hashCode()

    override fun toString(): String = "Euler(x=$xValue, y=$yValue, z=$zValue, order=$orderValue)"

    /** Order of the three distinct axes in an Euler rotation. */
    enum class Order { XYZ, YXZ, ZXY, ZYX, YZX, XZY }

    companion object {
        /** The order of new Euler instances, `XYZ`. */
        val DEFAULT_ORDER = Order.XYZ
    }
}
