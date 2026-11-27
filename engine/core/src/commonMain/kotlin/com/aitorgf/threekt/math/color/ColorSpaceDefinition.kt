package com.aitorgf.threekt.math.color

import com.aitorgf.threekt.math.Matrix3

/**
 * Defines a color space through its chromaticities, transfer function, XYZ transforms, and luminance
 * coefficients.
 */
data class ColorSpaceDefinition(
    val primaries: DoubleArray,
    val whitePoint: DoubleArray,
    val transfer: ColorTransfer,
    val toXYZ: Matrix3,
    val fromXYZ: Matrix3,
    val luminanceCoefficients: DoubleArray,
    val drawingBufferColorSpace: ColorSpace? = null,
    val unpackColorSpace: ColorSpace? = null,
    val toneMappingMode: String = "standard",
)
