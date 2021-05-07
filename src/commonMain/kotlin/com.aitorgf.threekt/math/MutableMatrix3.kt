package com.aitorgf.threekt.math

import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport
import kotlin.js.JsName

@ExperimentalJsExport
@JsExport
class MutableMatrix3 : Matrix3(), MutableMatrix {
    override var elements: Array<Double> = arrayOf(
        1.0, 0.0, 0.0,
        0.0, 1.0, 0.0,
        0.0, 0.0, 1.0,
    )

    fun set(
        n11: Double,
        n12: Double,
        n13: Double,
        n21: Double,
        n22: Double,
        n23: Double,
        n31: Double,
        n32: Double,
        n33: Double
    ): MutableMatrix3 {
        this.elements[0] = n11
        this.elements[1] = n21
        this.elements[2] = n31
        this.elements[3] = n12
        this.elements[4] = n22
        this.elements[5] = n32
        this.elements[6] = n13
        this.elements[7] = n23
        this.elements[8] = n33

        return this
    }

    /**
     * Set this matrix to the upper 3x3 matrix of the Matrix4 [m].
     */
    @JsName("setMatrix4")
    fun set(m: Matrix4): MutableMatrix3 {
        this.set(
            m.elements[0], m.elements[4], m.elements[8],
            m.elements[1], m.elements[5], m.elements[9],
            m.elements[2], m.elements[6], m.elements[10]
        )

        return this
    }

    /**
     * Sets this matrix as the upper left 3x3 of the normal matrix of the passed matrix4. The normal matrix is the inverse transpose of the matrix m.
     */
    fun getNormalMatrix(m: Matrix4): MutableMatrix3 {
        return this.set(m).invert().transpose()
    }

    fun invert(): MutableMatrix3 {
        val n11 = this.elements[0]
        val n21 = this.elements[1]
        val n31 = this.elements[2]
        val n12 = this.elements[3]
        val n22 = this.elements[4]
        val n32 = this.elements[5]
        val n13 = this.elements[6]
        val n23 = this.elements[7]
        val n33 = this.elements[8]

        val t11 = n33 * n22 - n32 * n23
        val t12 = n32 * n13 - n33 * n12
        val t13 = n23 * n12 - n22 * n13

        val det = n11 * t11 + n21 * t12 + n31 * t13

        if (det == 0.0) {
            return this.set(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        }

        val detInv = 1.0 / det

        this.elements[0] = t11 * detInv
        this.elements[1] = (n31 * n23 - n33 * n21) * detInv
        this.elements[2] = (n32 * n21 - n31 * n22) * detInv

        this.elements[3] = t12 * detInv
        this.elements[4] = (n33 * n11 - n31 * n13) * detInv
        this.elements[5] = (n31 * n12 - n32 * n11) * detInv

        this.elements[6] = t13 * detInv
        this.elements[7] = (n21 * n13 - n23 * n11) * detInv
        this.elements[8] = (n22 * n11 - n21 * n12) * detInv

        return this
    }

    fun transpose(): MutableMatrix3 {
        var tmp: Double = this.elements[1]
        this.elements[1] = this.elements[3]
        this.elements[3] = tmp
        tmp = this.elements[2]
        this.elements[2] = this.elements[6]
        this.elements[6] = tmp
        tmp = this.elements[5]
        this.elements[5] = this.elements[7]
        this.elements[7] = tmp

        return this
    }
}
