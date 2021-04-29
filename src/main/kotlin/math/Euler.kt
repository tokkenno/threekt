package math

import utils.events.EventEmitter
import utils.events.Observable

@ExperimentalJsExport
@JsExport
class Euler(initX: Double = 0.0, initY: Double = 0.0, initZ: Double = 0.0, var order: AxisOrder = AxisOrder.XYZ) {
    companion object {
        val DefaultOrder: AxisOrder = AxisOrder.XYZ;
    }

    private val onChangeEmitter = EventEmitter()

    val onChange: Observable
        get() = onChangeEmitter

    private var value: MutableVector3 = MutableVector3(initX, initY, initZ)

    var x: Double
        get() {
            return this.value.get(0)
        }
        set(value) {
            this.value.setIndex(0, value)
            this.onChangeEmitter.emit();
        }

    var y: Double
        get() {
            return this.value.get(1)
        }
        set(value) {
            this.value.setIndex(1, value)
            this.onChangeEmitter.emit();
        }

    var z: Double
        get() {
            return this.value.get(2)
        }
        set(value) {
            this.value.setIndex(2, value)
            this.onChangeEmitter.emit();
        }

    fun setFromQuaternion(quaternion: Quaternion, order: AxisOrder? = null, update: Boolean = false): Euler {
        val matrix = MutableMatrix4()
        matrix.makeRotationFromQuaternion(quaternion);
        return this.setFromRotationMatrix(matrix, order, update);
    }

    private fun setFromRotationMatrix(matrix: MutableMatrix4, order: AxisOrder? = null, update: Boolean): Euler {
        val m11 = matrix.elements[0]
        val m12 = matrix.elements[4]
        val m13 = matrix.elements[8]
        val m21 = matrix.elements[1]
        val m22 = matrix.elements[5]
        val m23 = matrix.elements[9]
        val m31 = matrix.elements[2]
        val m32 = matrix.elements[6]
        val m33 = matrix.elements[10]
        val validOrder = order ?: this.order

        when (validOrder) {
            AxisOrder.XYZ -> {
                this.y = kotlin.math.asin(Utils.clamp(m13, -1.0, 1.0));
                if (kotlin.math.abs(m13) < 0.9999999) {
                    this.x = kotlin.math.atan2(-m23, m33);
                    this.z = kotlin.math.atan2(-m12, m11);
                } else {
                    this.x = kotlin.math.atan2(m32, m22);
                    this.z = 0.0;
                }
            }
            AxisOrder.YXZ -> {
                this.x = kotlin.math.asin(-Utils.clamp(m23, -1.0, 1.0));

                if (kotlin.math.abs(m23) < 0.9999999) {
                    this.y = kotlin.math.atan2(m13, m33);
                    this.z = kotlin.math.atan2(m21, m22);
                } else {
                    this.y = kotlin.math.atan2(-m31, m11);
                    this.z = 0.0;
                }
            }
            AxisOrder.ZXY -> {
                this.x = kotlin.math.asin(Utils.clamp(m32, -1.0, 1.0));

                if (kotlin.math.abs(m32) < 0.9999999) {
                    this.y = kotlin.math.atan2(-m31, m33);
                    this.z = kotlin.math.atan2(-m12, m22);
                } else {
                    this.y = 0.0;
                    this.z = kotlin.math.atan2(m21, m11);
                }
            }
            AxisOrder.ZYX -> {
                this.y = kotlin.math.asin(-Utils.clamp(m31, -1.0, 1.0));

                if (kotlin.math.abs(m31) < 0.9999999) {
                    this.x = kotlin.math.atan2(m32, m33);
                    this.z = kotlin.math.atan2(m21, m11);
                } else {
                    this.x = 0.0;
                    this.z = kotlin.math.atan2(-m12, m22);
                }
            }
            AxisOrder.YZX -> {
                this.z = kotlin.math.asin(Utils.clamp(m21, -1.0, 1.0));

                if (kotlin.math.abs(m21) < 0.9999999) {
                    this.x = kotlin.math.atan2(-m23, m22);
                    this.y = kotlin.math.atan2(-m31, m11);
                } else {
                    this.x = 0.0;
                    this.y = kotlin.math.atan2(m13, m33);
                }
            }
            AxisOrder.XZY -> {
                this.z = kotlin.math.asin(-Utils.clamp(m12, -1.0, 1.0));

                if (kotlin.math.abs(m12) < 0.9999999) {
                    this.x = kotlin.math.atan2(m32, m22);
                    this.y = kotlin.math.atan2(m13, m11);
                } else {
                    this.x = kotlin.math.atan2(-m23, m33);
                    this.y = 0.0;
                }
            }
        }

        this.order = validOrder
        if (update) this.onChangeEmitter.emit();
        return this;
    }
}
