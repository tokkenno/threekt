package math

@ExperimentalJsExport
@JsExport
open class Matrix4 : Matrix {
    open val elements: Array<Double> = arrayOf(
        1.0, 0.0, 0.0, 0.0,
        0.0, 1.0, 0.0, 0.0,
        0.0, 0.0, 1.0, 0.0,
        0.0, 0.0, 0.0, 1.0
    )

    fun determinant(): Double {
        val n11 = this.elements[0];
        val n12 = this.elements[4];
        val n13 = this.elements[8];
        val n14 = this.elements[12];
        val n21 = this.elements[1];
        val n22 = this.elements[5];
        val n23 = this.elements[9];
        val n24 = this.elements[13];
        val n31 = this.elements[2];
        val n32 = this.elements[6];
        val n33 = this.elements[10];
        val n34 = this.elements[14];
        val n41 = this.elements[3];
        val n42 = this.elements[7];
        val n43 = this.elements[11];
        val n44 = this.elements[15];

        //TODO: make this more efficient
        //( based on http://www.euclideanspace.com/maths/algebra/matrix/functions/inverse/fourD/index.htm )

        return (n41 * (
                +n14 * n23 * n32
                        - n13 * n24 * n32
                        - n14 * n22 * n33
                        + n12 * n24 * n33
                        + n13 * n22 * n34
                        - n12 * n23 * n34) +
                n42 * (
                +n11 * n23 * n34
                        - n11 * n24 * n33
                        + n14 * n21 * n33
                        - n13 * n21 * n34
                        + n13 * n24 * n31
                        - n14 * n23 * n31) +
                n43 * (
                +n11 * n24 * n32
                        - n11 * n22 * n34
                        - n14 * n21 * n32
                        + n12 * n21 * n34
                        + n14 * n22 * n31
                        - n12 * n24 * n31) +
                n44 * (
                -n13 * n22 * n31
                        - n11 * n23 * n32
                        + n11 * n22 * n33
                        + n13 * n21 * n32
                        - n12 * n21 * n33
                        + n12 * n23 * n31)
                );
    }

    fun getMaxScaleOnAxis(): Double {
        val scaleXSq =
            this.elements[0] * this.elements[0] + this.elements[1] * this.elements[1] + this.elements[2] * this.elements[2];
        val scaleYSq =
            this.elements[4] * this.elements[4] + this.elements[5] * this.elements[5] + this.elements[6] * this.elements[6];
        val scaleZSq =
            this.elements[8] * this.elements[8] + this.elements[9] * this.elements[9] + this.elements[10] * this.elements[10];

        return kotlin.math.sqrt(kotlin.math.max(scaleXSq, kotlin.math.max(scaleYSq, scaleZSq)));
    }
}
