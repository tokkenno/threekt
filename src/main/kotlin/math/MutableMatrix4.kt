package math

@ExperimentalJsExport
@JsExport
class MutableMatrix4 : Matrix4(), MutableMatrix {
    override var elements: Array<Double> = arrayOf(
        1.0, 0.0, 0.0, 0.0,
        0.0, 1.0, 0.0, 0.0,
        0.0, 0.0, 1.0, 0.0,
        0.0, 0.0, 0.0, 1.0
    )

    fun set(
        n11: Double,
        n12: Double,
        n13: Double,
        n14: Double,
        n21: Double,
        n22: Double,
        n23: Double,
        n24: Double,
        n31: Double,
        n32: Double,
        n33: Double,
        n34: Double,
        n41: Double,
        n42: Double,
        n43: Double,
        n44: Double
    ): MutableMatrix4 {
        this.elements[0] = n11; this.elements[4] = n12; this.elements[8] = n13; this.elements[12] = n14;
        this.elements[1] = n21; this.elements[5] = n22; this.elements[9] = n23; this.elements[13] = n24;
        this.elements[2] = n31; this.elements[6] = n32; this.elements[10] = n33; this.elements[14] = n34;
        this.elements[3] = n41; this.elements[7] = n42; this.elements[11] = n43; this.elements[15] = n44;

        return this;
    }

    fun copy(m: Matrix4): MutableMatrix4 {
        this.elements[0] = m.elements[0]; this.elements[1] = m.elements[1]; this.elements[2] =
            m.elements[2]; this.elements[3] = m.elements[3];
        this.elements[4] = m.elements[4]; this.elements[5] = m.elements[5]; this.elements[6] =
            m.elements[6]; this.elements[7] = m.elements[7];
        this.elements[8] = m.elements[8]; this.elements[9] = m.elements[9]; this.elements[10] =
            m.elements[10]; this.elements[11] = m.elements[11];
        this.elements[12] = m.elements[12]; this.elements[13] = m.elements[13]; this.elements[14] =
            m.elements[14]; this.elements[15] = m.elements[15];

        return this
    }

    fun compose(position: Vector3, quaternion: Quaternion, scale: Vector3): MutableMatrix4 {
        val x: Double = quaternion.x
        val y: Double = quaternion.y
        val z: Double = quaternion.z
        val w: Double = quaternion.w
        val x2: Double = x + x
        val y2: Double = y + y
        val z2: Double = z + z
        val xx: Double = x * x2
        val xy: Double = x * y2
        val xz: Double = x * z2
        val yy: Double = y * y2
        val yz: Double = y * z2
        val zz: Double = z * z2
        val wx: Double = w * x2
        val wy: Double = w * y2
        val wz: Double = w * z2
        val sx: Double = scale.x
        val sy: Double = scale.y
        val sz: Double = scale.z;

        this.elements[0] = (1 - (yy + zz)) * sx;
        this.elements[1] = (xy + wz) * sx;
        this.elements[2] = (xz - wy) * sx;
        this.elements[3] = 0.0

        this.elements[4] = (xy - wz) * sy;
        this.elements[5] = (1.0 - (xx + zz)) * sy;
        this.elements[6] = (yz + wx) * sy;
        this.elements[7] = 0.0

        this.elements[8] = (xz + wy) * sz;
        this.elements[9] = (yz - wx) * sz;
        this.elements[10] = (1.0 - (xx + yy)) * sz
        this.elements[11] = 0.0

        this.elements[12] = position.x
        this.elements[13] = position.y
        this.elements[14] = position.z
        this.elements[15] = 1.0

        return this;
    }

    fun identity(): MutableMatrix4 {
        this.set(
            1.0, 0.0, 0.0, 0.0,
            0.0, 1.0, 0.0, 0.0,
            0.0, 0.0, 1.0, 0.0,
            0.0, 0.0, 0.0, 1.0
        )
        return this
    }

