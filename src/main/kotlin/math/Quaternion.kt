package math

import utils.events.EventEmitter
import utils.events.Observable

@ExperimentalJsExport
@JsExport
class Quaternion {
    private val onChangeEmitter = EventEmitter()

    val onChange: Observable
        get() = onChangeEmitter

    var x: Double = 0.0
        set(value) {
            if (value != x) {
                field = value
                onChangeEmitter.emit(this)
            }
        }

    var y: Double = 0.0
        set(value) {
            if (value != y) {
                field = value
                onChangeEmitter.emit(this)
            }
        }

    var z: Double = 0.0
        set(value) {
            if (value != z) {
                field = value
                onChangeEmitter.emit(this)
            }
        }

    var w: Double = 0.0
        set(value) {
            if (value != w) {
                field = value
                onChangeEmitter.emit(this)
            }
        }

    fun set(x: Double, y: Double, z: Double, w: Double): Quaternion {
        this.x = x
        this.y = y
        this.z = z
        this.w = w

        this.onChangeEmitter.emit(this)
        return this
    }

    fun setFromAxisAngle(axis: Vector3, angle: Radian): Quaternion {
        // assumes axis is normalized

        val halfAngle = angle.value / 2
        val s = kotlin.math.sin(halfAngle)

        this.x = axis.x * s
        this.y = axis.y * s
        this.z = axis.z * s
        this.w = kotlin.math.cos(halfAngle)

        this.onChangeEmitter.emit(this)
        return this
    }

    fun premultiply(q: Quaternion): Quaternion {
        return this.multiplyQuaternionsAndSet(q, this)
    }

    fun multiplyQuaternionsAndSet(a: Quaternion, b: Quaternion): Quaternion {
        this.x = a.x * b.w + a.w * b.x + a.y * b.z - a.z * b.y
        this.y = a.y * b.w + a.w * b.y + a.z * b.x - a.x * b.z
        this.z = a.z * b.w + a.w * b.z + a.x * b.y - a.y * b.x
        this.w = a.w * b.w - a.x * b.x - a.y * b.y - a.z * b.z

        this.onChangeEmitter.emit(this)
        return this
    }

    fun setFromEuler(euler: Euler, update: Boolean = false): Quaternion {
        val order = euler.order;

        val c1 = kotlin.math.cos(euler.x / 2)
        val c2 = kotlin.math.cos(euler.y / 2)
        val c3 = kotlin.math.cos(euler.z / 2)

        val s1 = kotlin.math.sin(euler.x / 2)
        val s2 = kotlin.math.sin(euler.y / 2)
        val s3 = kotlin.math.sin(euler.z / 2)

        when (order) {
            AxisOrder.XYZ -> {
                this.x = s1 * c2 * c3 + c1 * s2 * s3
                this.y = c1 * s2 * c3 - s1 * c2 * s3
                this.z = c1 * c2 * s3 + s1 * s2 * c3
                this.w = c1 * c2 * c3 - s1 * s2 * s3
            }
            AxisOrder.YXZ -> {
                this.x = s1 * c2 * c3 + c1 * s2 * s3
                this.y = c1 * s2 * c3 - s1 * c2 * s3
                this.z = c1 * c2 * s3 - s1 * s2 * c3
                this.w = c1 * c2 * c3 + s1 * s2 * s3
            }
            AxisOrder.ZXY -> {
                this.x = s1 * c2 * c3 - c1 * s2 * s3
                this.y = c1 * s2 * c3 + s1 * c2 * s3
                this.z = c1 * c2 * s3 + s1 * s2 * c3
                this.w = c1 * c2 * c3 - s1 * s2 * s3
            }
            AxisOrder.ZYX -> {
                this.x = s1 * c2 * c3 - c1 * s2 * s3
                this.y = c1 * s2 * c3 + s1 * c2 * s3
                this.z = c1 * c2 * s3 - s1 * s2 * c3
                this.w = c1 * c2 * c3 + s1 * s2 * s3
            }
            AxisOrder.YZX -> {
                this.x = s1 * c2 * c3 + c1 * s2 * s3
                this.y = c1 * s2 * c3 + s1 * c2 * s3
                this.z = c1 * c2 * s3 - s1 * s2 * c3
                this.w = c1 * c2 * c3 - s1 * s2 * s3
            }
            AxisOrder.XZY -> {
                this.x = s1 * c2 * c3 - c1 * s2 * s3
                this.y = c1 * s2 * c3 - s1 * c2 * s3
                this.z = c1 * c2 * s3 + s1 * s2 * c3
                this.w = c1 * c2 * c3 + s1 * s2 * s3
            }
        }

        if (update) {
            this.onChangeEmitter.emit()
        }

        return this
    }

    fun setFromRotationMatrix(m: MutableMatrix4): Quaternion {
        val m11 = m.elements[ 0 ]; val m12 = m.elements[ 4 ]; val m13 = m.elements[ 8 ];
        val m21 = m.elements[ 1 ]; val m22 = m.elements[ 5 ]; val m23 = m.elements[ 9 ];
        val m31 = m.elements[ 2 ]; val m32 = m.elements[ 6 ]; val m33 = m.elements[ 10 ];

        val trace = m11 + m22 + m33;

        if ( trace > 0 ) {
            val s = 0.5 / kotlin.math.sqrt( trace + 1.0 );

            this.w = 0.25 / s;
            this.x = ( m32 - m23 ) * s;
            this.y = ( m13 - m31 ) * s;
            this.z = ( m21 - m12 ) * s;
        } else if ( m11 > m22 && m11 > m33 ) {
            val s = 2.0 * kotlin.math.sqrt( 1.0 + m11 - m22 - m33 );

            this.w = ( m32 - m23 ) / s;
            this.x = 0.25 * s;
            this.y = ( m12 + m21 ) / s;
            this.z = ( m13 + m31 ) / s;
        } else if ( m22 > m33 ) {
            val s = 2.0 * kotlin.math.sqrt( 1.0 + m22 - m11 - m33 );

            this.w = ( m13 - m31 ) / s;
            this.x = ( m12 + m21 ) / s;
            this.y = 0.25 * s;
            this.z = ( m23 + m32 ) / s;
        } else {
            val s = 2.0 * kotlin.math.sqrt( 1.0 + m33 - m11 - m22 );

            this.w = ( m21 - m12 ) / s;
            this.x = ( m13 + m31 ) / s;
            this.y = ( m23 + m32 ) / s;
            this.z = 0.25 * s;
        }

        this.onChangeEmitter.emit()
        return this
    }
}
