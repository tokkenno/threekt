package com.aitorgf.threekt.extras.curves

import com.aitorgf.threekt.math.Vector4
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals

class NURBSCurveTest {
    @Test
    fun weightedQuarterCircleHasExactEndpointsAndTangents() {
        val curve = NURBSCurve(
            2, doubleArrayOf(0.0, 0.0, 0.0, 1.0, 1.0, 1.0),
            arrayOf(Vector4(1.0, 0.0, 0.0), Vector4(1.0, 1.0, 0.0, sqrt(0.5)), Vector4(0.0, 1.0, 0.0)),
        )
        val middle = curve.getPoint(0.5)
        assertEquals(sqrt(0.5), middle.x, 1e-12)
        assertEquals(sqrt(0.5), middle.y, 1e-12)
        assertEquals(1.0, curve.getPoint(0.0).x)
        assertEquals(1.0, curve.getPoint(1.0).y)
        assertEquals(1.0, curve.getTangent(0.0).y, 1e-12)
        assertEquals(-1.0, curve.getTangent(1.0).x, 1e-12)
    }
}
