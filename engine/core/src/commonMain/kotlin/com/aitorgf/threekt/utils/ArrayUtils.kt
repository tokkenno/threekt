package com.aitorgf.threekt.utils

/**
 * Returns whether [array] holds a value of 65535 or more, which needs 32-bit indices. 65535 itself is
 * excluded from 16-bit indices because WebGL 2 reserves it to restart primitives.
 */
internal fun arrayNeedsUint32(array: IntArray): Boolean {
    for (i in array.indices.reversed()) {
        if (array[i] >= 65535) return true
    }
    return false
}
