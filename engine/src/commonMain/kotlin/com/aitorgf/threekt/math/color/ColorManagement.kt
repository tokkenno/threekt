package com.aitorgf.threekt.math.color

import com.aitorgf.threekt.math.Color
import com.aitorgf.threekt.math.Matrix3
import com.aitorgf.threekt.math.Vector3

/** Registry and conversion utilities for RGB color spaces. The default working space is linear sRGB. */
object ColorManagement {
    var enabled = true
    var workingColorSpace = ColorSpace.LinearSRGB
    private val rec709 = ColorSpaceDefinition(
        doubleArrayOf(0.640, 0.330, 0.300, 0.600, 0.150, 0.060),
        doubleArrayOf(0.3127, 0.3290),
        ColorTransfer.Linear,
        Matrix3(0.4123908, 0.3575843, 0.1804808, 0.2126390, 0.7151687, 0.0721923, 0.0193308, 0.1191948, 0.9505322),
        Matrix3(3.2409699, -1.5373832, -0.4986108, -0.9692436, 1.8759675, 0.0415551, 0.0556301, -0.2039770, 1.0569715),
        doubleArrayOf(0.2126, 0.7152, 0.0722),
        drawingBufferColorSpace = ColorSpace.SRGB,
        unpackColorSpace = ColorSpace.SRGB,
    )
    val spaces = mutableMapOf(
        ColorSpace.LinearSRGB to rec709,
        ColorSpace.SRGB to rec709.copy(transfer = ColorTransfer.SRGB, unpackColorSpace = null),
    )

    /** Registers or replaces definitions for the supplied color spaces. */
    fun define(definitions: Map<ColorSpace, ColorSpaceDefinition>) {
        spaces.putAll(definitions)
    }

    /**
     * Converts [color] in place from [sourceColorSpace] to [targetColorSpace]. Returns the supplied color.
     */
    fun convert(color: Color, sourceColorSpace: ColorSpace, targetColorSpace: ColorSpace): Color {
        if (!enabled || sourceColorSpace == targetColorSpace ||
            sourceColorSpace == ColorSpace.None || targetColorSpace == ColorSpace.None
        ) return color
        val source = spaces.getValue(sourceColorSpace)
        val target = spaces.getValue(targetColorSpace)
        if (source.transfer == ColorTransfer.SRGB) color.convertSRGBToLinear()
        if (source.primaries !== target.primaries) color.applyMatrix3(source.toXYZ).applyMatrix3(target.fromXYZ)
        if (target.transfer == ColorTransfer.SRGB) color.convertLinearToSRGB()
        return color
    }

    /** Converts [color] in place from the working space to [target] and returns it. */
    fun workingToColorSpace(color: Color, target: ColorSpace): Color = convert(color, workingColorSpace, target)

    /** Converts [color] in place from [source] to the working space and returns it. */
    fun colorSpaceToWorking(color: Color, source: ColorSpace): Color = convert(color, source, workingColorSpace)

    /** Returns the registered red, green, and blue chromaticity coordinates for [colorSpace]. */
    fun getPrimaries(colorSpace: ColorSpace): DoubleArray = spaces.getValue(colorSpace).primaries

    /** Returns the transfer function for [colorSpace]. [ColorSpace.None] uses a linear transfer. */
    fun getTransfer(colorSpace: ColorSpace): ColorTransfer =
        if (colorSpace == ColorSpace.None) ColorTransfer.Linear else spaces.getValue(colorSpace).transfer

    /** Returns the tone mapping mode registered for [colorSpace]. */
    fun getToneMappingMode(colorSpace: ColorSpace): String = spaces.getValue(colorSpace).toneMappingMode

    /** Writes the luminance coefficients for [colorSpace] into [target] and returns it. */
    fun getLuminanceCoefficients(target: Vector3, colorSpace: ColorSpace = workingColorSpace): Vector3 =
        target.fromArray(spaces.getValue(colorSpace).luminanceCoefficients)

    /**
     * Stores the linear RGB transform from [sourceColorSpace] to [targetColorSpace] in [target] and
     * returns it.
     */
    internal fun getMatrix(target: Matrix3, sourceColorSpace: ColorSpace, targetColorSpace: ColorSpace): Matrix3 =
        target.multiplyMatrices(spaces.getValue(targetColorSpace).fromXYZ, spaces.getValue(sourceColorSpace).toXYZ)

    /** Returns the drawing buffer color space configured for [colorSpace], if present. */
    internal fun getDrawingBufferColorSpace(colorSpace: ColorSpace): ColorSpace? = spaces.getValue(colorSpace).drawingBufferColorSpace

    /** Returns the texture unpack color space configured for [colorSpace], if present. */
    internal fun getUnpackColorSpace(colorSpace: ColorSpace = workingColorSpace): ColorSpace? = spaces.getValue(colorSpace).unpackColorSpace
}
