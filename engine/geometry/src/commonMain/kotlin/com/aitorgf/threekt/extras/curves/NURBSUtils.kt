package com.aitorgf.threekt.extras.curves

import com.aitorgf.threekt.math.Vector3
import com.aitorgf.threekt.math.Vector4

/** Basis and homogeneous point evaluation shared by the NURBS addons. */
internal object NURBSUtils {
    fun findSpan(degree: Int, u: Double, knots: DoubleArray): Int {
        val n = knots.size - degree - 1
        if (u >= knots[n]) return n - 1
        if (u <= knots[degree]) return degree
        var low = degree
        var high = n
        var mid = (low + high) / 2
        while (u < knots[mid] || u >= knots[mid + 1]) {
            if (u < knots[mid]) high = mid else low = mid
            mid = (low + high) / 2
        }
        return mid
    }

    // The NURBS Book, algorithm A2.2.
    fun calcBasisFunctions(span: Int, u: Double, degree: Int, knots: DoubleArray): DoubleArray {
        val basis = DoubleArray(degree + 1)
        val left = DoubleArray(degree + 1)
        val right = DoubleArray(degree + 1)
        basis[0] = 1.0
        for (j in 1..degree) {
            left[j] = u - knots[span + 1 - j]
            right[j] = knots[span + j] - u
            var saved = 0.0
            for (r in 0 until j) {
                val rv = right[r + 1]
                val lv = left[j - r]
                val term = basis[r] / (rv + lv)
                basis[r] = saved + rv * term
                saved = lv * term
            }
            basis[j] = saved
        }
        return basis
    }

    fun calcBSplinePoint(degree: Int, knots: DoubleArray, points: Array<Vector4>, u: Double): Vector4 {
        val span = findSpan(degree, u, knots)
        val basis = calcBasisFunctions(span, u, degree, knots)
        val point = Vector4(0.0, 0.0, 0.0, 0.0)
        for (i in 0..degree) {
            addWeighted(point, points[span - degree + i], basis[i])
        }
        return point
    }

    fun calcCurveTangent(
        degree: Int, knots: DoubleArray, points: Array<Vector4>, u: Double, target: Vector3,
    ): Vector3 {
        if (degree == 0) return target.setScalar(0.0)
        val span = findSpan(degree, u, knots)
        val lowerBasis = calcBasisFunctions(span, u, degree - 1, knots)
        val derivative = Vector4(0.0, 0.0, 0.0, 0.0)
        for (j in 0..degree) {
            val i = span - degree + j
            val leftWidth = knots[i + degree] - knots[i]
            val rightWidth = knots[i + degree + 1] - knots[i + 1]
            val left = if (j > 0 && leftWidth != 0.0) lowerBasis[j - 1] / leftWidth else 0.0
            val right = if (j < degree && rightWidth != 0.0) lowerBasis[j] / rightWidth else 0.0
            addWeighted(derivative, points[i], degree * (left - right))
        }
        val point = calcBSplinePoint(degree, knots, points, u)
        val weightDerivative = derivative.w / point.w
        return target.set(
            (derivative.x - weightDerivative * point.x) / point.w,
            (derivative.y - weightDerivative * point.y) / point.w,
            (derivative.z - weightDerivative * point.z) / point.w,
        ).normalize()
    }

    fun calcSurfacePoint(
        p: Int, q: Int, knots1: DoubleArray, knots2: DoubleArray, points: Array<Array<Vector4>>,
        u: Double, v: Double, target: Vector3,
    ) {
        val uSpan = findSpan(p, u, knots1)
        val vSpan = findSpan(q, v, knots2)
        val nu = calcBasisFunctions(uSpan, u, p, knots1)
        val nv = calcBasisFunctions(vSpan, v, q, knots2)
        val sum = Vector4(0.0, 0.0, 0.0, 0.0)
        val row = Vector4(0.0, 0.0, 0.0, 0.0)
        for (l in 0..q) {
            row.setScalar(0.0)
            for (k in 0..p) addWeighted(row, points[uSpan - p + k][vSpan - q + l], nu[k])
            sum.add(row.multiplyScalar(nv[l]))
        }
        target.set(sum.x / sum.w, sum.y / sum.w, sum.z / sum.w)
    }

    fun calcVolumePoint(
        p: Int, q: Int, r: Int, knots1: DoubleArray, knots2: DoubleArray, knots3: DoubleArray,
        points: Array<Array<Array<Vector4>>>, u: Double, v: Double, w: Double, target: Vector3,
    ) {
        val uSpan = findSpan(p, u, knots1)
        val vSpan = findSpan(q, v, knots2)
        val wSpan = findSpan(r, w, knots3)
        val nu = calcBasisFunctions(uSpan, u, p, knots1)
        val nv = calcBasisFunctions(vSpan, v, q, knots2)
        val nw = calcBasisFunctions(wSpan, w, r, knots3)
        val sum = Vector4(0.0, 0.0, 0.0, 0.0)
        val row = Vector4(0.0, 0.0, 0.0, 0.0)
        for (m in 0..r) {
            for (l in 0..q) {
                row.setScalar(0.0)
                for (k in 0..p) addWeighted(row, points[uSpan - p + k][vSpan - q + l][wSpan - r + m], nu[k])
                sum.add(row.multiplyScalar(nw[m]).multiplyScalar(nv[l]))
            }
        }
        target.set(sum.x / sum.w, sum.y / sum.w, sum.z / sum.w)
    }

    private fun addWeighted(target: Vector4, point: Vector4, basis: Double) {
        val weight = basis * point.w
        target.x += point.x * weight
        target.y += point.y * weight
        target.z += point.z * weight
        target.w += weight
    }
}