    fun decompose(position: MutableVector3, quaternion: Quaternion, scale: MutableVector3): MutableMatrix4 {
        val v1 = MutableVector3()
        var sx = v1.set(arrayOf(this.elements[0], this.elements[1], this.elements[2])).length()
        val sy = v1.set(arrayOf(this.elements[4], this.elements[5], this.elements[6])).length()
        val sz = v1.set(arrayOf(this.elements[8], this.elements[9], this.elements[10])).length()

        // if determine is negative, we need to invert one scale
        val det = this.determinant()
        if (det < 0) sx = -sx;

        position.x = this.elements[12];
        position.y = this.elements[13];
        position.z = this.elements[14];

        // scale the rotation part
        val m1 = MutableMatrix4()
        m1.copy(this)

        val invSX = 1 / sx
        val invSY = 1 / sy
        val invSZ = 1 / sz

        m1.elements[0] *= invSX
        m1.elements[1] *= invSX
        m1.elements[2] *= invSX

        m1.elements[4] *= invSY
        m1.elements[5] *= invSY
        m1.elements[6] *= invSY

        m1.elements[8] *= invSZ
        m1.elements[9] *= invSZ
        m1.elements[10] *= invSZ

        quaternion.setFromRotationMatrix(m1)

        scale.x = sx
        scale.y = sy
        scale.z = sz

        return this
    }

