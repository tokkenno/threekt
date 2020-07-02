package math

import utils.events.EventEmitter
import utils.events.Observable

class Quaternion {
    private val onChangeEmitter = EventEmitter<Quaternion>()

    val onChange: Observable<Quaternion>
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
}