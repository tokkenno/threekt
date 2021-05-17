package com.aitorgf.threekt.math

import kotlin.js.ExperimentalJsExport
import kotlin.test.Test
import kotlin.test.assertEquals

@ExperimentalJsExport
class MutableBox3Test {
    val zero = Vector3(0.0, 0.0, 0.0)
    val one = Vector3(1.0, 1.0, 1.0)
    val two = Vector3(2.0, 2.0, 2.0)

    fun testSetFromCenterAndSize() {
        var a = MutableBox3(this.zero, this.one);
        var b = a.clone();

        var centerA = a.center
        var sizeA = a.size
        a.setFromCenterAndSize(centerA, sizeA);
        assertEquals(b, a, "The center and size must be the same in this point")

        a.setFromCenterAndSize(one, sizeA)
        assertEquals(one, a.center, "The center must be (1,1,1) in this point")
        assertEquals(sizeA, a.size, "The value must be the same in this point")

        a.setFromCenterAndSize(one, two)
        assertEquals(one, a.center, "The center must be (1,1,1) in this point, even if size change")
        assertEquals(two, a.size, "The value must be (2,2,2) in this point")
    }
}