    fun invert(): MutableMatrix4 {
        val n11 = this.elements[0]
        val n21 = this.elements[1]
        val n31 = this.elements[2]
        val n41 = this.elements[3]
        val n12 = this.elements[4]
        val n22 = this.elements[5]
        val n32 = this.elements[6]
        val n42 = this.elements[7]
        val n13 = this.elements[8]
        val n23 = this.elements[9]
        val n33 = this.elements[10]
        val n43 = this.elements[11]
        val n14 = this.elements[12]
        val n24 = this.elements[13]
        val n34 = this.elements[14]
        val n44 = this.elements[15]

        val t11 =
            n23 * n34 * n42 - n24 * n33 * n42 + n24 * n32 * n43 - n22 * n34 * n43 - n23 * n32 * n44 + n22 * n33 * n44
        val t12 =
            n14 * n33 * n42 - n13 * n34 * n42 - n14 * n32 * n43 + n12 * n34 * n43 + n13 * n32 * n44 - n12 * n33 * n44
        val t13 =
            n13 * n24 * n42 - n14 * n23 * n42 + n14 * n22 * n43 - n12 * n24 * n43 - n13 * n22 * n44 + n12 * n23 * n44
        val t14 =
            n14 * n23 * n32 - n13 * n24 * n32 - n14 * n22 * n33 + n12 * n24 * n33 + n13 * n22 * n34 - n12 * n23 * n34;

        val det = n11 * t11 + n21 * t12 + n31 * t13 + n41 * t14;

        if (det == 0.0) {
            return this.set(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        }

        val detInv = 1.0 / det;

        this.elements[0] = t11 * detInv;
        this.elements[1] =
            (n24 * n33 * n41 - n23 * n34 * n41 - n24 * n31 * n43 + n21 * n34 * n43 + n23 * n31 * n44 - n21 * n33 * n44) * detInv
        this.elements[2] =
            (n22 * n34 * n41 - n24 * n32 * n41 + n24 * n31 * n42 - n21 * n34 * n42 - n22 * n31 * n44 + n21 * n32 * n44) * detInv
        this.elements[3] =
            (n23 * n32 * n41 - n22 * n33 * n41 - n23 * n31 * n42 + n21 * n33 * n42 + n22 * n31 * n43 - n21 * n32 * n43) * detInv
        this.elements[4] = t12 * detInv;
        this.elements[5] =
            (n13 * n34 * n41 - n14 * n33 * n41 + n14 * n31 * n43 - n11 * n34 * n43 - n13 * n31 * n44 + n11 * n33 * n44) * detInv
        this.elements[6] =
            (n14 * n32 * n41 - n12 * n34 * n41 - n14 * n31 * n42 + n11 * n34 * n42 + n12 * n31 * n44 - n11 * n32 * n44) * detInv
        this.elements[7] =
            (n12 * n33 * n41 - n13 * n32 * n41 + n13 * n31 * n42 - n11 * n33 * n42 - n12 * n31 * n43 + n11 * n32 * n43) * detInv
        this.elements[8] = t13 * detInv;
        this.elements[9] =
            (n14 * n23 * n41 - n13 * n24 * n41 - n14 * n21 * n43 + n11 * n24 * n43 + n13 * n21 * n44 - n11 * n23 * n44) * detInv
        this.elements[10] =
            (n12 * n24 * n41 - n14 * n22 * n41 + n14 * n21 * n42 - n11 * n24 * n42 - n12 * n21 * n44 + n11 * n22 * n44) * detInv
        this.elements[11] =
            (n13 * n22 * n41 - n12 * n23 * n41 - n13 * n21 * n42 + n11 * n23 * n42 + n12 * n21 * n43 - n11 * n22 * n43) * detInv
        this.elements[12] = t14 * detInv;
        this.elements[13] =
            (n13 * n24 * n31 - n14 * n23 * n31 + n14 * n21 * n33 - n11 * n24 * n33 - n13 * n21 * n34 + n11 * n23 * n34) * detInv
        this.elements[14] =
            (n14 * n22 * n31 - n12 * n24 * n31 - n14 * n21 * n32 + n11 * n24 * n32 + n12 * n21 * n34 - n11 * n22 * n34) * detInv
        this.elements[15] =
            (n12 * n23 * n31 - n13 * n22 * n31 + n13 * n21 * n32 - n11 * n23 * n32 - n12 * n21 * n33 + n11 * n22 * n33) * detInv

        return this;
    }

    fun multiply(a: Matrix4, b: Matrix4? = null): MutableMatrix4 {
        val ae = if (b != null) {
            a.elements
        } else this.elements
        val be = if (b != null) {
            b.elements
        } else a.elements

        val a11 = ae[0];
        val a12 = ae[4];
        val a13 = ae[8];
        val a14 = ae[12];
        val a21 = ae[1];
        val a22 = ae[5];
        val a23 = ae[9];
        val a24 = ae[13];
        val a31 = ae[2];
        val a32 = ae[6];
        val a33 = ae[10];
        val a34 = ae[14];
        val a41 = ae[3];
        val a42 = ae[7];
        val a43 = ae[11];
        val a44 = ae[15];

        val b11 = be[0];
        val b12 = be[4];
        val b13 = be[8];
        val b14 = be[12];
        val b21 = be[1];
        val b22 = be[5];
        val b23 = be[9];
        val b24 = be[13];
        val b31 = be[2];
        val b32 = be[6];
        val b33 = be[10];
        val b34 = be[14];
        val b41 = be[3];
        val b42 = be[7];
        val b43 = be[11];
        val b44 = be[15];

        this.elements[0] = a11 * b11 + a12 * b21 + a13 * b31 + a14 * b41;
        this.elements[4] = a11 * b12 + a12 * b22 + a13 * b32 + a14 * b42;
        this.elements[8] = a11 * b13 + a12 * b23 + a13 * b33 + a14 * b43;
        this.elements[12] = a11 * b14 + a12 * b24 + a13 * b34 + a14 * b44;

        this.elements[1] = a21 * b11 + a22 * b21 + a23 * b31 + a24 * b41;
        this.elements[5] = a21 * b12 + a22 * b22 + a23 * b32 + a24 * b42;
        this.elements[9] = a21 * b13 + a22 * b23 + a23 * b33 + a24 * b43;
        this.elements[13] = a21 * b14 + a22 * b24 + a23 * b34 + a24 * b44;

        this.elements[2] = a31 * b11 + a32 * b21 + a33 * b31 + a34 * b41;
        this.elements[6] = a31 * b12 + a32 * b22 + a33 * b32 + a34 * b42;
        this.elements[10] = a31 * b13 + a32 * b23 + a33 * b33 + a34 * b43;
        this.elements[14] = a31 * b14 + a32 * b24 + a33 * b34 + a34 * b44;

        this.elements[3] = a41 * b11 + a42 * b21 + a43 * b31 + a44 * b41;
        this.elements[7] = a41 * b12 + a42 * b22 + a43 * b32 + a44 * b42;
        this.elements[11] = a41 * b13 + a42 * b23 + a43 * b33 + a44 * b43;
        this.elements[15] = a41 * b14 + a42 * b24 + a43 * b34 + a44 * b44;

        return this;
    }

    fun premultiply(other: Matrix4): MutableMatrix4 {
        return this.multiply(other, this)
    }

    fun makeRotationFromQuaternion(quaternion: Quaternion): MutableMatrix4 {
        return this.compose(Vector3.zero, quaternion, Vector3.one);
    }
}
